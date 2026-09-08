import { Injectable, signal } from '@angular/core';

export interface DialogState {
  title: string;
  message: string;
  confirmLabel?: string;
  cancelLabel?: string;
  type?: 'confirm' | 'alert' | 'prompt';
  inputLabel?: string;
  inputValue?: string;
  inputType?: string;
}

@Injectable({ providedIn: 'root' })
export class DialogService {
  state = signal<DialogState | null>(null);
  private resolver: ((value: string | boolean | null) => void) | null = null;

  confirm(title: string, message: string, confirmLabel = 'Confirm'): Promise<boolean> {
    return new Promise(resolve => {
      this.resolver = v => resolve(!!v);
      this.state.set({ title, message, confirmLabel, cancelLabel: 'Cancel', type: 'confirm' });
    });
  }

  alert(title: string, message: string): Promise<void> {
    return new Promise(resolve => {
      this.resolver = () => resolve();
      this.state.set({ title, message, confirmLabel: 'OK', type: 'alert' });
    });
  }

  prompt(title: string, message: string, inputLabel = 'Value', inputType = 'text'): Promise<string | null> {
    return new Promise(resolve => {
      this.resolver = v => resolve(typeof v === 'string' ? v : null);
      this.state.set({ title, message, inputLabel, inputValue: '', inputType, confirmLabel: 'Save', cancelLabel: 'Cancel', type: 'prompt' });
    });
  }

  resolve(value: string | boolean | null) {
    this.resolver?.(value);
    this.resolver = null;
    this.state.set(null);
  }
}
