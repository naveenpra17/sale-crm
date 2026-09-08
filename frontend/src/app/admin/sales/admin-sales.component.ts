import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { SalesService } from '../../services/sales.service';
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
          <div><div class="eyebrow">Administration</div><h1 class="title">Sales Management</h1></div>
          <div style="display:flex;gap:8px;flex-wrap:wrap">
            <button class="btn btn-secondary" (click)="exportCsv()">Export CSV</button>
            <button class="btn btn-primary" (click)="openCreate()">Add Sale</button>
          </div>
        </div>

        <div class="filter-row">
          <input placeholder="Search salesperson, buyer, plot" [formControl]="searchControl">
          <select [formControl]="userControl"><option value="">All salespeople</option><option *ngFor="let u of userOptions" [value]="u.id">{{ u.name }}</option></select>
          <input type="date" [formControl]="fromControl" aria-label="From date">
          <input type="date" [formControl]="toControl" aria-label="To date">
          <button class="btn btn-secondary" type="button" (click)="clearFilters()">Clear</button>
        </div>

        <div class="list mobile-sales">
          <div class="list-item sale-card" *ngFor="let s of sales">
            <div class="grow">
              <strong>{{ s.salesperson }}</strong>
              <div class="muted">{{ s.saleDate | date }} · {{ s.acres | number:'1.2-2' }} ac</div>
              <div class="muted">{{ s.buyerName || 'No buyer' }} · {{ s.plotReference || 'No plot' }}</div>
            </div>
            <div style="display:flex;gap:8px">
              <button class="btn btn-secondary" (click)="openEdit(s)">Edit</button>
              <button class="btn btn-danger" (click)="remove(s.id)">Delete</button>
            </div>
          </div>
        </div>

        <div class="toolbar" style="margin-top:12px">
          <button class="btn btn-secondary" [disabled]="page === 0" (click)="page = page - 1; load()">Previous</button>
          <span class="muted">Page {{ page + 1 }}</span>
          <button class="btn btn-secondary" [disabled]="!hasMore" (click)="page = page + 1; load()">Next</button>
        </div>
      </section>

      <section class="card modal-card" *ngIf="editing">
        <h2>{{ editing.id ? 'Edit Sale' : 'Create Sale' }}</h2>
        <form [formGroup]="form" (ngSubmit)="save()">
          <div class="field"><label>Salesperson</label><select formControlName="userId"><option *ngFor="let u of userOptions" [value]="u.id">{{ u.name }}</option></select></div>
          <div class="field"><label>Acres</label><input type="number" step="0.0001" formControlName="acres"></div>
          <div class="field"><label>Sale date</label><input type="date" formControlName="saleDate"></div>
          <div class="field"><label>Buyer</label><input formControlName="buyerName"></div>
          <div class="field"><label>Plot</label><input formControlName="plotReference"></div>
          <div class="field"><label>Notes</label><textarea rows="3" formControlName="notes"></textarea></div>
          <div class="toolbar">
            <button class="btn btn-secondary" type="button" (click)="editing = null">Cancel</button>
            <button class="btn btn-primary" [disabled]="form.invalid || saving">{{ saving ? 'Saving…' : 'Save' }}</button>
          </div>
          <p class="error" *ngIf="formError">{{ formError }}</p>
        </form>
      </section>
    </main>
  `
})
export class AdminSalesComponent implements OnInit {
  private api = inject(SalesService);
  private admin = inject(AdminService);
  private dialog = inject(DialogService);
  private fb = inject(FormBuilder);
  sales: any[] = [];
  userOptions: any[] = [];
  loading = true;
  error = '';
  page = 0;
  hasMore = false;
  editing: any = null;
  formError = '';
  saving = false;
  searchControl = this.fb.control('');
  userControl = this.fb.control('');
  fromControl = this.fb.control('');
  toControl = this.fb.control('');
  form = this.fb.nonNullable.group({
    userId: [0, Validators.required],
    acres: [0, [Validators.required, Validators.min(0.0001)]],
    saleDate: ['', Validators.required],
    buyerName: [''],
    plotReference: [''],
    notes: ['']
  });

  ngOnInit() {
    this.admin.users('page=0&size=100&active=true').subscribe(x => this.userOptions = x.content || []);
    this.load();
    this.searchControl.valueChanges.pipe(debounceTime(300), distinctUntilChanged()).subscribe(() => { this.page = 0; this.load(); });
    this.userControl.valueChanges.subscribe(() => { this.page = 0; this.load(); });
    this.fromControl.valueChanges.subscribe(() => { this.page = 0; this.load(); });
    this.toControl.valueChanges.subscribe(() => { this.page = 0; this.load(); });
  }

  clearFilters() {
    this.searchControl.setValue('');
    this.userControl.setValue('');
    this.fromControl.setValue('');
    this.toControl.setValue('');
    this.page = 0;
    this.load();
  }

  load() {
    this.loading = true;
    this.error = '';
    const q = encodeURIComponent(this.searchControl.value || '');
    const userId = this.userControl.value;
    const from = this.fromControl.value;
    const to = this.toControl.value;
    const userParam = userId ? `&userId=${userId}` : '';
    const fromParam = from ? `&fromDate=${from}` : '';
    const toParam = to ? `&toDate=${to}` : '';
    this.api.adminSales(`page=${this.page}&size=20&search=${q}${userParam}${fromParam}${toParam}`).subscribe({
      next: x => {
        this.sales = x.content || [];
        this.hasMore = !x.last;
        this.loading = false;
      },
      error: e => {
        this.error = apiErrorMessage(e, 'Unable to load sales.');
        this.loading = false;
      }
    });
  }

  openCreate() {
    this.editing = {};
    this.form.reset({
      userId: this.userOptions[0]?.id || 0,
      acres: 0,
      saleDate: new Date().toISOString().slice(0, 10),
      buyerName: '',
      plotReference: '',
      notes: ''
    });
  }

  openEdit(s: any) {
    this.editing = s;
    this.form.patchValue({
      userId: s.userId,
      acres: s.acres,
      saleDate: s.saleDate,
      buyerName: s.buyerName || '',
      plotReference: s.plotReference || '',
      notes: s.notes || ''
    });
  }

  async save() {
    this.formError = '';
    if (this.saving) return;
    this.saving = true;
    const value = this.form.getRawValue();
    try {
      if (this.editing.id) {
        await firstValueFrom(this.api.update(this.editing.id, value));
      } else {
        await firstValueFrom(this.api.create(value));
      }
      this.editing = null;
      this.load();
    } catch (e: any) {
      this.formError = apiErrorMessage(e, 'Unable to save sale');
    } finally {
      this.saving = false;
    }
  }

  async remove(id: number) {
    if (await this.dialog.confirm('Delete sale', 'Delete this sale? This cannot be undone.')) {
      this.api.delete(id).subscribe(() => this.load());
    }
  }

  exportCsv() {
    const q = encodeURIComponent(this.searchControl.value || '');
    const userId = this.userControl.value;
    const from = this.fromControl.value;
    const to = this.toControl.value;
    const userParam = userId ? `&userId=${userId}` : '';
    const fromParam = from ? `&fromDate=${from}` : '';
    const toParam = to ? `&toDate=${to}` : '';
    this.api.exportCsv(`search=${q}${userParam}${fromParam}${toParam}`).subscribe(blob => {
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = 'sales-export.csv';
      a.click();
      URL.revokeObjectURL(url);
    });
  }
}
