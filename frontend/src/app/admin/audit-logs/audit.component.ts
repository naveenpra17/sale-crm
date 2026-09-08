import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { AdminService } from '../../services/admin.service';
import { LoadingStateComponent, ErrorStateComponent, EmptyStateComponent } from '../../shared/state.components';
import { debounceTime, distinctUntilChanged } from 'rxjs';

@Component({
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, LoadingStateComponent, ErrorStateComponent, EmptyStateComponent],
  template: `
    <main class="page">
      <ac-loading-state *ngIf="loading && !logs.length"></ac-loading-state>
      <ac-error-state *ngIf="error" [message]="error" (retry)="load()"></ac-error-state>

      <section class="card" style="padding:18px" *ngIf="!error && (!loading || logs.length)">
        <div class="eyebrow">Administration</div>
        <h1 class="title">Audit Logs</h1>

        <div class="filter-row">
          <input placeholder="Search actor, action, target" [formControl]="searchControl">
          <select [formControl]="actionControl">
            <option value="">All actions</option>
            <option value="LOGIN">Login</option>
            <option value="LOGOUT">Logout</option>
            <option value="CREATE_SALE">Create sale</option>
            <option value="UPDATE_SALE">Update sale</option>
            <option value="DELETE_SALE">Delete sale</option>
            <option value="CREATE_USER">Create user</option>
            <option value="UPDATE_USER">Update user</option>
            <option value="DEACTIVATE_USER">Deactivate user</option>
            <option value="REACTIVATE_USER">Reactivate user</option>
            <option value="RESET_PASSWORD">Reset password</option>
            <option value="CHANGE_PASSWORD">Change password</option>
            <option value="UPDATE_PROJECT">Update project</option>
          </select>
          <input type="date" [formControl]="fromControl" aria-label="From date">
          <input type="date" [formControl]="toControl" aria-label="To date">
          <button class="btn btn-secondary" type="button" (click)="clearFilters()">Clear</button>
        </div>

        <ac-empty-state *ngIf="!loading && !logs.length" title="No audit entries" message="Actions will appear here as they occur."></ac-empty-state>
        <div class="list">
          <div class="list-item" *ngFor="let a of logs">
            <div class="grow">
              <strong>{{ a.action }}</strong>
              <div class="muted">{{ a.entityType }} #{{ a.entityId }} · {{ a.userName || 'System' }}</div>
              <div class="muted" *ngIf="a.newValue">{{ a.newValue }}</div>
            </div>
            <span class="muted">{{ a.createdAt | date:'short' }}</span>
          </div>
        </div>
        <div class="toolbar" style="margin-top:12px">
          <button class="btn btn-secondary" [disabled]="page === 0" (click)="page = page - 1; load()">Previous</button>
          <span class="muted">Page {{ page + 1 }}</span>
          <button class="btn btn-secondary" [disabled]="!hasMore" (click)="page = page + 1; load()">Next</button>
        </div>
      </section>
    </main>
  `
})
export class AuditComponent implements OnInit {
  private api = inject(AdminService);
  private fb = inject(FormBuilder);
  logs: any[] = [];
  loading = true;
  error = '';
  page = 0;
  hasMore = false;
  searchControl = this.fb.control('');
  actionControl = this.fb.control('');
  fromControl = this.fb.control('');
  toControl = this.fb.control('');

  ngOnInit() {
    this.load();
    this.searchControl.valueChanges.pipe(debounceTime(300), distinctUntilChanged()).subscribe(() => { this.page = 0; this.load(); });
    this.actionControl.valueChanges.subscribe(() => { this.page = 0; this.load(); });
    this.fromControl.valueChanges.subscribe(() => { this.page = 0; this.load(); });
    this.toControl.valueChanges.subscribe(() => { this.page = 0; this.load(); });
  }

  clearFilters() {
    this.searchControl.setValue('');
    this.actionControl.setValue('');
    this.fromControl.setValue('');
    this.toControl.setValue('');
    this.page = 0;
    this.load();
  }

  load() {
    this.loading = true;
    this.error = '';
    const q = encodeURIComponent(this.searchControl.value || '');
    const action = this.actionControl.value;
    const from = this.fromControl.value;
    const to = this.toControl.value;
    const actionParam = action ? `&action=${encodeURIComponent(action)}` : '';
    const fromParam = from ? `&from=${from}T00:00:00.000Z` : '';
    const toParam = to ? `&to=${to}T23:59:59.999Z` : '';
    this.api.audit(`page=${this.page}&size=20&search=${q}${actionParam}${fromParam}${toParam}`).subscribe({
      next: x => {
        this.logs = x.content || [];
        this.hasMore = !x.last;
        this.loading = false;
      },
      error: e => {
        this.error = e?.error?.message || 'Unable to load audit logs.';
        this.loading = false;
      }
    });
  }
}
