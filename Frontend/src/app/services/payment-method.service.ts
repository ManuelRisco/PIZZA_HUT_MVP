import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { PaymentMethodDTO } from '../models/payment-method.interface';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class PaymentMethodService {
  private readonly apiUrl= `${environment.apiUrl}/payment-methods`;
  private readonly httpOptions= {
    headers: new HttpHeaders({ 'Content-Type': 'application/json' })
  };

  constructor(private readonly http: HttpClient) { }

  // Listar todos los mÃ©todos de pago
  listarMetodosPago(): Observable<PaymentMethodDTO[]> {
    return this.http.get<any>(`${this.apiUrl}`).pipe(map(res => res.data !== undefined ? res.data : res));
  }

  // Listar solo mÃ©todos de pago activos
  listarMetodosPagoActivos(): Observable<PaymentMethodDTO[]> {
    return this.http.get<any>(`${this.apiUrl}/active`).pipe(map(res => res.data !== undefined ? res.data : res));
  }

  // Obtener mÃ©todo de pago por ID
  obtenerMetodoPagoPorId(id: number): Observable<PaymentMethodDTO> {
    return this.http.get<any>(`${this.apiUrl}/${id}`).pipe(map(res => res.data !== undefined ? res.data : res));
  }

  // Crear nuevo mÃ©todo de pago
  crearMetodoPago(metodoPago: PaymentMethodDTO): Observable<PaymentMethodDTO> {
    return this.http.post<any>(`${this.apiUrl}`, metodoPago, this.httpOptions).pipe(map(res => res.data !== undefined ? res.data : res));
  }

  // Actualizar mÃ©todo de pago existente
  actualizarMetodoPago(id: number, metodoPago: PaymentMethodDTO): Observable<PaymentMethodDTO> {
    return this.http.put<any>(`${this.apiUrl}/${id}`, metodoPago, this.httpOptions).pipe(map(res => res.data !== undefined ? res.data : res));
  }

  // Eliminar mÃ©todo de pago
  eliminarMetodoPago(id: number): Observable<any> {
    return this.http.delete<any>(`${this.apiUrl}/${id}`).pipe(map(res => res.data !== undefined ? res.data : res));
  }

  // Cambiar estado (activar/desactivar)
  cambiarEstado(id: number, isActive: boolean): Observable<PaymentMethodDTO> {
    return this.http.patch<any>(`${this.apiUrl}/${id}/status?active=${isActive}`, {}, this.httpOptions).pipe(map(res => res.data !== undefined ? res.data : res));
  }
}

