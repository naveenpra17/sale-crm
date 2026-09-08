import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'ac-loading-state',
  standalone: true,
  imports: [CommonModule],
  template: `<div class="state-card card" role="status"><div class="spinner"></div><p class="muted">{{message}}</p></div>`
})
export class LoadingStateComponent {
  @Input() message = 'Loading…';
}

@Component({
  selector: 'ac-error-state',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="state-card card" role="alert">
      <strong>{{title}}</strong>
      <p class="muted">{{message}}</p>
      <button class="btn btn-secondary" *ngIf="showRetry" (click)="retry.emit()">Try again</button>
    </div>
  `
})
export class ErrorStateComponent {
  @Input() title = 'Something went wrong';
  @Input() message = 'Please try again.';
  @Input() showRetry = true;
  @Output() retry = new EventEmitter<void>();
}

@Component({
  selector: 'ac-empty-state',
  standalone: true,
  imports: [CommonModule],
  template: `<div class="state-card card"><strong>{{title}}</strong><p class="muted">{{message}}</p></div>`
})
export class EmptyStateComponent {
  @Input() title = 'Nothing here yet';
  @Input() message = 'No records to display.';
}
