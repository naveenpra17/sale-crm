import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { AdminService } from '../../services/admin.service';
import { DashboardService } from '../../services/dashboard.service';
import { DialogService } from '../../shared/dialog.service';
import { LoadingStateComponent, ErrorStateComponent } from '../../shared/state.components';
import { apiErrorMessage } from '../../core/api-error';
import { debounceTime, distinctUntilChanged, firstValueFrom } from 'rxjs';

@Component({
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, LoadingStateComponent, ErrorStateComponent],
  template: `
    <main class="page">
      <ac-loading-state *ngIf="loading"></ac-loading-state>
      <ac-error-state *ngIf="error" [message]="error" (retry)="load()"></ac-error-state>
      <section class="card" style="padding:20px" *ngIf="!loading && !error">
        <div class="eyebrow">Administration</div>
        <h1 class="title">Project Settings</h1>
        <form [formGroup]="form" (ngSubmit)="save()">
          <div class="field"><label>Project Name</label><input formControlName="projectName"></div>
          <div class="field"><label>Total Acres</label><input type="number" step="0.0001" formControlName="totalAcres"></div>
          <div class="field"><label>Start Date</label><input type="date" formControlName="startDate"></div>
          <div class="field"><label>Deadline (project timezone)</label><input type="datetime-local" formControlName="deadlineLocal"></div>
          <div class="field"><label>Timezone</label><input formControlName="timezone" placeholder="Asia/Kolkata"></div>
          <p class="muted" *ngIf="timezoneLabel">Displayed in {{ timezoneLabel }}</p>
          <button class="btn btn-primary" [disabled]="form.invalid || saving">Save Settings</button>
          <p class="status" *ngIf="saved">Settings updated.</p>
          <p class="error" *ngIf="saveError">{{ saveError }}</p>
        </form>
      </section>
    </main>
  `
})
export class SettingsComponent implements OnInit {
  private api = inject(AdminService);
  private dashboard = inject(DashboardService);
  private dialog = inject(DialogService);
  private fb = inject(FormBuilder);
  loading = true;
  error = '';
  saving = false;
  saved = false;
  saveError = '';
  timezoneLabel = '';
  private originalTotal = 0;
  form = this.fb.nonNullable.group({
    projectName: ['', Validators.required],
    totalAcres: [0, [Validators.required, Validators.min(0.0001)]],
    startDate: ['', Validators.required],
    deadlineLocal: ['', Validators.required],
    timezone: ['', Validators.required]
  });

  ngOnInit() { this.load(); }

  load() {
    this.loading = true;
    this.api.project().subscribe({
      next: (x: any) => {
        this.originalTotal = Number(x.totalAcres);
        this.timezoneLabel = x.timezone;
        this.form.patchValue({
          projectName: x.projectName,
          totalAcres: x.totalAcres,
          startDate: x.startDate,
          deadlineLocal: this.toInput(x.deadlineLocal || x.deadline),
          timezone: x.timezone
        });
        this.loading = false;
      },
      error: e => {
        this.error = apiErrorMessage(e, 'Unable to load settings.');
        this.loading = false;
      }
    });
  }

  async save() {
    const value = this.form.getRawValue();
    const nextTotal = Number(value.totalAcres);
    const message = nextTotal !== this.originalTotal
      ? `Change project target from ${this.originalTotal} to ${nextTotal} acres?`
      : 'Save project settings?';
    if (!(await this.dialog.confirm('Confirm changes', message))) return;

    this.saving = true;
    this.saveError = '';
    this.saved = false;
    try {
      await firstValueFrom(this.api.updateProject({
        projectName: value.projectName,
        totalAcres: nextTotal,
        startDate: value.startDate,
        deadlineLocal: value.deadlineLocal,
        timezone: value.timezone
      }));
      this.saved = true;
      this.originalTotal = nextTotal;
      await firstValueFrom(this.dashboard.get());
    } catch (e: any) {
      this.saveError = apiErrorMessage(e, 'Unable to save settings');
    } finally {
      this.saving = false;
    }
  }

  private toInput(value: string) {
    if (!value) return '';
    if (value.length >= 16 && value.includes('T')) return value.slice(0, 16);
    const d = new Date(value);
    const pad = (n: number) => String(n).padStart(2, '0');
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
  }
}
