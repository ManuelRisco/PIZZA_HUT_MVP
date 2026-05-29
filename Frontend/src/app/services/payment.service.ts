import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { PaymentDTO } from '../models/admin.interface';

@Injectable({
  providedIn: 'root'
})
export class Payment {
  private apiUrl = 'http://localhost:8089/api';
  private httpOptions = {
    headers: new HttpHeaders({ 'Content-Type': 'application/json' })
  };

  constructor(private http: HttpClient) {}

  // Operaciones CRUD para Pagos
  obtenerTodos(): Observable<PaymentDTO[]> {
    return this.http.get<PaymentDTO[]>(`${this.apiUrl}/payments`);
  }

  obtenerPorId(id: number): Observable<PaymentDTO> {
    return this.http.get<PaymentDTO>(`${this.apiUrl}/payments/${id}`);
  }

  obtenerPorPedido(orderId: number): Observable<PaymentDTO> {
    return this.http.get<PaymentDTO>(`${this.apiUrl}/payments/order/${orderId}`);
  }

  crear(payment: PaymentDTO): Observable<PaymentDTO> {
    return this.http.post<PaymentDTO>(`${this.apiUrl}/payments`, payment, this.httpOptions);
  }

  actualizar(id: number, payment: PaymentDTO): Observable<PaymentDTO> {
    return this.http.put<PaymentDTO>(`${this.apiUrl}/payments/${id}`, payment, this.httpOptions);
  }

  eliminar(id: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/payments/${id}`);
  }
}
