import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { IngredientDTO } from '../models/admin.interface';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class IngredientService {
  private apiUrl = environment.apiUrl;
  private httpOptions = {
    headers: new HttpHeaders({
      'Content-Type': 'application/json'
    })
  };

  constructor(private http: HttpClient) {}

  obtenerTodos(): Observable<IngredientDTO[]> {
    return this.http.get<any>(`${this.apiUrl}/ingredients`).pipe(map(res => res.data !== undefined ? res.data : res));
  }

  obtenerPorId(id: number): Observable<IngredientDTO> {
    return this.http.get<any>(`${this.apiUrl}/ingredients/${id}`).pipe(map(res => res.data !== undefined ? res.data : res));
  }

  obtenerDisponibles(): Observable<IngredientDTO[]> {
    return this.http.get<any>(`${this.apiUrl}/ingredients/available`).pipe(map(res => res.data !== undefined ? res.data : res));
  }

  crear(ingredient: IngredientDTO): Observable<IngredientDTO> {
    return this.http.post<any>(`${this.apiUrl}/ingredients`, ingredient, this.httpOptions).pipe(map(res => res.data !== undefined ? res.data : res));
  }

  actualizar(id: number, ingredient: IngredientDTO): Observable<IngredientDTO> {
    return this.http.put<any>(`${this.apiUrl}/ingredients/${id}`, ingredient, this.httpOptions).pipe(map(res => res.data !== undefined ? res.data : res));
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<any>(`${this.apiUrl}/ingredients/${id}`).pipe(map(res => res.data !== undefined ? res.data : res));
  }

  cambiarDisponibilidad(id: number, disponible: boolean): Observable<IngredientDTO> {
    return this.http.patch<any>(`${this.apiUrl}/ingredients/${id}/availability?available=${disponible}`, {}, this.httpOptions).pipe(map(res => res.data !== undefined ? res.data : res));
  }
}
