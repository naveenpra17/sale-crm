import { Injectable, isDevMode } from '@angular/core';
import { environment } from '../../../environments/environment';

export interface AppRuntimeConfig {
  apiBaseUrl: string;
}

/**
 * When the SPA and API are on different origins, browsers block the HttpOnly
 * ACRES_REFRESH cookie (third-party). Production uses the Vercel /api proxy
 * so requests stay same-origin and refresh cookies work.
 */
export function resolveApiBaseUrl(configured: string, origin = ''): string {
  const url = configured.replace(/\/$/, '');
  if (!url || url.startsWith('/')) {
    return url || '/api';
  }
  if (origin && url.startsWith(origin)) {
    return url;
  }
  return '/api';
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
        const configured = String(json.apiBaseUrl);
        const resolved = resolveApiBaseUrl(configured, window.location.origin);
        if (resolved !== configured.replace(/\/$/, '')) {
          console.info('[config] API routed via same-origin proxy:', resolved);
        }
        this.config = { apiBaseUrl: resolved };
      } else {
        this.config = { apiBaseUrl: '/api' };
      }
    } catch (err) {
      if (isDevMode()) {
        console.warn('[config] Failed to load /assets/config.json; using /api proxy fallback', err);
      }
      this.config = { apiBaseUrl: '/api' };
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
