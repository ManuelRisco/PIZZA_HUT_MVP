import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { CategoryDTO } from '../models/pizza.interface';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class CategoriaService {
  private readonly apiUrl = `${environment.apiUrl}/categories`;
  private readonly httpOptions = {
    headers: new HttpHeaders({ 'Content-Type': 'application/json' })
  };

  constructor(private readonly http: HttpClient) {}

  listarCategorias(includeDeleted: boolean = false): Observable<CategoryDTO[]> {
    let params = new HttpParams();
    if (includeDeleted) {
      params = params.set('includeDeleted', 'true');
    }
    return this.http.get<any>(this.apiUrl, { params }).pipe(
      map(res => res.data !== undefined ? res.data : res)
    );
  }

  obtenerCategoriaPorId(id: number): Observable<CategoryDTO> {
    return this.http.get<any>(`${this.apiUrl}/${id}`).pipe(
      map(res => res.data !== undefined ? res.data : res)
    );
  }

  crearCategoria(categoria: CategoryDTO): Observable<CategoryDTO> {
    return this.http.post<any>(this.apiUrl, categoria, this.httpOptions).pipe(
      map(res => res.data !== undefined ? res.data : res)
    );
  }

  actualizarCategoria(id: number, categoria: CategoryDTO): Observable<CategoryDTO> {
    return this.http.put<any>(`${this.apiUrl}/${id}`, categoria, this.httpOptions).pipe(
      map(res => res.data !== undefined ? res.data : res)
    );
  }

  eliminarCategoria(id: number): Observable<any> {
    return this.http.delete<any>(`${this.apiUrl}/${id}`).pipe(
      map(res => res.data !== undefined ? res.data : res)
    );
  }

  restaurarCategoria(id: number): Observable<any> {
    return this.http.patch<any>(`${this.apiUrl}/${id}/restore`, {}, this.httpOptions).pipe(
      map(res => res.data !== undefined ? res.data : res)
    );
  }
}
