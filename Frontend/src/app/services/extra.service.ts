import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../environments/environment';

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
  private readonly apiUrl= `${environment.apiUrl}/extras`;

  constructor(private readonly http: HttpClient) {}

  listarTodos(): Observable<Extra[]> {
    return this.http.get<any>(this.apiUrl).pipe(map(res => res.data !== undefined ? res.data : res));
  }

  listarDisponibles(): Observable<Extra[]> {
    return this.http.get<any>(`${this.apiUrl}/disponibles`).pipe(map(res => res.data !== undefined ? res.data : res));
  }

  listarPorCategoria(categoria: string): Observable<Extra[]> {
    return this.http.get<any>(`${this.apiUrl}/categoria/${categoria}`).pipe(map(res => res.data !== undefined ? res.data : res));
  }

  obtenerPorId(id: number): Observable<Extra> {
    return this.http.get<any>(`${this.apiUrl}/${id}`).pipe(map(res => res.data !== undefined ? res.data : res));
  }

  buscarPorNombre(nombre: string): Observable<Extra[]> {
    return this.http.get<any>(`${this.apiUrl}/buscar?nombre=${nombre}`).pipe(map(res => res.data !== undefined ? res.data : res));
  }

  crear(extra: Extra): Observable<Extra> {
    return this.http.post<any>(this.apiUrl, extra).pipe(map(res => res.data !== undefined ? res.data : res));
  }

  actualizar(id: number, extra: Extra): Observable<Extra> {
    return this.http.put<any>(`${this.apiUrl}/${id}`, extra).pipe(map(res => res.data !== undefined ? res.data : res));
  }

  eliminar(id: number): Observable<any> {
    return this.http.delete<any>(`${this.apiUrl}/${id}`).pipe(map(res => res.data !== undefined ? res.data : res));
  }

  cambiarDisponibilidad(id: number): Observable<Extra> {
    return this.http.patch<any>(`${this.apiUrl}/${id}/disponibilidad`, {}).pipe(map(res => res.data !== undefined ? res.data : res));
  }
}

