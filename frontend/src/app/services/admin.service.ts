import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { AppConfigService } from '../core/config/app-config.service';

@Injectable({ providedIn: 'root' })
export class AdminService {
  private http = inject(HttpClient);
  private config = inject(AppConfigService);

  private url(path: string) {
    return `${this.config.apiBaseUrl}/admin${path}`;
  }

  project() {
    return this.http.get(this.url('/project'));
  }

  updateProject(v: any) {
    return this.http.put(this.url('/project'), v);
  }

  users(params = 'page=0&size=50') {
    return this.http.get<any>(`${this.url('/users')}?${params}`);
  }

  createUser(v: any) {
    return this.http.post(this.url('/users'), v);
  }

  updateUser(id: number, v: any) {
    return this.http.put(`${this.url('/users')}/${id}`, v);
  }

  resetPassword(id: number, newPassword: string) {
    return this.http.post(`${this.url('/users')}/${id}/reset-password`, { newPassword });
  }

  deactivate(id: number) {
    return this.http.delete(`${this.url('/users')}/${id}`);
  }

  reactivate(id: number) {
    return this.http.post(`${this.url('/users')}/${id}/reactivate`, {});
  }

  audit(params = 'page=0&size=20') {
    return this.http.get<any>(`${this.url('/audit-logs')}?${params}`);
  }
}
