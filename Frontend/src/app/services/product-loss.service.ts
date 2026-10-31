import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import {
  LossesSummaryDTO,
  ProductLossCreateDTO,
  ProductLossDTO
} from '../models/admin.interface';

@Injectable({
  providedIn: 'root'
})
export class ProductLossService {
  private readonly apiUrl = `${environment.apiUrl}/losses`;

  constructor(private readonly http: HttpClient) {}

  listarPerdidas(filters?: {
    startDate?: string;
    endDate?: string;
    category?: string;
    itemType?: string;
  }): Observable<ProductLossDTO[]> {
    let params = new HttpParams();
    if (filters?.startDate) {
      params = params.set('startDate', filters.startDate);
    }
    if (filters?.endDate) {
      params = params.set('endDate', filters.endDate);
    }
    if (filters?.category && filters.category !== 'TODOS') {
      params = params.set('category', filters.category);
    }
    if (filters?.itemType && filters.itemType !== 'TODOS') {
      params = params.set('itemType', filters.itemType);
    }
    return this.http.get<any>(this.apiUrl, { params }).pipe(
      map(res => (res.data !== undefined ? res.data : res))
    );
  }

  obtenerResumen(startDate?: string, endDate?: string): Observable<LossesSummaryDTO> {
    let params = new HttpParams();
    if (startDate) {
      params = params.set('startDate', startDate);
    }
    if (endDate) {
      params = params.set('endDate', endDate);
    }
    return this.http.get<any>(`${this.apiUrl}/summary`, { params }).pipe(
      map(res => (res.data !== undefined ? res.data : res))
    );
  }

  registrarPerdida(payload: ProductLossCreateDTO): Observable<ProductLossDTO> {
    return this.http.post<any>(this.apiUrl, payload).pipe(
      map(res => (res.data !== undefined ? res.data : res))
    );
  }

  actualizarPerdida(id: number, payload: ProductLossCreateDTO): Observable<ProductLossDTO> {
    return this.http.put<any>(`${this.apiUrl}/${id}`, payload).pipe(
      map(res => (res.data !== undefined ? res.data : res))
    );
  }

  eliminarPerdida(id: number, restoreStock = true): Observable<void> {
    const params = new HttpParams().set('restoreStock', String(restoreStock));
    return this.http.delete<any>(`${this.apiUrl}/${id}`, { params }).pipe(
      map(res => (res.data !== undefined ? res.data : res))
    );
  }
}

