import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { ProfitLossDashboardDTO } from '../models/admin.interface';

@Injectable({
  providedIn: 'root'
})
export class FinancialDashboardService {
  private readonly apiUrl = `${environment.apiUrl}/analytics`;

  constructor(private readonly http: HttpClient) {}

  obtenerEstadoResultados(options?: {
    startDate?: string;
    endDate?: string;
    groupBy?: 'DAY' | 'MONTH';
    onlyDelivered?: boolean;
  }): Observable<ProfitLossDashboardDTO> {
    let params = new HttpParams()
      .set('groupBy', options?.groupBy || 'DAY')
      .set('onlyDelivered', String(options?.onlyDelivered ?? false));

    if (options?.startDate) {
      params = params.set('startDate', options.startDate);
    }
    if (options?.endDate) {
      params = params.set('endDate', options.endDate);
    }

    return this.http.get<any>(`${this.apiUrl}/profit-loss`, { params }).pipe(
      map(res => (res.data !== undefined ? res.data : res))
    );
  }
}

