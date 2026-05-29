import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { PaymentMethodDTO } from '../models/payment-method.interface';

@Injectable({
  providedIn: 'root'
})
export class PaymentMethodService {
  private apiUrl = 'http://localhost:8089/api/payment-methods';
  private httpOptions = {
    headers: new HttpHeaders({ 'Content-Type': 'application/json' })
  };

  constructor(private http: HttpClient) { }

  // Listar todos los métodos de pago
  listarMetodosPago(): Observable<PaymentMethodDTO[]> {
    return this.http.get<PaymentMethodDTO[]>(`${this.apiUrl}`);
  }

  // Listar solo métodos de pago activos
  listarMetodosPagoActivos(): Observable<PaymentMethodDTO[]> {
    return this.http.get<PaymentMethodDTO[]>(`${this.apiUrl}/active`);
  }

  // Obtener método de pago por ID
  obtenerMetodoPagoPorId(id: number): Observable<PaymentMethodDTO> {
    return this.http.get<PaymentMethodDTO>(`${this.apiUrl}/${id}`);
  }

  // Crear nuevo método de pago
  crearMetodoPago(metodoPago: PaymentMethodDTO): Observable<PaymentMethodDTO> {
    return this.http.post<PaymentMethodDTO>(`${this.apiUrl}`, metodoPago, this.httpOptions);
  }

  // Actualizar método de pago existente
  actualizarMetodoPago(id: number, metodoPago: PaymentMethodDTO): Observable<PaymentMethodDTO> {
    return this.http.put<PaymentMethodDTO>(`${this.apiUrl}/${id}`, metodoPago, this.httpOptions);
  }

  // Eliminar método de pago
  eliminarMetodoPago(id: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/${id}`);
  }

  // Cambiar estado (activar/desactivar)
  cambiarEstado(id: number, isActive: boolean): Observable<PaymentMethodDTO> {
    return this.http.patch<PaymentMethodDTO>(`${this.apiUrl}/${id}/status?active=${isActive}`, {}, this.httpOptions);
  }
}
