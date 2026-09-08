import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../core/auth/auth.service';

@Component({
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <main class="auth-page">
      <section class="auth-card">
        <div class="brand">SALES</div>
        <div style="font-size:18px;font-weight:800">CHANGE PASSWORD</div>
        <p class="brand-sub">You must set a new password before continuing.</p>
        <form [formGroup]="form" (ngSubmit)="submit()">
          <div class="field"><label>Current password</label><input type="password" formControlName="currentPassword"></div>
          <div class="field"><label>New password</label><input type="password" formControlName="newPassword"></div>
          <div class="field"><label>Confirm password</label><input type="password" formControlName="confirmPassword"></div>
          <button class="btn btn-primary" style="width:100%" [disabled]="loading || form.invalid">Update Password</button>
          <p class="error" *ngIf="error">{{ error }}</p>
        </form>
      </section>
    </main>
  `
})
export class ChangePasswordComponent {
  private fb = inject(FormBuilder);
  private auth = inject(AuthService);
  private router = inject(Router);
  loading = false;
  error = '';
  form = this.fb.nonNullable.group({
    currentPassword: ['', Validators.required],
    newPassword: ['', [Validators.required, Validators.minLength(8)]],
    confirmPassword: ['', Validators.required]
  });

  async submit() {
    if (this.form.invalid) return;
    const { currentPassword, newPassword, confirmPassword } = this.form.getRawValue();
    if (newPassword !== confirmPassword) {
      this.error = 'Passwords do not match';
      return;
    }
    this.loading = true;
    this.error = '';
    try {
      await this.auth.changePassword(currentPassword, newPassword);
      await this.router.navigateByUrl('/dashboard');
    } catch (e: any) {
      this.error = e?.error?.message || 'Unable to change password';
    } finally {
      this.loading = false;
    }
  }
}
