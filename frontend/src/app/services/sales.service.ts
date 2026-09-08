import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Sale, Page } from '../models/models';
import { Observable } from 'rxjs';
import { AppConfigService } from '../core/config/app-config.service';

@Injectable({ providedIn: 'root' })
export class SalesService {
  private http = inject(HttpClient);
  private config = inject(AppConfigService);

  private url(path: string) {
    return `${this.config.apiBaseUrl}${path}`;
  }

  mySales(page = 0, size = 20): Observable<Page<Sale>> {
    return this.http.get<Page<Sale>>(`${this.url('/me/sales')}?page=${page}&size=${size}`);
  }

  adminSales(params = 'page=0&size=20') {
    return this.http.get<any>(`${this.url('/admin/sales')}?${params}`);
  }

  exportCsv(params = '') {
    return this.http.get(`${this.url('/admin/sales/export')}?${params}`, { responseType: 'blob' });
  }

  create(v: any) {
    return this.http.post(this.url('/admin/sales'), v);
  }

  update(id: number, v: any) {
    return this.http.put(`${this.url('/admin/sales')}/${id}`, v);
  }

  delete(id: number) {
    return this.http.delete(`${this.url('/admin/sales')}/${id}`);
  }
}
