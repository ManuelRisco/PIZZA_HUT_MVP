import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface FavoriteDTO {
  userId: number;
  pizzaId: number;
  createdAt?: string;
}

@Injectable({
  providedIn: 'root'
})
export class FavoriteService {
  private http = inject(HttpClient);
  private apiUrl = 'http://localhost:8089/api/favorites';

  listarFavoritos(): Observable<FavoriteDTO[]> {
    return this.http.get<FavoriteDTO[]>(this.apiUrl);
  }

  obtenerFavoritosPorUserId(userId: number): Observable<FavoriteDTO[]> {
    return this.http.get<FavoriteDTO[]>(`${this.apiUrl}/user/${userId}`);
  }

  agregarFavorito(userId: number, pizzaId: number): Observable<FavoriteDTO> {
    return this.http.post<FavoriteDTO>(this.apiUrl, { userId, pizzaId });
  }

  eliminarFavorito(userId: number, pizzaId: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/${userId}/${pizzaId}`);
  }

  esFavorito(userId: number, pizzaId: number): Observable<FavoriteDTO> {
    return this.http.get<FavoriteDTO>(`${this.apiUrl}/${userId}/${pizzaId}`);
  }
}
