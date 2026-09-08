import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DashboardService } from '../services/dashboard.service';
import { Dashboard } from '../models/models';
import { AuthService } from '../core/auth/auth.service';
import { interval, Subscription, timer } from 'rxjs';
import { switchMap } from 'rxjs/operators';
import { apiErrorMessage } from '../core/api-error';
import { LoadingStateComponent, ErrorStateComponent } from '../shared/state.components';

@Component({
  standalone: true,
  imports: [CommonModule, LoadingStateComponent, ErrorStateComponent],
  template: `
    <main class="page">
      <ac-loading-state *ngIf="loading && !d" message="Loading dashboard…"></ac-loading-state>
      <ac-error-state *ngIf="error && !d" [message]="error" (retry)="load()"></ac-error-state>

      <div *ngIf="d as x">
        <div class="refresh-meta">
          <span *ngIf="lastUpdated">Last updated {{ lastUpdatedSeconds }}s ago</span>
          <span class="refresh-warn" *ngIf="refreshError">Unable to refresh · <button class="link-btn" (click)="load(true)">Retry</button></span>
        </div>

        <section class="card hero">
          <div class="hero-content">
            <div class="eyebrow" style="color:#d7eee5">{{ x.projectName }}</div>
            <h1 class="title" style="color:#fff;margin-top:8px">{{ greeting }}, {{ name }} 👋</h1>
            <p style="opacity:.85;margin:8px 0 0">Turning land into opportunities.</p>
          </div>
        </section>

        <section class="countdown">
          <div class="eyebrow">{{ complete ? 'Challenge Status' : 'Challenge Ends In' }}</div>
          <div *ngIf="!complete; else done" class="countdown-grid">
            <div class="countdown-tile"><strong>{{ days }}</strong><span>Days</span></div>
            <div class="countdown-tile"><strong>{{ hours | number:'2.0-0' }}</strong><span>Hours</span></div>
            <div class="countdown-tile"><strong>{{ minutes | number:'2.0-0' }}</strong><span>Minutes</span></div>
            <div class="countdown-tile"><strong>{{ seconds | number:'2.0-0' }}</strong><span>Seconds</span></div>
          </div>
          <ng-template #done><div class="digits" style="font-size:28px">DEADLINE PASSED</div></ng-template>
        </section>

        <section class="card" style="padding:18px;margin-top:14px">
          <div style="display:flex;justify-content:space-between;align-items:center">
            <div>
              <div class="eyebrow">Overall Progress</div>
              <strong style="font-size:28px">{{ x.soldAcres | number:'1.2-2' }} / {{ x.totalAcres | number:'1.2-2' }} acres</strong>
              <div class="muted">{{ x.progressPercent | number:'1.2-2' }}% of target</div>
            </div>
            <span class="pill">{{ statusLabel(x.status) }}</span>
          </div>
          <div class="progress-track" style="margin:12px 0 18px"><div class="progress-fill" [style.width.%]="visualProgress"></div></div>
          <div class="stats">
            <div class="stat"><strong>{{ x.soldAcres | number:'1.2-2' }}</strong><span>Sold</span></div>
            <div class="stat"><strong>{{ x.remainingAcres | number:'1.2-2' }}</strong><span>Remaining</span></div>
            <div class="stat"><strong>{{ x.totalAcres | number:'1.2-2' }}</strong><span>Target</span></div>
          </div>
        </section>

        <section class="card" style="padding:18px;margin-top:14px">
          <div class="toolbar"><h2 style="margin:0;font-size:18px">Sales Pace</h2><span class="pill">{{ statusLabel(x.status) }}</span></div>
          <div class="grid two" style="margin-top:14px">
            <div class="pace-card"><strong>{{ x.currentDailyRate | number:'1.2-2' }}</strong><span>Current acres/day</span></div>
            <div class="pace-card"><strong>{{ x.requiredDailyRate | number:'1.2-2' }}</strong><span>Required acres/day</span></div>
          </div>
          <p class="pace-diff" [class.ahead]="x.paceDifference >= 0" [class.behind]="x.paceDifference < 0">
            {{ paceDiffMessage(x.paceDifference) }}
          </p>
          <div *ngIf="x.projectedCompletionDate" style="margin-top:14px">
            <div class="eyebrow">Projected completion</div><strong>{{ x.projectedCompletionDate | date }}</strong>
          </div>
        </section>

        <section class="card" style="padding:18px;margin-top:14px">
          <div class="toolbar"><h2 style="margin:0;font-size:18px">Leaderboard</h2></div>
          <div class="list">
            <div class="list-item" *ngFor="let p of x.leaderboard" [class.highlight]="p.userId === auth.user?.id" [class.top1]="p.rank===1" [class.top2]="p.rank===2" [class.top3]="p.rank===3">
              <div class="rank" [class.top-rank]="p.rank <= 3">{{ p.rank }}</div>
              <div class="avatar">{{ p.name.charAt(0) }}</div>
              <div class="grow"><strong>{{ p.name }}</strong><div class="muted">{{ p.percentageOfTeamSales | number:'1.1-1' }}% of team</div></div>
              <strong>{{ p.acresSold | number:'1.2-2' }} ac</strong>
            </div>
          </div>
        </section>

        <section class="card" style="padding:18px;margin-top:14px">
          <div class="toolbar">
            <div><div class="eyebrow">My Performance</div><h2 style="margin:3px 0">Rank #{{ x.myPerformance.rank || '—' }}</h2></div>
            <strong style="font-size:22px">{{ x.myPerformance.acresSold | number:'1.2-2' }} ac</strong>
          </div>
          <div class="grid two" style="margin-top:14px">
            <div><div class="eyebrow">Of project target</div><strong>{{ x.myPerformance.percentageOfTarget | number:'1.1-1' }}%</strong></div>
            <div><div class="eyebrow">Total sales</div><strong>{{ x.myPerformance.salesCount }}</strong></div>
          </div>
        </section>

        <section class="card" style="padding:18px;margin-top:14px">
          <div class="toolbar"><h2 style="margin:0;font-size:18px">Recent Sales</h2></div>
          <div class="list">
            <div class="list-item" *ngFor="let s of x.recentSales">
              <div class="avatar">▣</div>
              <div class="grow"><strong>{{ s.plotReference || s.buyerName || 'Land sale' }}</strong><div class="muted">{{ s.saleDate | date }} · {{ s.salesperson }}</div></div>
              <strong>{{ s.acres | number:'1.2-2' }} ac</strong>
            </div>
          </div>
        </section>
      </div>
    </main>
  `
})
export class DashboardComponent implements OnInit, OnDestroy {
  private api = inject(DashboardService);
  auth = inject(AuthService);
  d?: Dashboard;
  name = 'there';
  greeting = 'Good Morning';
  days = 0; hours = 0; minutes = 0; seconds = 0;
  complete = false;
  visualProgress = 0;
  offset = 0;
  loading = true;
  error = '';
  refreshError = false;
  lastUpdated = 0;
  lastUpdatedSeconds = 0;
  private tickSub?: Subscription;
  private refreshSub?: Subscription;
  private metaSub?: Subscription;
  private onVisibility = () => this.handleVisibility();

