import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DialogService } from './dialog.service';

@Component({
  selector: 'ac-dialog-host',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="dialog-backdrop" *ngIf="dialog.state() as d" role="dialog" aria-modal="true" [attr.aria-label]="d.title">
      <section class="card dialog-panel">
        <h2 class="title" style="font-size:20px">{{ d.title }}</h2>
        <p class="muted" style="margin:10px 0 16px">{{ d.message }}</p>
        <div class="field" *ngIf="d.type === 'prompt'">
          <label>{{ d.inputLabel }}</label>
          <input [type]="d.inputType || 'text'" [(ngModel)]="inputValue" (keyup.enter)="submitPrompt()">
        </div>
        <div class="toolbar" style="margin-top:16px">
          <button class="btn btn-secondary" *ngIf="d.type !== 'alert'" type="button" (click)="dialog.resolve(false)">{{ d.cancelLabel || 'Cancel' }}</button>
          <button class="btn btn-primary" type="button" (click)="confirm()">{{ d.confirmLabel || 'OK' }}</button>
        </div>
      </section>
    </div>
  `
})
export class DialogHostComponent {
  dialog = inject(DialogService);
  inputValue = '';

  confirm() {
    const d = this.dialog.state();
    if (!d) return;
    if (d.type === 'prompt') {
      this.dialog.resolve(this.inputValue);
    } else if (d.type === 'confirm') {
      this.dialog.resolve(true);
    } else {
      this.dialog.resolve(true);
    }
    this.inputValue = '';
  }

  submitPrompt() {
    this.confirm();
  }
}
