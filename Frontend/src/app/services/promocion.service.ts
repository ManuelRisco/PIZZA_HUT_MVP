import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../environments/environment';

export interface Promotion {
  id?: number;
  code: string;
  name: string;
  description?: string;
  discountType: 'PERCENTAGE' | 'FIXED_AMOUNT' | 'BUNDLE';
  discountValue?: number;
  finalPrice?: number;
  minPurchase?: number;
  maxDiscount?: number;
  isActive: boolean;
  startDate: string;
  endDate: string;
  usageLimit?: number;
  usageCount?: number;
  applicableTo: 'ALL' | 'PIZZAS' | 'EXTRAS' | 'SPECIFIC_PRODUCTS';
  createdAt?: string;
  updatedAt?: string;
  deletedAt?: string;
  currentlyActive?: boolean;
  remainingUses?: number;
}

@Injectable({
  providedIn: 'root'
})
export class PromocionService {
  private readonly apiUrl= `${environment.apiUrl}/promociones`;

  constructor(private readonly http: HttpClient) {}

  listarTodas(): Observable<Promotion[]> {
    return this.http.get<any>(this.apiUrl).pipe(map(res => res.data !== undefined ? res.data : res));
  }

  listarActivas(): Observable<Promotion[]> {
    return this.http.get<any>(`${this.apiUrl}/activas`).pipe(map(res => res.data !== undefined ? res.data : res));
  }

  listarActivasPorTipo(tipo: string): Observable<Promotion[]> {
    return this.http.get<any>(`${this.apiUrl}/activas/${tipo}`).pipe(map(res => res.data !== undefined ? res.data : res));
  }

  obtenerPorId(id: number): Observable<Promotion> {
    return this.http.get<any>(`${this.apiUrl}/${id}`).pipe(map(res => res.data !== undefined ? res.data : res));
  }

  obtenerPorCodigo(code: string): Observable<Promotion> {
    return this.http.get<any>(`${this.apiUrl}/codigo/${code}`).pipe(map(res => res.data !== undefined ? res.data : res));
  }

  validarPromocion(code: string, orderTotal: number, userId?: number, items?: any[]): Observable<any> {
    const payload: any = { code, orderTotal };
    if (userId) {
      payload.userId = userId;
    }
    if (items) {
      payload.items = items;
    }
    return this.http.post<any>(`${this.apiUrl}/validar`, payload).pipe(map(res => res.data !== undefined ? res.data : res));
  }

  crear(promotion: Promotion): Observable<Promotion> {
    return this.http.post<any>(this.apiUrl, promotion).pipe(map(res => res.data !== undefined ? res.data : res));
  }

  actualizar(id: number, promotion: Promotion): Observable<Promotion> {
    return this.http.put<any>(`${this.apiUrl}/${id}`, promotion).pipe(map(res => res.data !== undefined ? res.data : res));
  }

  eliminar(id: number): Observable<any> {
    return this.http.delete<any>(`${this.apiUrl}/${id}`).pipe(map(res => res.data !== undefined ? res.data : res));
  }

  activar(id: number): Observable<Promotion> {
    return this.http.patch<any>(`${this.apiUrl}/${id}/activar`, {}).pipe(map(res => res.data !== undefined ? res.data : res));
  }

  desactivar(id: number): Observable<Promotion> {
    return this.http.patch<any>(`${this.apiUrl}/${id}/desactivar`, {}).pipe(map(res => res.data !== undefined ? res.data : res));
  }

  proximasAVencer(dias: number = 7): Observable<Promotion[]> {
    return this.http.get<any>(`${this.apiUrl}/proximas-vencer?dias=${dias}`).pipe(map(res => res.data !== undefined ? res.data : res));
  }
}

