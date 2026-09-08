import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../core/auth/auth.service';
import { UserService } from '../services/user.service';
import { firstValueFrom } from 'rxjs';

@Component({
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <main class="page">
      <section class="card" style="padding:20px">
        <div style="display:flex;gap:14px;align-items:center">
          <div class="avatar" style="width:56px;height:56px;font-size:22px">{{ u?.name?.charAt(0) }}</div>
          <div>
            <h1 class="title" style="font-size:22px">{{ u?.name }}</h1>
            <div class="muted">{{ u?.email }}</div>
          </div>
        </div>
        <div style="margin-top:18px" class="grid">
          <div class="list-item"><span>Role</span><strong class="grow" style="text-align:right">{{ u?.role }}</strong></div>
          <div class="list-item"><span>Status</span><strong class="grow" style="text-align:right">{{ u?.active ? 'Active' : 'Inactive' }}</strong></div>
          <div class="list-item"><span>Last Login</span><strong class="grow" style="text-align:right">{{ u?.lastLoginAt | date:'medium' }}</strong></div>
        </div>

        <div class="grid" style="margin-top:18px">
          <button class="btn btn-secondary" (click)="showPassword = !showPassword">Change Password</button>
          <button *ngIf="u?.role === 'ADMIN'" class="btn btn-secondary" (click)="router.navigateByUrl('/admin')">Admin Panel</button>
          <button class="btn btn-danger" (click)="logout()">Logout</button>
        </div>

        <form *ngIf="showPassword" [formGroup]="form" (ngSubmit)="submitPassword()" style="margin-top:18px">
          <div class="field"><label>Current password</label><input type="password" formControlName="currentPassword"></div>
          <div class="field"><label>New password</label><input type="password" formControlName="newPassword"></div>
          <div class="field"><label>Confirm password</label><input type="password" formControlName="confirmPassword"></div>
          <button class="btn btn-primary" [disabled]="saving || form.invalid">Save Password</button>
          <p class="error" *ngIf="passwordError">{{ passwordError }}</p>
          <p class="status" *ngIf="passwordSaved">Password updated.</p>
        </form>
      </section>
    </main>
  `
})
export class ProfileComponent {
  auth = inject(AuthService);
  router = inject(Router);
  private users = inject(UserService);
  private fb = inject(FormBuilder);
  u = this.auth.user;
  showPassword = false;
  saving = false;
  passwordError = '';
  passwordSaved = false;
  form = this.fb.nonNullable.group({
    currentPassword: ['', Validators.required],
    newPassword: ['', [Validators.required, Validators.minLength(8)]],
    confirmPassword: ['', Validators.required]
  });

  async submitPassword() {
    const { currentPassword, newPassword, confirmPassword } = this.form.getRawValue();
    if (newPassword !== confirmPassword) {
      this.passwordError = 'Passwords do not match';
      return;
    }
    this.saving = true;
    this.passwordError = '';
    this.passwordSaved = false;
    try {
      await firstValueFrom(this.users.changePassword(currentPassword, newPassword, confirmPassword));
      this.passwordSaved = true;
      this.form.reset();
      await this.auth.refresh();
    } catch (e: any) {
      this.passwordError = e?.error?.message || 'Unable to change password';
    } finally {
      this.saving = false;
    }
  }

  async logout() {
    await this.auth.logout();
    await this.router.navigateByUrl('/login');
  }
}
