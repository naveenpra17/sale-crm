import { Routes } from '@angular/router';
import { authGuard, adminGuard, guestGuard, passwordChangeGuard } from './core/guards/auth.guard';

export const appRoutes: Routes = [
  { path: 'login', canActivate: [guestGuard], loadComponent: () => import('./auth/login.component').then(m => m.LoginComponent) },
  { path: 'change-password', canActivate: [passwordChangeGuard], loadComponent: () => import('./auth/change-password.component').then(m => m.ChangePasswordComponent) },
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () => import('./shell/shell.component').then(m => m.ShellComponent),
    children: [
      { path: 'dashboard', loadComponent: () => import('./dashboard/dashboard.component').then(m => m.DashboardComponent) },
      { path: 'my-sales', loadComponent: () => import('./my-sales/my-sales.component').then(m => m.MySalesComponent) },
      { path: 'leaderboard', loadComponent: () => import('./leaderboard/leaderboard.component').then(m => m.LeaderboardComponent) },
      { path: 'profile', loadComponent: () => import('./profile/profile.component').then(m => m.ProfileComponent) },
      { path: 'admin', canActivate: [adminGuard], loadComponent: () => import('./admin/admin-home.component').then(m => m.AdminHomeComponent) },
      { path: 'admin/settings', canActivate: [adminGuard], loadComponent: () => import('./admin/settings/settings.component').then(m => m.SettingsComponent) },
      { path: 'admin/users', canActivate: [adminGuard], loadComponent: () => import('./admin/users/users.component').then(m => m.UsersComponent) },
      { path: 'admin/sales', canActivate: [adminGuard], loadComponent: () => import('./admin/sales/admin-sales.component').then(m => m.AdminSalesComponent) },
      { path: 'admin/audit-logs', canActivate: [adminGuard], loadComponent: () => import('./admin/audit-logs/audit.component').then(m => m.AuditComponent) },
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' }
    ]
  },
  { path: '**', redirectTo: '' }
];
