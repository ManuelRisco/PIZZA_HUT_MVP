import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { IngredientDTO } from '../models/admin.interface';

@Injectable({
  providedIn: 'root'
})
export class IngredientService {
  private apiUrl = 'http://localhost:8089/api';
  private httpOptions = {
    headers: new HttpHeaders({
      'Content-Type': 'application/json'
    })
  };

  constructor(private http: HttpClient) {}

  obtenerTodos(): Observable<IngredientDTO[]> {
    return this.http.get<IngredientDTO[]>(`${this.apiUrl}/ingredients`);
  }

  obtenerPorId(id: number): Observable<IngredientDTO> {
    return this.http.get<IngredientDTO>(`${this.apiUrl}/ingredients/${id}`);
  }

  obtenerDisponibles(): Observable<IngredientDTO[]> {
    return this.http.get<IngredientDTO[]>(`${this.apiUrl}/ingredients/available`);
  }

  crear(ingredient: IngredientDTO): Observable<IngredientDTO> {
    return this.http.post<IngredientDTO>(`${this.apiUrl}/ingredients`, ingredient, this.httpOptions);
  }

  actualizar(id: number, ingredient: IngredientDTO): Observable<IngredientDTO> {
    return this.http.put<IngredientDTO>(`${this.apiUrl}/ingredients/${id}`, ingredient, this.httpOptions);
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/ingredients/${id}`);
  }

  cambiarDisponibilidad(id: number, disponible: boolean): Observable<IngredientDTO> {
    return this.http.patch<IngredientDTO>(`${this.apiUrl}/ingredients/${id}/availability?available=${disponible}`, {}, this.httpOptions);
  }
}