  ngOnInit() {
    this.load();
    this.tickSub = interval(1000).subscribe(() => { this.tick(); this.updateMeta(); });
    document.addEventListener('visibilitychange', this.onVisibility);
    this.startRefreshPolling();
  }

  private handleVisibility() {
    if (document.visibilityState === 'visible') {
      if (this.d) {
        this.api.get().subscribe({
          next: x => { this.apply(x); this.refreshError = false; },
          error: () => { this.refreshError = true; }
        });
      }
      this.startRefreshPolling();
    } else {
      this.refreshSub?.unsubscribe();
    }
  }

  private startRefreshPolling() {
    this.refreshSub?.unsubscribe();
    if (document.hidden) return;
    this.refreshSub = timer(45000, 45000).pipe(switchMap(() => this.api.get())).subscribe({
      next: x => { this.apply(x); this.refreshError = false; },
      error: () => { this.refreshError = true; }
    });
  }

  load(force = false) {
    if (!force && this.d) return;
    if (!this.d) { this.loading = true; this.error = ''; }
    this.api.get().subscribe({
      next: x => { this.apply(x); this.loading = false; this.refreshError = false; },
      error: e => {
        if (!this.d) {
          this.loading = false;
          this.error = apiErrorMessage(e, 'Unable to load dashboard.');
        } else {
          this.refreshError = true;
        }
      }
    });
  }

  apply(x: Dashboard) {
    this.d = x;
    this.name = this.auth.user?.name?.split(' ')[0] || 'there';
    this.greeting = this.timeGreeting(x.timezone);
    this.offset = new Date(x.serverTime).getTime() - Date.now();
    this.lastUpdated = Date.now();
    this.tick();
  }

  updateMeta() {
    if (this.lastUpdated) {
      this.lastUpdatedSeconds = Math.floor((Date.now() - this.lastUpdated) / 1000);
    }
  }

  statusLabel(status: string) {
    return status.replaceAll('_', ' ');
  }

  paceDiffMessage(diff: number) {
    if (diff > 0) return `You're ahead by ${diff.toFixed(2)} acres/day`;
    if (diff < 0) return `You're behind by ${Math.abs(diff).toFixed(2)} acres/day`;
    return 'You are exactly on pace';
  }

  timeGreeting(timezone: string) {
    try {
      const hour = Number(new Intl.DateTimeFormat('en-US', { hour: 'numeric', hour12: false, timeZone: timezone }).format(new Date()));
      if (hour < 12) return 'Good Morning';
      if (hour < 17) return 'Good Afternoon';
      return 'Good Evening';
    } catch {
      return 'Hello';
    }
  }

  tick() {
    if (!this.d || !this.lastUpdated) return;
    const elapsed = Math.floor((Date.now() - this.lastUpdated) / 1000);
    const remaining = Math.max(0, this.d.secondsUntilDeadline - elapsed);
    if (remaining <= 0) {
      this.complete = true;
      this.days = this.hours = this.minutes = this.seconds = 0;
      return;
    }
    this.complete = false;
    let s = remaining;
    this.days = Math.floor(s / 86400); s %= 86400;
    this.hours = Math.floor(s / 3600); s %= 3600;
    this.minutes = Math.floor(s / 60);
    this.seconds = s % 60;
    this.visualProgress = Math.min(this.d.progressPercent, 100);
  }

  ngOnDestroy() {
    this.tickSub?.unsubscribe();
    this.refreshSub?.unsubscribe();
    this.metaSub?.unsubscribe();
    document.removeEventListener('visibilitychange', this.onVisibility);
  }
}
