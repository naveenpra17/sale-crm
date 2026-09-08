import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../auth/auth.service';

export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (!auth.user) {
    return router.createUrlTree(['/login']);
  }
  if (auth.user.mustChangePassword) {
    return router.createUrlTree(['/change-password']);
  }
  return true;
};

export const adminGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  return auth.user?.role === 'ADMIN' ? true : router.createUrlTree(['/dashboard']);
};

export const guestGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  return auth.user ? router.createUrlTree(['/dashboard']) : true;
};

export const passwordChangeGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (!auth.user) {
    return router.createUrlTree(['/login']);
  }
  if (!auth.user.mustChangePassword) {
    return router.createUrlTree(['/dashboard']);
  }
  return true;
};
