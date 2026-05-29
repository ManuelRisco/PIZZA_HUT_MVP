import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';

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
  private apiUrl = 'http://localhost:8089/api/order-item-extras';
  private httpOptions = {
    headers: new HttpHeaders({ 'Content-Type': 'application/json' })
  };

  constructor(private http: HttpClient) {}

  listarTodos(): Observable<OrderItemExtraDTO[]> {
    return this.http.get<OrderItemExtraDTO[]>(this.apiUrl);
  }

  obtenerPorOrderItemId(orderItemId: number): Observable<OrderItemExtraDTO[]> {
    return this.http.get<OrderItemExtraDTO[]>(`${this.apiUrl}/order-item/${orderItemId}`);
  }

  crear(extra: OrderItemExtraDTO): Observable<OrderItemExtraDTO> {
    return this.http.post<OrderItemExtraDTO>(this.apiUrl, extra, this.httpOptions);
  }

  eliminarPorOrderItemId(orderItemId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/order-item/${orderItemId}`);
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
