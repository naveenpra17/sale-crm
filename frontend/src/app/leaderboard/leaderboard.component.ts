import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DashboardService } from '../services/dashboard.service';
import { AuthService } from '../core/auth/auth.service';
import { LoadingStateComponent, ErrorStateComponent } from '../shared/state.components';

@Component({
  standalone: true,
  imports: [CommonModule, LoadingStateComponent, ErrorStateComponent],
  template: `
    <main class="page">
      <ac-loading-state *ngIf="loading"></ac-loading-state>
      <ac-error-state *ngIf="error" [message]="error" (retry)="load()"></ac-error-state>
      <section class="card" style="padding:18px" *ngIf="!loading && !error">
        <div class="eyebrow">Team competition</div>
        <h1 class="title">Leaderboard</h1>
        <div class="list">
          <div class="list-item" *ngFor="let p of board" [class.highlight]="p.userId === auth.user?.id" [class.top1]="p.rank===1" [class.top2]="p.rank===2" [class.top3]="p.rank===3">
            <div class="rank" [class.top-rank]="p.rank <= 3">{{ p.rank }}</div>
            <div class="avatar">{{ p.name.charAt(0) }}</div>
            <div class="grow"><strong>{{ p.name }}</strong><div class="muted">{{ p.percentageOfTeamSales | number:'1.1-1' }}% of team sales</div></div>
            <strong>{{ p.acresSold | number:'1.2-2' }} ac</strong>
          </div>
        </div>
        <section class="card" style="padding:14px;margin-top:14px;box-shadow:none;background:#f3faf6" *ngIf="current">
          <div class="eyebrow">Your position</div>
          <strong>#{{ current.rank || '—' }} · {{ current.acresSold | number:'1.2-2' }} acres</strong>
        </section>
      </section>
    </main>
  `
})
export class LeaderboardComponent implements OnInit {
  private api = inject(DashboardService);
  auth = inject(AuthService);
  board: any[] = [];
  current: any;
  loading = true;
  error = '';

  ngOnInit() { this.load(); }

  load() {
    this.loading = true;
    this.error = '';
    this.api.leaderboard().subscribe({
      next: x => {
        this.board = x.entries;
        this.current = x.currentUser;
        this.loading = false;
      },
      error: e => {
        this.error = e?.friendlyMessage || e?.error?.message || 'Unable to load leaderboard.';
        this.loading = false;
      }
    });
  }
}
