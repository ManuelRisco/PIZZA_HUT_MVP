import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../environments/environment';

export interface OrderItemExtraDTO {
  id?: number;
  orderItemId: number;
  ingredientId: number;
  ingredientName: string;
  extraCost: number;
}

@Injectable({
  providedIn: 'root'
})
export class OrderItemExtraService {
  private readonly apiUrl= `${environment.apiUrl}/order-item-extras`;
  private readonly httpOptions= {
    headers: new HttpHeaders({ 'Content-Type': 'application/json' })
  };

  constructor(private readonly http: HttpClient) {}

  listarTodos(): Observable<OrderItemExtraDTO[]> {
    return this.http.get<any>(this.apiUrl).pipe(map(res => res.data !== undefined ? res.data : res));
  }

  obtenerPorOrderItemId(orderItemId: number): Observable<OrderItemExtraDTO[]> {
    return this.http.get<any>(`${this.apiUrl}/order-item/${orderItemId}`).pipe(map(res => res.data !== undefined ? res.data : res));
  }

  crear(extra: OrderItemExtraDTO): Observable<OrderItemExtraDTO> {
    return this.http.post<any>(this.apiUrl, extra, this.httpOptions).pipe(map(res => res.data !== undefined ? res.data : res));
  }

  eliminarPorOrderItemId(orderItemId: number): Observable<void> {
    return this.http.delete<any>(`${this.apiUrl}/order-item/${orderItemId}`).pipe(map(res => res.data !== undefined ? res.data : res));
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<any>(`${this.apiUrl}/${id}`).pipe(map(res => res.data !== undefined ? res.data : res));
  }
}

