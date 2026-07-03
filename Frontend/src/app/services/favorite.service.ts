import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../environments/environment';

export interface FavoriteDTO {
  userId: number;
  pizzaId: number;
  createdAt?: string;
}

@Injectable({
  providedIn: 'root'
})
export class FavoriteService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl= `${environment.apiUrl}/favorites`;

  listarFavoritos(): Observable<FavoriteDTO[]> {
    return this.http.get<any>(this.apiUrl).pipe(map(res => res.data !== undefined ? res.data : res));
  }

  obtenerFavoritosPorUserId(userId: number): Observable<FavoriteDTO[]> {
    return this.http.get<any>(`${this.apiUrl}/user/${userId}`).pipe(map(res => res.data !== undefined ? res.data : res));
  }

  agregarFavorito(userId: number, pizzaId: number): Observable<FavoriteDTO> {
    return this.http.post<any>(this.apiUrl, { userId, pizzaId }).pipe(map(res => res.data !== undefined ? res.data : res));
  }

  eliminarFavorito(userId: number, pizzaId: number): Observable<any> {
    return this.http.delete<any>(`${this.apiUrl}/${userId}/${pizzaId}`).pipe(map(res => res.data !== undefined ? res.data : res));
  }

  esFavorito(userId: number, pizzaId: number): Observable<FavoriteDTO> {
    return this.http.get<any>(`${this.apiUrl}/${userId}/${pizzaId}`).pipe(map(res => res.data !== undefined ? res.data : res));
  }
}
