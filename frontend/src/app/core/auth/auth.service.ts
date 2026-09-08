import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, firstValueFrom } from 'rxjs';
import { User } from '../../models/models';
import { AppConfigService } from '../config/app-config.service';
import { AuthCoordinator } from './auth-coordinator';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private config = inject(AppConfigService);
  private tokenValue: string | null = null;
  private csrfTokenValue: string | null = null;
  private userSubject = new BehaviorSubject<User | null>(null);
  user$ = this.userSubject.asObservable();
  initializing = true;
  private initialized = false;

  private api(path: string) {
    return `${this.config.apiBaseUrl}${path}`;
  }

  /** In-memory CSRF token for cross-origin deployments (cookie is not readable via document.cookie). */
  get csrfToken() {
    return this.csrfTokenValue;
  }

  async fetchCsrf(): Promise<string> {
    const r = await firstValueFrom(
      this.http.get<{ token: string }>(this.api('/auth/csrf'), { withCredentials: true })
    );
    this.csrfTokenValue = r.token;
    return r.token;
  }

  async initialize() {
    if (this.initialized) return;
    this.initialized = true;
    try {
      await this.fetchCsrf();
      const r = await firstValueFrom(this.http.post<any>(this.api('/auth/refresh'), {}, { withCredentials: true }));
      this.tokenValue = r.accessToken;
      this.userSubject.next(r.user);
    } catch {
      this.tokenValue = null;
      this.csrfTokenValue = null;
      this.userSubject.next(null);
    } finally {
      this.initializing = false;
    }
  }

  async login(email: string, password: string) {
    await this.fetchCsrf();
    const r = await firstValueFrom(this.http.post<any>(this.api('/auth/login'), { email, password }, { withCredentials: true }));
    this.tokenValue = r.accessToken;
    this.userSubject.next(r.user);
  }

  async logout() {
    try {
      if (!this.csrfTokenValue) {
        await this.fetchCsrf();
      }
      await firstValueFrom(this.http.post(this.api('/auth/logout'), {}, { withCredentials: true }));
    } finally {
      this.tokenValue = null;
      this.csrfTokenValue = null;
      this.userSubject.next(null);
    }
  }

  get token() {
    return this.tokenValue;
  }

  get user() {
    return this.userSubject.value;
  }

  async refresh(): Promise<string> {
    if (!this.csrfTokenValue) {
      await this.fetchCsrf();
    }
    const payload = await AuthCoordinator.coordinateRefresh(async () => {
      const r = await firstValueFrom(this.http.post<any>(this.api('/auth/refresh'), {}, { withCredentials: true }));
      return { accessToken: r.accessToken as string, user: r.user as User };
    });
    this.tokenValue = payload.accessToken;
    this.userSubject.next(payload.user);
    return payload.accessToken;
  }
}
