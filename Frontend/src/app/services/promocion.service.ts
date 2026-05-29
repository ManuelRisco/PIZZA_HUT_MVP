import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

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
  private apiUrl = 'http://localhost:8089/api/promociones';

  constructor(private http: HttpClient) {}

  listarTodas(): Observable<Promotion[]> {
    return this.http.get<Promotion[]>(this.apiUrl);
  }

  listarActivas(): Observable<Promotion[]> {
    return this.http.get<Promotion[]>(`${this.apiUrl}/activas`);
  }

  listarActivasPorTipo(tipo: string): Observable<Promotion[]> {
    return this.http.get<Promotion[]>(`${this.apiUrl}/activas/${tipo}`);
  }

  obtenerPorId(id: number): Observable<Promotion> {
    return this.http.get<Promotion>(`${this.apiUrl}/${id}`);
  }

  obtenerPorCodigo(code: string): Observable<Promotion> {
    return this.http.get<Promotion>(`${this.apiUrl}/codigo/${code}`);
  }

  validarPromocion(code: string, orderTotal: number, userId?: number, items?: any[]): Observable<any> {
    const payload: any = { code, orderTotal };
    if (userId) {
      payload.userId = userId;
    }
    if (items) {
      payload.items = items;
    }
    return this.http.post(`${this.apiUrl}/validar`, payload);
  }

  crear(promotion: Promotion): Observable<Promotion> {
    return this.http.post<Promotion>(this.apiUrl, promotion);
  }

  actualizar(id: number, promotion: Promotion): Observable<Promotion> {
    return this.http.put<Promotion>(`${this.apiUrl}/${id}`, promotion);
  }

  eliminar(id: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/${id}`);
  }

  activar(id: number): Observable<Promotion> {
    return this.http.patch<Promotion>(`${this.apiUrl}/${id}/activar`, {});
  }

  desactivar(id: number): Observable<Promotion> {
    return this.http.patch<Promotion>(`${this.apiUrl}/${id}/desactivar`, {});
  }

  proximasAVencer(dias: number = 7): Observable<Promotion[]> {
    return this.http.get<Promotion[]>(`${this.apiUrl}/proximas-vencer?dias=${dias}`);
  }
}
