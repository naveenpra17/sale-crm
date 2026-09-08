import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SalesService } from '../services/sales.service';
import { LoadingStateComponent, ErrorStateComponent, EmptyStateComponent } from '../shared/state.components';
import { Sale } from '../models/models';

@Component({
  standalone: true,
  imports: [CommonModule, LoadingStateComponent, ErrorStateComponent, EmptyStateComponent],
  template: `
    <main class="page">
      <ac-loading-state *ngIf="loading && !sales.length"></ac-loading-state>
      <ac-error-state *ngIf="error" [message]="error" (retry)="load(true)"></ac-error-state>

      <section class="card" style="padding:18px" *ngIf="!error && (!loading || sales.length)">
        <div class="toolbar">
          <div><div class="eyebrow">Your sales history</div><h1 class="title">My Sales</h1></div>
        </div>
        <ac-empty-state *ngIf="!loading && !sales.length" title="No sales yet" message="Your recorded sales will appear here once they are added."></ac-empty-state>
        <div class="list">
          <div class="list-item" *ngFor="let s of sales">
            <div class="avatar">▣</div>
            <div class="grow">
              <strong>{{ s.plotReference || s.buyerName || 'Land sale' }}</strong>
              <div class="muted" style="font-size:12px">{{ s.saleDate | date }} · {{ s.buyerName || 'No buyer recorded' }}</div>
            </div>
            <strong>{{ s.acres | number:'1.2-2' }} ac</strong>
          </div>
        </div>
        <div class="toolbar" style="margin-top:12px" *ngIf="hasMore">
          <button class="btn btn-secondary" [disabled]="loadingMore" (click)="loadMore()">{{ loadingMore ? 'Loading…' : 'Load more' }}</button>
        </div>
      </section>
    </main>
  `
})
export class MySalesComponent implements OnInit {
  private api = inject(SalesService);
  sales: Sale[] = [];
  loading = true;
  loadingMore = false;
  error = '';
  page = 0;
  hasMore = false;

  ngOnInit() {
    this.load(true);
  }

  load(reset = false) {
    if (reset) {
      this.page = 0;
      this.sales = [];
    }
    this.loading = true;
    this.error = '';
    this.api.mySales(this.page).subscribe({
      next: x => {
        this.sales = reset ? (x.content || []) : [...this.sales, ...(x.content || [])];
        this.hasMore = !x.last;
        this.loading = false;
      },
      error: e => {
        this.error = e?.error?.message || 'Unable to load sales.';
        this.loading = false;
      }
    });
  }

  loadMore() {
    if (!this.hasMore || this.loadingMore) return;
    this.loadingMore = true;
    this.page += 1;
    this.api.mySales(this.page).subscribe({
      next: x => {
        this.sales = [...this.sales, ...(x.content || [])];
        this.hasMore = !x.last;
        this.loadingMore = false;
      },
      error: () => {
        this.page -= 1;
        this.loadingMore = false;
      }
    });
  }
}
