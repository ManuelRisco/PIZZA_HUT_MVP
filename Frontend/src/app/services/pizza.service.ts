import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { PizzaDTO, PizzaCreateDTO, CategoryDTO, IngredientDTO } from '../models/pizza.interface';

@Injectable({
  providedIn: 'root'
})
export class PizzaService {
  private apiUrl = 'http://localhost:8089/api';
  private httpOptions = {
    headers: new HttpHeaders({ 'Content-Type': 'application/json' })
  };

  constructor(private http: HttpClient) { }

  // Operaciones CRUD para Pizzas
  listarPizzas(): Observable<PizzaDTO[]> {
    return this.http.get<PizzaDTO[]>(`${this.apiUrl}/pizzas`);
  }

  obtenerPizzaPorId(id: number): Observable<PizzaDTO> {
    return this.http.get<PizzaDTO>(`${this.apiUrl}/pizzas/${id}`);
  }

  crearPizza(pizza: PizzaCreateDTO): Observable<PizzaDTO> {
    return this.http.post<PizzaDTO>(`${this.apiUrl}/pizzas`, pizza, this.httpOptions);
  }

  actualizarPizza(id: number, pizza: PizzaCreateDTO): Observable<PizzaDTO> {
    return this.http.put<PizzaDTO>(`${this.apiUrl}/pizzas/${id}`, pizza, this.httpOptions);
  }

  eliminarPizza(id: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/pizzas/${id}`);
  }

  // Operaciones para Categorías
  listarCategorias(): Observable<CategoryDTO[]> {
    return this.http.get<CategoryDTO[]>(`${this.apiUrl}/categories`);
  }

  obtenerCategoriaPorId(id: number): Observable<CategoryDTO> {
    return this.http.get<CategoryDTO>(`${this.apiUrl}/categories/${id}`);
  }

  crearCategoria(categoria: CategoryDTO): Observable<CategoryDTO> {
    return this.http.post<CategoryDTO>(`${this.apiUrl}/categories`, categoria, this.httpOptions);
  }

  actualizarCategoria(id: number, categoria: CategoryDTO): Observable<CategoryDTO> {
    return this.http.put<CategoryDTO>(`${this.apiUrl}/categories/${id}`, categoria, this.httpOptions);
  }

  eliminarCategoria(id: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/categories/${id}`);
  }

  // Operaciones para Ingredientes
  listarIngredientes(): Observable<IngredientDTO[]> {
    return this.http.get<IngredientDTO[]>(`${this.apiUrl}/ingredients`);
  }

  obtenerIngredientePorId(id: number): Observable<IngredientDTO> {
    return this.http.get<IngredientDTO>(`${this.apiUrl}/ingredients/${id}`);
  }

  crearIngrediente(ingrediente: IngredientDTO): Observable<IngredientDTO> {
    return this.http.post<IngredientDTO>(`${this.apiUrl}/ingredients`, ingrediente, this.httpOptions);
  }

  actualizarIngrediente(id: number, ingrediente: IngredientDTO): Observable<IngredientDTO> {
    return this.http.put<IngredientDTO>(`${this.apiUrl}/ingredients/${id}`, ingrediente, this.httpOptions);
  }

  eliminarIngrediente(id: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/ingredients/${id}`);
  }
}