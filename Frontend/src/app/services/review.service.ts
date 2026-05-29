import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ReviewDTO } from '../models/admin.interface';

@Injectable({
  providedIn: 'root'
})
export class Review {
  private apiUrl = 'http://localhost:8089/api';
  private httpOptions = {
    headers: new HttpHeaders({ 'Content-Type': 'application/json' })
  };

  constructor(private http: HttpClient) {}

  // Operaciones CRUD para Reseñas
  obtenerTodos(): Observable<ReviewDTO[]> {
    return this.http.get<ReviewDTO[]>(`${this.apiUrl}/reviews`);
  }

  obtenerPorId(id: number): Observable<ReviewDTO> {
    return this.http.get<ReviewDTO>(`${this.apiUrl}/reviews/${id}`);
  }

  obtenerPorPedido(orderId: number): Observable<ReviewDTO[]> {
    return this.http.get<ReviewDTO[]>(`${this.apiUrl}/reviews/order/${orderId}`);
  }

  obtenerPorUsuario(userId: number): Observable<ReviewDTO[]> {
    return this.http.get<ReviewDTO[]>(`${this.apiUrl}/reviews/user/${userId}`);
  }

  crear(review: ReviewDTO): Observable<ReviewDTO> {
    return this.http.post<ReviewDTO>(`${this.apiUrl}/reviews`, review, this.httpOptions);
  }

  actualizar(id: number, review: ReviewDTO): Observable<ReviewDTO> {
    return this.http.put<ReviewDTO>(`${this.apiUrl}/reviews/${id}`, review, this.httpOptions);
  }

  eliminar(id: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/reviews/${id}`);
  }

  desactivar(id: number): Observable<any> {
    return this.http.patch(`${this.apiUrl}/reviews/${id}/desactivar`, {}, this.httpOptions);
  }

  activar(id: number): Observable<any> {
    return this.http.patch(`${this.apiUrl}/reviews/${id}/activar`, {}, this.httpOptions);
  }
}
