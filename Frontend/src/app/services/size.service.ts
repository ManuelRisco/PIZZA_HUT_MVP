import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { SizeDTO } from '../models/admin.interface';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class Size {
  private readonly apiUrl= environment.apiUrl;
  private readonly httpOptions= {
    headers: new HttpHeaders({ 'Content-Type': 'application/json' })
  };

  constructor(private readonly http: HttpClient) {}

  // Operaciones CRUD para Tamaños
  obtenerTodos(): Observable<SizeDTO[]> {
    return this.http.get<any>(`${this.apiUrl}/sizes`).pipe(map(res => res.data !== undefined ? res.data : res));
  }

  obtenerPorId(id: number): Observable<SizeDTO> {
    return this.http.get<any>(`${this.apiUrl}/sizes/${id}`).pipe(map(res => res.data !== undefined ? res.data : res));
  }

  crear(size: SizeDTO): Observable<SizeDTO> {
    return this.http.post<any>(`${this.apiUrl}/sizes`, size, this.httpOptions).pipe(map(res => res.data !== undefined ? res.data : res));
  }

  actualizar(id: number, size: SizeDTO): Observable<SizeDTO> {
    return this.http.put<any>(`${this.apiUrl}/sizes/${id}`, size, this.httpOptions).pipe(map(res => res.data !== undefined ? res.data : res));
  }

  eliminar(id: number): Observable<any> {
    return this.http.delete<any>(`${this.apiUrl}/sizes/${id}`).pipe(map(res => res.data !== undefined ? res.data : res));
  }
}

