import { Injectable, isDevMode } from '@angular/core';
import { environment } from '../../../environments/environment';

export interface AppRuntimeConfig {
  apiBaseUrl: string;
}

@Injectable({ providedIn: 'root' })
export class AppConfigService {
  private config: AppRuntimeConfig = { apiBaseUrl: environment.apiBaseUrl };
  private loaded = false;

  async load(): Promise<void> {
    if (isDevMode()) {
      this.config = { apiBaseUrl: environment.apiBaseUrl };
      this.loaded = true;
      return;
    }
    try {
      const response = await fetch('/assets/config.json', { cache: 'no-store' });
      if (!response.ok) {
        throw new Error(`config unavailable (${response.status})`);
      }
      const json = await response.json();
      if (json?.apiBaseUrl) {
        this.config = { apiBaseUrl: String(json.apiBaseUrl).replace(/\/$/, '') };
      } else {
        throw new Error('config.json missing apiBaseUrl');
      }
    } catch (err) {
      if (isDevMode()) {
        console.warn('[config] Failed to load /assets/config.json; using build-time fallback', err);
      }
      this.config = { apiBaseUrl: environment.apiBaseUrl };
    } finally {
      this.loaded = true;
    }
  }

  get isLoaded(): boolean {
    return this.loaded;
  }

  get apiBaseUrl(): string {
    return this.config.apiBaseUrl;
  }
}
