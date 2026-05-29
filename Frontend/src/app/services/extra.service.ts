import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Extra {
  id?: number;
  name: string;
  description?: string;
  price: number;
  category: 'BEBIDA' | 'POSTRE' | 'ENTRADA' | 'COMPLEMENTO';
  isAvailable: boolean;
  displayOrder?: number;
  createdAt?: string;
  updatedAt?: string;
  deletedAt?: string;
}

@Injectable({
  providedIn: 'root'
})
export class ExtraService {
  private apiUrl = 'http://localhost:8089/api/extras';

  constructor(private http: HttpClient) {}

  listarTodos(): Observable<Extra[]> {
    return this.http.get<Extra[]>(this.apiUrl);
  }

  listarDisponibles(): Observable<Extra[]> {
    return this.http.get<Extra[]>(`${this.apiUrl}/disponibles`);
  }

  listarPorCategoria(categoria: string): Observable<Extra[]> {
    return this.http.get<Extra[]>(`${this.apiUrl}/categoria/${categoria}`);
  }

  obtenerPorId(id: number): Observable<Extra> {
    return this.http.get<Extra>(`${this.apiUrl}/${id}`);
  }

  buscarPorNombre(nombre: string): Observable<Extra[]> {
    return this.http.get<Extra[]>(`${this.apiUrl}/buscar?nombre=${nombre}`);
  }

  crear(extra: Extra): Observable<Extra> {
    return this.http.post<Extra>(this.apiUrl, extra);
  }

  actualizar(id: number, extra: Extra): Observable<Extra> {
    return this.http.put<Extra>(`${this.apiUrl}/${id}`, extra);
  }

  eliminar(id: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/${id}`);
  }

  cambiarDisponibilidad(id: number): Observable<Extra> {
    return this.http.patch<Extra>(`${this.apiUrl}/${id}/disponibilidad`, {});
  }
}
