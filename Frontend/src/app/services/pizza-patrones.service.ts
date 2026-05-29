import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { PizzaDTO } from '../models/pizza.interface';

/**
 * Servicio que utiliza los patrones de diseño del backend para pizzas
 */
@Injectable({
  providedIn: 'root'
})
export class PizzaPatronesService {
  private apiUrl = 'http://localhost:8089/api/pizzas/patrones';

  constructor(private http: HttpClient) { }

  /**
   * SPECIFICATION PATTERN - Filtrar pizzas disponibles
   */
  listarPizzasDisponibles(): Observable<PizzaDTO[]> {
    return this.http.get<PizzaDTO[]>(`${this.apiUrl}/disponibles`);
  }

  /**
   * SPECIFICATION PATTERN - Filtrar pizzas populares
   */
  listarPizzasPopulares(): Observable<PizzaDTO[]> {
    return this.http.get<PizzaDTO[]>(`${this.apiUrl}/populares`);
  }

  /**
   * SPECIFICATION PATTERN - Filtrar pizzas por categoría
   */
  listarPizzasPorCategoria(categoryId: number): Observable<PizzaDTO[]> {
    return this.http.get<PizzaDTO[]>(`${this.apiUrl}/categoria/${categoryId}`);
  }

  /**
   * COMPOSITE PATTERN - Filtrar pizzas disponibles Y populares
   */
  listarPizzasDisponiblesYPopulares(): Observable<PizzaDTO[]> {
    return this.http.get<PizzaDTO[]>(`${this.apiUrl}/disponibles-populares`);
  }

  /**
   * DECORATOR PATTERN - Calcular precio con extras
   */
  calcularPrecioConExtras(pizzaId: number, extras: string[]): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/${pizzaId}/extras`, { extras });
  }

  /**
   * BUILDER PATTERN - Crear pizza personalizada
   */
  crearPizzaPersonalizada(pizzaData: any): Observable<PizzaDTO> {
    return this.http.post<PizzaDTO>(`${this.apiUrl}/personalizada`, pizzaData);
  }
}
