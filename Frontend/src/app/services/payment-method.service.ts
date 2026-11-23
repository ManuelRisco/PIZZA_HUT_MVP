import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { PaymentMethodDTO } from '../models/payment-method.interface';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root',
})
export class PaymentMethodService {
  private readonly apiUrl = `${environment.apiUrl}/payment-methods`;
  private readonly httpOptions = {
    headers: new HttpHeaders({ 'Content-Type': 'application/json' }),
  };

  constructor(private readonly http: HttpClient) {}

  listarMetodosPago(includeDeleted: boolean = false): Observable<PaymentMethodDTO[]> {
    let params = new HttpParams();
    if (includeDeleted) {
      params = params.set('includeDeleted', 'true');
    }
    return this.http
      .get<any>(`${this.apiUrl}`, { params })
      .pipe(map((res) => (res.data !== undefined ? res.data : res)));
  }

  listarMetodosPagoActivos(): Observable<PaymentMethodDTO[]> {
    return this.http
      .get<any>(`${this.apiUrl}/active`)
      .pipe(map((res) => (res.data !== undefined ? res.data : res)));
  }

  obtenerMetodoPagoPorId(id: number): Observable<PaymentMethodDTO> {
    return this.http
      .get<any>(`${this.apiUrl}/${id}`)
      .pipe(map((res) => (res.data !== undefined ? res.data : res)));
  }

  crearMetodoPago(metodoPago: PaymentMethodDTO): Observable<PaymentMethodDTO> {
    return this.http
      .post<any>(`${this.apiUrl}`, metodoPago, this.httpOptions)
      .pipe(map((res) => (res.data !== undefined ? res.data : res)));
  }

  actualizarMetodoPago(id: number, metodoPago: PaymentMethodDTO): Observable<PaymentMethodDTO> {
    return this.http
      .put<any>(`${this.apiUrl}/${id}`, metodoPago, this.httpOptions)
      .pipe(map((res) => (res.data !== undefined ? res.data : res)));
  }

  eliminarMetodoPago(id: number): Observable<any> {
    return this.http
      .delete<any>(`${this.apiUrl}/${id}`)
      .pipe(map((res) => (res.data !== undefined ? res.data : res)));
  }

  restaurarMetodoPago(id: number): Observable<any> {
    return this.http
      .patch<any>(`${this.apiUrl}/${id}/restore`, {}, this.httpOptions)
      .pipe(map((res) => (res.data !== undefined ? res.data : res)));
  }

  cambiarEstado(id: number, isActive: boolean): Observable<PaymentMethodDTO> {
    return this.http
      .patch<any>(`${this.apiUrl}/${id}/status?active=${isActive}`, {}, this.httpOptions)
      .pipe(map((res) => (res.data !== undefined ? res.data : res)));
  }
}
