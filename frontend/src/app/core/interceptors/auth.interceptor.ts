import { HttpErrorResponse, HttpEvent, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Observable, from, throwError } from 'rxjs';
import { catchError, switchMap, shareReplay, finalize } from 'rxjs/operators';
import { AuthService } from '../auth/auth.service';

let refresh$: Observable<string> | null = null;

function requestId() {
  return crypto.randomUUID().replace(/-/g, '').slice(0, 8).toUpperCase();
}

export const authInterceptor: HttpInterceptorFn = (req, next): Observable<HttpEvent<unknown>> => {
  const auth = inject(AuthService);
  const isAuth = req.url.includes('/auth/');
  let r = req.clone({
    withCredentials: true,
    setHeaders: { 'X-Request-ID': req.headers.get('X-Request-ID') || requestId() }
  });
  const token = auth.token;
  if (token && !isAuth) {
    r = r.clone({ setHeaders: { Authorization: `Bearer ${token}`, 'X-Request-ID': r.headers.get('X-Request-ID')! } });
  }
  const xsrf = auth.csrfToken;
  const isAuthMutation = isAuth && ['POST', 'PUT', 'DELETE', 'PATCH'].includes(req.method) && !req.url.includes('/auth/csrf');
  if (xsrf && isAuthMutation) {
    r = r.clone({ setHeaders: { 'X-XSRF-TOKEN': xsrf, 'X-Request-ID': r.headers.get('X-Request-ID')! } });
  }

  return next(r).pipe(
    catchError((err: HttpErrorResponse) => {
      const csrfFailed = err.status === 403
        && (err.error?.error === 'CSRF' || err.error?.message === 'CSRF validation failed');
      if (csrfFailed && isAuthMutation && !req.headers.has('X-CSRF-RETRY')) {
        return from(auth.fetchCsrf()).pipe(
          switchMap(() => {
            const retry = r.clone({
              setHeaders: {
                'X-XSRF-TOKEN': auth.csrfToken!,
                'X-Request-ID': r.headers.get('X-Request-ID')!,
                'X-CSRF-RETRY': '1'
              }
            });
            return next(retry);
          })
        );
      }
      if (err.status === 403) {
        return throwError(() => ({ ...err, friendlyMessage: err.error?.message || 'You do not have permission to perform this action.' }));
      }
      if (err.status === 429) {
        return throwError(() => ({ ...err, friendlyMessage: err.error?.message || 'Too many requests. Please wait and try again.' }));
      }
      if (err.status === 0) {
        return throwError(() => ({ ...err, friendlyMessage: 'Unable to reach the server. Check your connection.' }));
      }
      if (err.status >= 500) {
        return throwError(() => ({ ...err, friendlyMessage: 'Something went wrong on our side. Please try again.' }));
      }
      if (err.status !== 401 || isAuth) {
        return throwError(() => err);
      }
      if (!refresh$) {
        refresh$ = from(auth.refresh()).pipe(
          shareReplay(1),
          finalize(() => {
            refresh$ = null;
          })
        );
      }
      return refresh$.pipe(
        switchMap((t: string) =>
          next(r.clone({ setHeaders: { Authorization: `Bearer ${t}` }, withCredentials: true }))
        ),
        catchError(e => {
          auth.logout().catch(() => {});
          return throwError(() => e);
        })
      );
    })
  );
};
