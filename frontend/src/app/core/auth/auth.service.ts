import { Injectable, inject, isDevMode } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { BehaviorSubject, firstValueFrom } from 'rxjs';
import { User } from '../../models/models';
import { AppConfigService } from '../config/app-config.service';
import { AuthCoordinator } from './auth-coordinator';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private config = inject(AppConfigService);
  private tokenValue: string | null = null;
  private csrfTokenValue: string | null = null;
  private userSubject = new BehaviorSubject<User | null>(null);
  user$ = this.userSubject.asObservable();
  initializing = true;
  private initialized = false;

  private api(path: string) {
    return `${this.config.apiBaseUrl}${path}`;
  }

  /** In-memory CSRF token for cross-origin deployments (cookie is not readable via document.cookie). */
  get csrfToken() {
    return this.csrfTokenValue;
  }

  async fetchCsrf(): Promise<string> {
    const r = await firstValueFrom(
      this.http.get<{ token: string }>(this.api('/auth/csrf'), { withCredentials: true })
    );
    this.csrfTokenValue = r.token;
    return r.token;
  }

  async initialize(): Promise<void> {
    if (this.initialized) {
      return;
    }
    this.initialized = true;

    if (!this.config.isLoaded) {
      const err = new Error('AppConfigService.load() must complete before AuthService.initialize()');
      if (isDevMode()) {
        console.error('[auth] Startup blocked: runtime config not loaded', err);
      }
      this.clearSession();
      this.initializing = false;
      throw err;
    }

    try {
      await this.fetchCsrf();
      const r = await firstValueFrom(
        this.http.post<{ accessToken: string; user: User }>(
          this.api('/auth/refresh'),
          {},
          { withCredentials: true }
        )
      );
      this.tokenValue = r.accessToken;
      this.userSubject.next(r.user);
      if (isDevMode()) {
        console.debug('[auth] Session restored from refresh cookie');
      }
    } catch (err) {
      if (this.isMissingRefreshSession(err)) {
        if (isDevMode()) {
          console.debug('[auth] No active refresh session on startup');
        }
        this.clearSession();
      } else {
        if (isDevMode()) {
          console.error('[auth] Startup initialization failed', err);
        }
        this.clearSession();
        throw err;
      }
    } finally {
      this.initializing = false;
    }
  }

  async login(email: string, password: string) {
    await this.fetchCsrf();
    const r = await firstValueFrom(
      this.http.post<{ accessToken: string; user: User }>(
        this.api('/auth/login'),
        { email, password },
        { withCredentials: true }
      )
    );
    this.tokenValue = r.accessToken;
    this.userSubject.next(r.user);
  }

  async logout() {
    try {
      if (!this.csrfTokenValue) {
        await this.fetchCsrf();
      }
      await firstValueFrom(this.http.post(this.api('/auth/logout'), {}, { withCredentials: true }));
    } finally {
      this.clearSession();
    }
  }

  get token() {
    return this.tokenValue;
  }

  get user() {
    return this.userSubject.value;
  }

  async changePassword(currentPassword: string, newPassword: string) {
    if (!this.csrfTokenValue) {
      await this.fetchCsrf();
    }
    const r = await firstValueFrom(
      this.http.post<{ accessToken: string; user: User }>(
        this.api('/auth/change-password'),
        { currentPassword, newPassword },
        { withCredentials: true }
      )
    );
    this.tokenValue = r.accessToken;
    this.userSubject.next(r.user);
  }

  async refresh(): Promise<string> {
    if (!this.csrfTokenValue) {
      await this.fetchCsrf();
    }
    const payload = await AuthCoordinator.coordinateRefresh(async () => {
      const r = await firstValueFrom(
        this.http.post<{ accessToken: string; user: User }>(
          this.api('/auth/refresh'),
          {},
          { withCredentials: true }
        )
      );
      return { accessToken: r.accessToken, user: r.user };
    });
    this.tokenValue = payload.accessToken;
    this.userSubject.next(payload.user);
    return payload.accessToken;
  }

  private clearSession(): void {
    this.tokenValue = null;
    this.csrfTokenValue = null;
    this.userSubject.next(null);
  }

  private isMissingRefreshSession(err: unknown): boolean {
    return err instanceof HttpErrorResponse && (err.status === 401 || err.status === 403);
  }
}
