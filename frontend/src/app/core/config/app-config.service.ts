import { Injectable, isDevMode } from '@angular/core';
import { environment } from '../../../environments/environment';

export interface AppRuntimeConfig {
  apiBaseUrl: string;
}

@Injectable({ providedIn: 'root' })
export class AppConfigService {
  private config: AppRuntimeConfig = { apiBaseUrl: environment.apiBaseUrl };

  async load(): Promise<void> {
    if (isDevMode()) {
      this.config = { apiBaseUrl: environment.apiBaseUrl };
      return;
    }
    try {
      const response = await fetch('/assets/config.json', { cache: 'no-store' });
      if (!response.ok) {
        throw new Error('config unavailable');
      }
      const json = await response.json();
      if (json?.apiBaseUrl) {
        this.config = { apiBaseUrl: String(json.apiBaseUrl).replace(/\/$/, '') };
      }
    } catch {
      this.config = { apiBaseUrl: environment.apiBaseUrl };
    }
  }

  get apiBaseUrl(): string {
    return this.config.apiBaseUrl;
  }
}
