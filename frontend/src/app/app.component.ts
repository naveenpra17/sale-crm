import { Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from './core/auth/auth.service';
import { DialogHostComponent } from './shared/dialog-host.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, CommonModule, DialogHostComponent],
  template: `
    <div class="offline-banner" *ngIf="offline" role="status">You're offline. Some data may be outdated.</div>
    <div class="splash" *ngIf="auth.initializing" role="status" aria-live="polite">
      <div class="splash-card card">
        <div class="brand" style="color:var(--green)">SALES</div>
        <div style="font-weight:800">CHALLENGE</div>
        <p class="muted" style="margin:12px 0 0">Restoring your session…</p>
      </div>
    </div>
    <router-outlet *ngIf="!auth.initializing"/>
    <ac-dialog-host/>
  `
})
export class AppComponent {
  auth = inject(AuthService);
  offline = !navigator.onLine;

  constructor() {
    window.addEventListener('online', () => (this.offline = false));
    window.addEventListener('offline', () => (this.offline = true));
  }
}
