import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { ReviewDTO } from '../models/admin.interface';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class Review {
  private readonly apiUrl= environment.apiUrl;
  private readonly httpOptions= {
    headers: new HttpHeaders({ 'Content-Type': 'application/json' })
  };

  constructor(private readonly http: HttpClient) {}

  // Operaciones CRUD para ReseÃ±as
  obtenerTodos(): Observable<ReviewDTO[]> {
    return this.http.get<any>(`${this.apiUrl}/reviews`).pipe(map(res => res.data !== undefined ? res.data : res));
  }

  obtenerPorId(id: number): Observable<ReviewDTO> {
    return this.http.get<any>(`${this.apiUrl}/reviews/${id}`).pipe(map(res => res.data !== undefined ? res.data : res));
  }

  obtenerPorPedido(orderId: number): Observable<ReviewDTO[]> {
    return this.http.get<any>(`${this.apiUrl}/reviews/order/${orderId}`).pipe(map(res => res.data !== undefined ? res.data : res));
  }

  obtenerPorUsuario(userId: number): Observable<ReviewDTO[]> {
    return this.http.get<any>(`${this.apiUrl}/reviews/user/${userId}`).pipe(map(res => res.data !== undefined ? res.data : res));
  }

  crear(review: ReviewDTO): Observable<ReviewDTO> {
    return this.http.post<any>(`${this.apiUrl}/reviews`, review, this.httpOptions).pipe(map(res => res.data !== undefined ? res.data : res));
  }

  actualizar(id: number, review: ReviewDTO): Observable<ReviewDTO> {
    return this.http.put<any>(`${this.apiUrl}/reviews/${id}`, review, this.httpOptions).pipe(map(res => res.data !== undefined ? res.data : res));
  }

  eliminar(id: number): Observable<any> {
    return this.http.delete<any>(`${this.apiUrl}/reviews/${id}`).pipe(map(res => res.data !== undefined ? res.data : res));
  }

  desactivar(id: number): Observable<any> {
    return this.http.patch<any>(`${this.apiUrl}/reviews/${id}/desactivar`, {}, this.httpOptions).pipe(map(res => res.data !== undefined ? res.data : res));
  }

  activar(id: number): Observable<any> {
    return this.http.patch<any>(`${this.apiUrl}/reviews/${id}/activar`, {}, this.httpOptions).pipe(map(res => res.data !== undefined ? res.data : res));
  }
}

