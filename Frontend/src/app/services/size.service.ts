import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SizeDTO } from '../models/admin.interface';

@Injectable({
  providedIn: 'root'
})
export class Size {
  private apiUrl = 'http://localhost:8089/api';
  private httpOptions = {
    headers: new HttpHeaders({ 'Content-Type': 'application/json' })
  };

  constructor(private http: HttpClient) {}

  // Operaciones CRUD para Tamaños
  obtenerTodos(): Observable<SizeDTO[]> {
    return this.http.get<SizeDTO[]>(`${this.apiUrl}/sizes`);
  }

  obtenerPorId(id: number): Observable<SizeDTO> {
    return this.http.get<SizeDTO>(`${this.apiUrl}/sizes/${id}`);
  }

  crear(size: SizeDTO): Observable<SizeDTO> {
    return this.http.post<SizeDTO>(`${this.apiUrl}/sizes`, size, this.httpOptions);
  }

  actualizar(id: number, size: SizeDTO): Observable<SizeDTO> {
    return this.http.put<SizeDTO>(`${this.apiUrl}/sizes/${id}`, size, this.httpOptions);
  }

  eliminar(id: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/sizes/${id}`);
  }
}
