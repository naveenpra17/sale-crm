import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Dashboard, LeaderboardResponse } from '../models/models';
import { Observable } from 'rxjs';
import { AppConfigService } from '../core/config/app-config.service';

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private http = inject(HttpClient);
  private config = inject(AppConfigService);

  get(): Observable<Dashboard> {
    return this.http.get<Dashboard>(`${this.config.apiBaseUrl}/dashboard`);
  }

  leaderboard(): Observable<LeaderboardResponse> {
    return this.http.get<LeaderboardResponse>(`${this.config.apiBaseUrl}/leaderboard`);
  }
}
