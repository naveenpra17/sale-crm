import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { AdminService } from '../../services/admin.service';
import { LoadingStateComponent, ErrorStateComponent } from '../../shared/state.components';
import { DialogService } from '../../shared/dialog.service';
import { apiErrorMessage } from '../../core/api-error';
import { debounceTime, distinctUntilChanged, firstValueFrom } from 'rxjs';

@Component({
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, LoadingStateComponent, ErrorStateComponent],
  template: `
    <main class="page">
      <ac-loading-state *ngIf="loading"></ac-loading-state>
      <ac-error-state *ngIf="error" [message]="error" (retry)="load()"></ac-error-state>

      <section class="card" style="padding:18px" *ngIf="!loading && !error">
        <div class="toolbar">
          <div><div class="eyebrow">Administration</div><h1 class="title">Users</h1></div>
          <button class="btn btn-primary" (click)="openCreate()">Add User</button>
        </div>

        <div class="filter-row">
          <input placeholder="Search name or email" [formControl]="searchControl">
          <select [formControl]="statusControl"><option value="">All statuses</option><option value="true">Active</option><option value="false">Inactive</option></select>
          <select [formControl]="roleControl"><option value="">All roles</option><option value="USER">User</option><option value="ADMIN">Admin</option></select>
        </div>

        <div class="list">
          <div class="list-item" *ngFor="let u of users">
            <div class="avatar">{{ u.name.charAt(0) }}</div>
            <div class="grow">
              <strong>{{ u.name }}</strong>
              <div class="muted">{{ u.email }} · {{ u.role }}</div>
            </div>
            <span class="pill">{{ u.active ? 'Active' : 'Inactive' }}</span>
            <button class="btn btn-secondary" (click)="openEdit(u)">Edit</button>
            <button class="btn btn-danger" *ngIf="u.active" (click)="deactivate(u.id)">Deactivate</button>
            <button class="btn btn-secondary" *ngIf="!u.active" (click)="reactivate(u.id)">Reactivate</button>
          </div>
        </div>

        <div class="toolbar" style="margin-top:12px">
          <button class="btn btn-secondary" [disabled]="page === 0" (click)="page = page - 1; load()">Previous</button>
          <span class="muted">Page {{ page + 1 }}</span>
          <button class="btn btn-secondary" [disabled]="!hasMore" (click)="page = page + 1; load()">Next</button>
        </div>
      </section>

      <section class="card modal-card" *ngIf="editing">
        <h2>{{ editing.id ? 'Edit User' : 'Create User' }}</h2>
        <form [formGroup]="form" (ngSubmit)="save()">
          <div class="field"><label>Name</label><input formControlName="name"></div>
          <div class="field"><label>Email</label><input type="email" formControlName="email"></div>
          <div class="field" *ngIf="!editing.id"><label>Temporary password</label><input type="password" formControlName="password"></div>
          <div class="field"><label>Role</label><select formControlName="role"><option value="USER">USER</option><option value="ADMIN">ADMIN</option></select></div>
          <label *ngIf="editing.id" style="display:flex;gap:8px;align-items:center"><input type="checkbox" formControlName="active"> Active</label>
          <label style="display:flex;gap:8px;align-items:center;margin-top:8px"><input type="checkbox" formControlName="mustChangePassword"> Must change password</label>
          <div class="toolbar" style="margin-top:14px">
            <button class="btn btn-secondary" type="button" (click)="editing = null">Cancel</button>
            <button class="btn btn-primary" [disabled]="form.invalid">Save</button>
          </div>
          <p class="error" *ngIf="formError">{{ formError }}</p>
        </form>
        <button *ngIf="editing.id" class="btn btn-secondary" style="margin-top:10px" (click)="resetPassword()">Reset Password</button>
      </section>
    </main>
  `
})
export class UsersComponent implements OnInit {
  private api = inject(AdminService);
  private dialog = inject(DialogService);
  private fb = inject(FormBuilder);
  users: any[] = [];
  loading = true;
  error = '';
  page = 0;
  hasMore = false;
  editing: any = null;
  formError = '';
  searchControl = this.fb.control('');
  statusControl = this.fb.control('');
  roleControl = this.fb.control('');
  form = this.fb.nonNullable.group({
    name: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    password: [''],
    role: ['USER', Validators.required],
    active: [true],
    mustChangePassword: [true]
  });

  ngOnInit() {
    this.load();
    this.searchControl.valueChanges.pipe(debounceTime(300), distinctUntilChanged()).subscribe(() => { this.page = 0; this.load(); });
    this.statusControl.valueChanges.subscribe(() => { this.page = 0; this.load(); });
    this.roleControl.valueChanges.subscribe(() => { this.page = 0; this.load(); });
  }

  load() {
    this.loading = true;
    this.error = '';
    const q = encodeURIComponent(this.searchControl.value || '');
    const active = this.statusControl.value;
    const role = this.roleControl.value;
    const activeParam = active ? `&active=${active}` : '';
    const roleParam = role ? `&role=${role}` : '';
    this.api.users(`page=${this.page}&size=20&search=${q}${activeParam}${roleParam}`).subscribe({
      next: x => {
        this.users = x.content || [];
        this.hasMore = !x.last;
        this.loading = false;
      },
      error: e => {
        this.error = apiErrorMessage(e, 'Unable to load users.');
        this.loading = false;
      }
    });
  }

  openCreate() {
    this.editing = {};
    this.form.reset({ name: '', email: '', password: '', role: 'USER', active: true, mustChangePassword: true });
    this.form.controls.password.setValidators([Validators.required, Validators.minLength(8)]);
    this.form.controls.password.updateValueAndValidity();
  }

  openEdit(u: any) {
    this.editing = u;
    this.form.patchValue({ name: u.name, email: u.email, role: u.role, active: u.active, mustChangePassword: u.mustChangePassword });
    this.form.controls.password.clearValidators();
    this.form.controls.password.updateValueAndValidity();
  }

  async save() {
    this.formError = '';
    const value = this.form.getRawValue();
    try {
      if (this.editing.id) {
        await firstValueFrom(this.api.updateUser(this.editing.id, value));
      } else {
        await firstValueFrom(this.api.createUser(value));
      }
      this.editing = null;
      this.load();
    } catch (e: any) {
      this.formError = apiErrorMessage(e, 'Unable to save user');
    }
  }

  async deactivate(id: number) {
    if (await this.dialog.confirm('Deactivate user', 'Deactivate this user? Their sales history will be preserved.')) {
      this.api.deactivate(id).subscribe(() => this.load());
    }
  }

  async reactivate(id: number) {
    if (await this.dialog.confirm('Reactivate user', 'Allow this user to sign in again?')) {
      this.api.reactivate(id).subscribe(() => this.load());
    }
  }

  async resetPassword() {
    const pw = await this.dialog.prompt('Reset password', 'Enter a temporary password (min 8 characters).', 'Temporary password', 'password');
    if (!pw) return;
    try {
      await firstValueFrom(this.api.resetPassword(this.editing.id, pw));
      await this.dialog.alert('Password reset', 'The user must change their password on next login.');
    } catch (e: any) {
      await this.dialog.alert('Unable to reset', e?.error?.message || 'Unable to reset password');
    }
  }
}
