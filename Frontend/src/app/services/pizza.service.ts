import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { PizzaDTO, PizzaCreateDTO, CategoryDTO, IngredientDTO } from '../models/pizza.interface';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class PizzaService {
  private readonly apiUrl= environment.apiUrl;
  private readonly httpOptions= {
    headers: new HttpHeaders({ 'Content-Type': 'application/json' })
  };

  constructor(private readonly http: HttpClient) { }

  // Operaciones CRUD para Pizzas
  listarPizzas(): Observable<PizzaDTO[]> {
    return this.http.get<any>(`${this.apiUrl}/pizzas`).pipe(
      map(res => res.data ? res.data : res)
    );
  }

  obtenerPizzaPorId(id: number): Observable<PizzaDTO> {
    return this.http.get<any>(`${this.apiUrl}/pizzas/${id}`).pipe(
      map(res => res.data ? res.data : res)
    );
  }

  crearPizza(pizza: PizzaCreateDTO): Observable<PizzaDTO> {
    return this.http.post<any>(`${this.apiUrl}/pizzas`, pizza, this.httpOptions).pipe(
      map(res => res.data ? res.data : res)
    );
  }

  actualizarPizza(id: number, pizza: PizzaCreateDTO): Observable<PizzaDTO> {
    return this.http.put<any>(`${this.apiUrl}/pizzas/${id}`, pizza, this.httpOptions).pipe(
      map(res => res.data ? res.data : res)
    );
  }

  eliminarPizza(id: number): Observable<any> {
    return this.http.delete<any>(`${this.apiUrl}/pizzas/${id}`).pipe(
      map(res => res.data ? res.data : res)
    );
  }

  // Operaciones para Categorías
  listarCategorias(): Observable<CategoryDTO[]> {
    return this.http.get<any>(`${this.apiUrl}/categories`).pipe(
      map(res => res.data ? res.data : res)
    );
  }

  obtenerCategoriaPorId(id: number): Observable<CategoryDTO> {
    return this.http.get<any>(`${this.apiUrl}/categories/${id}`).pipe(
      map(res => res.data ? res.data : res)
    );
  }

  crearCategoria(categoria: CategoryDTO): Observable<CategoryDTO> {
    return this.http.post<any>(`${this.apiUrl}/categories`, categoria, this.httpOptions).pipe(
      map(res => res.data ? res.data : res)
    );
  }

  actualizarCategoria(id: number, categoria: CategoryDTO): Observable<CategoryDTO> {
    return this.http.put<any>(`${this.apiUrl}/categories/${id}`, categoria, this.httpOptions).pipe(
      map(res => res.data ? res.data : res)
    );
  }

  eliminarCategoria(id: number): Observable<any> {
    return this.http.delete<any>(`${this.apiUrl}/categories/${id}`).pipe(
      map(res => res.data ? res.data : res)
    );
  }

  // Operaciones para Ingredientes
  listarIngredientes(): Observable<IngredientDTO[]> {
    return this.http.get<any>(`${this.apiUrl}/ingredients`).pipe(
      map(res => res.data !== undefined ? res.data : res)
    );
  }

  obtenerIngredientePorId(id: number): Observable<IngredientDTO> {
    return this.http.get<any>(`${this.apiUrl}/ingredients/${id}`).pipe(
      map(res => res.data !== undefined ? res.data : res)
    );
  }

  crearIngrediente(ingrediente: IngredientDTO): Observable<IngredientDTO> {
    return this.http.post<any>(`${this.apiUrl}/ingredients`, ingrediente, this.httpOptions).pipe(
      map(res => res.data !== undefined ? res.data : res)
    );
  }

  actualizarIngrediente(id: number, ingrediente: IngredientDTO): Observable<IngredientDTO> {
    return this.http.put<any>(`${this.apiUrl}/ingredients/${id}`, ingrediente, this.httpOptions).pipe(
      map(res => res.data !== undefined ? res.data : res)
    );
  }

  eliminarIngrediente(id: number): Observable<any> {
    return this.http.delete<any>(`${this.apiUrl}/ingredients/${id}`).pipe(
      map(res => res.data !== undefined ? res.data : res)
    );
  }
}
