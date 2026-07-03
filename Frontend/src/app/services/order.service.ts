import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { OrderDTO, OrderItemDTO, OrderCompleteDTO } from '../models/admin.interface';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class Order {
  private readonly apiUrl= environment.apiUrl;
  private readonly httpOptions= {
    headers: new HttpHeaders({ 'Content-Type': 'application/json' })
  };

  constructor(private readonly http: HttpClient) {}

  // Operaciones CRUD para Pedidos
  obtenerTodos(): Observable<OrderDTO[]> {
    return this.http.get<any>(`${this.apiUrl}/orders`).pipe(map(res => res.data !== undefined ? res.data : res));
  }
  
  // Obtener pedidos completos con toda la informaciÃ³n
  obtenerTodosCompletos(): Observable<OrderCompleteDTO[]> {
    return this.http.get<any>(`${this.apiUrl}/orders/complete`).pipe(map(res => res.data !== undefined ? res.data : res));
  }
  
  // Obtener items de un pedido con informaciÃ³n completa
  obtenerItemsCompletos(orderId: number): Observable<any[]> {
    return this.http.get<any>(`${this.apiUrl}/orders/${orderId}/items`).pipe(map(res => res.data !== undefined ? res.data : res));
  }

  obtenerPorId(id: number): Observable<OrderDTO> {
    return this.http.get<any>(`${this.apiUrl}/orders/${id}`).pipe(map(res => res.data !== undefined ? res.data : res));
  }

  obtenerPorUsuario(userId: number): Observable<OrderDTO[]> {
    return this.http.get<any>(`${this.apiUrl}/orders/user/${userId}`).pipe(map(res => res.data !== undefined ? res.data : res));
  }

  obtenerPorEstado(status: string): Observable<OrderDTO[]> {
    return this.http.get<any>(`${this.apiUrl}/orders/status/${status}`).pipe(map(res => res.data !== undefined ? res.data : res));
  }

  crear(order: OrderDTO): Observable<OrderDTO> {
    return this.http.post<any>(`${this.apiUrl}/orders`, order, this.httpOptions).pipe(map(res => res.data !== undefined ? res.data : res));
  }

  // Nuevo m\u00e9todo Checkout Unificado
  checkout(payload: any): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/orders/checkout`, payload, this.httpOptions)
      .pipe(map(res => res.data !== undefined ? res.data : res));
  }

  actualizarEstado(id: number, order: OrderDTO): Observable<OrderDTO> {
    return this.http.put<any>(`${this.apiUrl}/orders/${id}`, order, this.httpOptions).pipe(map(res => res.data !== undefined ? res.data : res));
  }

  cambiarEstado(id: number, nuevoEstado: string): Observable<OrderDTO> {
    return this.http.patch<any>(
      `${this.apiUrl}/orders/${id}/status`, 
      { status: nuevoEstado }, 
      this.httpOptions
    ).pipe(map(res => res.data !== undefined ? res.data : res));
  }

  eliminar(id: number): Observable<any> {
    return this.http.delete<any>(`${this.apiUrl}/orders/${id}`).pipe(map(res => res.data !== undefined ? res.data : res));
  }

  // Operaciones para Items de Pedido
  obtenerItemsPorPedido(orderId: number): Observable<OrderItemDTO[]> {
    return this.http.get<any>(`${this.apiUrl}/order-items/order/${orderId}`).pipe(map(res => res.data !== undefined ? res.data : res));
  }

  crearItem(item: OrderItemDTO): Observable<OrderItemDTO> {
    return this.http.post<any>(`${this.apiUrl}/order-items`, item, this.httpOptions).pipe(map(res => res.data !== undefined ? res.data : res));
  }

  actualizarItem(id: number, item: OrderItemDTO): Observable<OrderItemDTO> {
    return this.http.put<any>(`${this.apiUrl}/order-items/${id}`, item, this.httpOptions).pipe(map(res => res.data !== undefined ? res.data : res));
  }

  eliminarItem(id: number): Observable<any> {
    return this.http.delete<any>(`${this.apiUrl}/order-items/${id}`).pipe(map(res => res.data !== undefined ? res.data : res));
  }
}

