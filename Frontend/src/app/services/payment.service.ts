import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { PaymentDTO } from '../models/admin.interface';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class Payment {
  private readonly apiUrl= environment.apiUrl;
  private readonly httpOptions= {
    headers: new HttpHeaders({ 'Content-Type': 'application/json' })
  };

  constructor(private readonly http: HttpClient) {}

  // Operaciones CRUD para Pagos
  obtenerTodos(): Observable<PaymentDTO[]> {
    return this.http.get<any>(`${this.apiUrl}/payments`).pipe(map(res => res.data !== undefined ? res.data : res));
  }

  obtenerPorId(id: number): Observable<PaymentDTO> {
    return this.http.get<any>(`${this.apiUrl}/payments/${id}`).pipe(map(res => res.data !== undefined ? res.data : res));
  }

  obtenerPorPedido(orderId: number): Observable<PaymentDTO> {
    return this.http.get<any>(`${this.apiUrl}/payments/order/${orderId}`).pipe(map(res => res.data !== undefined ? res.data : res));
  }

  crear(payment: PaymentDTO): Observable<PaymentDTO> {
    return this.http.post<any>(`${this.apiUrl}/payments`, payment, this.httpOptions).pipe(map(res => res.data !== undefined ? res.data : res));
  }

  actualizar(id: number, payment: PaymentDTO): Observable<PaymentDTO> {
    return this.http.put<any>(`${this.apiUrl}/payments/${id}`, payment, this.httpOptions).pipe(map(res => res.data !== undefined ? res.data : res));
  }

  eliminar(id: number): Observable<any> {
    return this.http.delete<any>(`${this.apiUrl}/payments/${id}`).pipe(map(res => res.data !== undefined ? res.data : res));
  }

  crearTokenIzipay(amount: number): Observable<{ formToken: string }> {
    return this.http.post<any>(`${this.apiUrl}/payments/create-token`, { amount }, this.httpOptions).pipe(map(res => res.data !== undefined ? res.data : res));
  }
}

