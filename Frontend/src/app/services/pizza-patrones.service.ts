import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { map, switchMap } from 'rxjs/operators';
import { PizzaDTO } from '../models/pizza.interface';
import { PizzaService } from './pizza.service';
import { IngredientService } from './ingredient.service';
import { environment } from '../../environments/environment';

/**
 * Servicio que simulaba los patrones de diseño del backend para pizzas.
 * Ahora utiliza los endpoints estándar y filtra en el cliente para mantener la interfaz.
 */
@Injectable({
  providedIn: 'root'
})
export class PizzaPatronesService {
  private apiUrl = `${environment.apiUrl}/pizzas`;

  constructor(
    private http: HttpClient,
    private pizzaService: PizzaService,
    private ingredientService: IngredientService
  ) { }

  /**
   * SPECIFICATION PATTERN - Filtrar pizzas disponibles (Simulado)
   */
  listarPizzasDisponibles(): Observable<PizzaDTO[]> {
    return this.pizzaService.listarPizzas().pipe(
      map(pizzas => pizzas.filter(p => p.isAvailable))
    );
  }

  /**
   * SPECIFICATION PATTERN - Filtrar pizzas populares (Simulado)
   */
  listarPizzasPopulares(): Observable<PizzaDTO[]> {
    return this.pizzaService.listarPizzas().pipe(
      map(pizzas => pizzas.filter(p => p.isPopular))
    );
  }

  /**
   * SPECIFICATION PATTERN - Filtrar pizzas por categoría (Simulado)
   */
  listarPizzasPorCategoria(categoryId: number): Observable<PizzaDTO[]> {
    return this.pizzaService.listarPizzas().pipe(
      map(pizzas => pizzas.filter((p: any) => p.categoryId === categoryId || p.category?.id === categoryId))
    );
  }

  /**
   * COMPOSITE PATTERN - Filtrar pizzas disponibles Y populares (Simulado)
   */
  listarPizzasDisponiblesYPopulares(): Observable<PizzaDTO[]> {
    return this.pizzaService.listarPizzas().pipe(
      map(pizzas => pizzas.filter(p => p.isAvailable && p.isPopular))
    );
  }

  /**
   * DECORATOR PATTERN - Calcular precio con extras (Simulado localmente)
   */
  calcularPrecioConExtras(pizzaId: number, extras: string[]): Observable<any> {
    return this.pizzaService.obtenerPizzaPorId(pizzaId).pipe(
      switchMap((pizza: PizzaDTO) => {
        return this.ingredientService.obtenerDisponibles().pipe(
          map(ingredientes => {
            let totalExtras = 0;
            let detalles: any[] = [];
            extras.forEach(extraName => {
              // Buscar ingrediente por nombre, ya que extras a veces es un array de nombres o IDs
              // Aquí intentamos manejar ambos casos para mayor seguridad
              let ing = ingredientes.find(i => i.name === extraName || i.id?.toString() === extraName);
              if (ing) {
                totalExtras += ing.extraCost || 0;
                detalles.push({ nombre: ing.name, costo: ing.extraCost });
              }
            });
            return {
              precioBase: pizza.price,
              costoExtras: totalExtras,
              precioTotal: pizza.price + totalExtras,
              extrasAplicados: detalles,
              descripcion: `Pizza ${pizza.name} con ${detalles.length} extras`
            };
          })
        );
      })
    );
  }

  /**
   * BUILDER PATTERN - Crear pizza personalizada (Mapeado a endpoint normal)
   */
  crearPizzaPersonalizada(pizzaData: any): Observable<PizzaDTO> {
    return this.pizzaService.crearPizza(pizzaData);
  }
}

