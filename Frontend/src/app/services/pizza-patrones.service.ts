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
  private readonly apiUrl= `${environment.apiUrl}/pizzas`;

  constructor(
    private readonly http: HttpClient,
    private readonly pizzaService: PizzaService,
    private readonly ingredientService: IngredientService
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
          map(ingredientes => this.calcularDetallesPrecio(pizza, ingredientes, extras))
        );
      })
    );
  }

  private calcularDetallesPrecio(pizza: PizzaDTO, ingredientes: any[], extras: string[]): any {
    let totalExtras = 0;
    const detalles: any[] = [];
    
    extras.forEach(extraName => {
      const ing = ingredientes.find(i => i.name === extraName || i.id?.toString() === extraName);
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
  }

  /**
   * BUILDER PATTERN - Crear pizza personalizada (Mapeado a endpoint normal)
   */
  crearPizzaPersonalizada(pizzaData: any): Observable<PizzaDTO> {
    return this.pizzaService.crearPizza(pizzaData);
  }
}


