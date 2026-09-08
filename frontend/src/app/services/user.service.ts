import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { AppConfigService } from '../core/config/app-config.service';

@Injectable({ providedIn: 'root' })
export class UserService {
  private http = inject(HttpClient);
  private config = inject(AppConfigService);

  changePassword(currentPassword: string, newPassword: string, confirmPassword: string) {
    return this.http.post(`${this.config.apiBaseUrl}/me/password`, {
      currentPassword,
      newPassword,
      confirmPassword
    });
  }
}
