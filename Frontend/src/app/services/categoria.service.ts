import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CategoryDTO } from '../models/pizza.interface';

@Injectable({
  providedIn: 'root'
})
export class CategoriaService {
  private apiUrl = 'http://localhost:8089/api/categories';
  private httpOptions = {
    headers: new HttpHeaders({ 'Content-Type': 'application/json' })
  };

  constructor(private http: HttpClient) {}

  listarCategorias(): Observable<CategoryDTO[]> {
    return this.http.get<CategoryDTO[]>(this.apiUrl);
  }

  obtenerCategoriaPorId(id: number): Observable<CategoryDTO> {
    return this.http.get<CategoryDTO>(`${this.apiUrl}/${id}`);
  }

  crearCategoria(categoria: CategoryDTO): Observable<CategoryDTO> {
    return this.http.post<CategoryDTO>(this.apiUrl, categoria, this.httpOptions);
  }

  actualizarCategoria(id: number, categoria: CategoryDTO): Observable<CategoryDTO> {
    return this.http.put<CategoryDTO>(`${this.apiUrl}/${id}`, categoria, this.httpOptions);
  }

  eliminarCategoria(id: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/${id}`);
  }
}
