import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { OrderDTO, OrderItemDTO, OrderCompleteDTO } from '../models/admin.interface';

@Injectable({
  providedIn: 'root'
})
export class Order {
  private apiUrl = 'http://localhost:8089/api';
  private httpOptions = {
    headers: new HttpHeaders({ 'Content-Type': 'application/json' })
  };

  constructor(private http: HttpClient) {}

  // Operaciones CRUD para Pedidos
  obtenerTodos(): Observable<OrderDTO[]> {
    return this.http.get<OrderDTO[]>(`${this.apiUrl}/orders`);
  }
  
  // Obtener pedidos completos con toda la información
  obtenerTodosCompletos(): Observable<OrderCompleteDTO[]> {
    return this.http.get<OrderCompleteDTO[]>(`${this.apiUrl}/orders/complete`);
  }
  
  // Obtener items de un pedido con información completa
  obtenerItemsCompletos(orderId: number): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/orders/${orderId}/items`);
  }

  obtenerPorId(id: number): Observable<OrderDTO> {
    return this.http.get<OrderDTO>(`${this.apiUrl}/orders/${id}`);
  }

  obtenerPorUsuario(userId: number): Observable<OrderDTO[]> {
    return this.http.get<OrderDTO[]>(`${this.apiUrl}/orders/user/${userId}`);
  }

  obtenerPorEstado(status: string): Observable<OrderDTO[]> {
    return this.http.get<OrderDTO[]>(`${this.apiUrl}/orders/status/${status}`);
  }

  crear(order: OrderDTO): Observable<OrderDTO> {
    return this.http.post<OrderDTO>(`${this.apiUrl}/orders`, order, this.httpOptions);
  }

  actualizarEstado(id: number, order: OrderDTO): Observable<OrderDTO> {
    return this.http.put<OrderDTO>(`${this.apiUrl}/orders/${id}`, order, this.httpOptions);
  }

  cambiarEstado(id: number, nuevoEstado: string): Observable<OrderDTO> {
    return this.http.patch<OrderDTO>(
      `${this.apiUrl}/orders/${id}/status`, 
      { status: nuevoEstado }, 
      this.httpOptions
    );
  }

  eliminar(id: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/orders/${id}`);
  }

  // Operaciones para Items de Pedido
  obtenerItemsPorPedido(orderId: number): Observable<OrderItemDTO[]> {
    return this.http.get<OrderItemDTO[]>(`${this.apiUrl}/order-items/order/${orderId}`);
  }

  crearItem(item: OrderItemDTO): Observable<OrderItemDTO> {
    return this.http.post<OrderItemDTO>(`${this.apiUrl}/order-items`, item, this.httpOptions);
  }

  actualizarItem(id: number, item: OrderItemDTO): Observable<OrderItemDTO> {
    return this.http.put<OrderItemDTO>(`${this.apiUrl}/order-items/${id}`, item, this.httpOptions);
  }

  eliminarItem(id: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/order-items/${id}`);
  }
}
