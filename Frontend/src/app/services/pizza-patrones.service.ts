import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { map, switchMap } from 'rxjs/operators';
import { PizzaDTO } from '../models/pizza.interface';
import { PizzaService } from './pizza.service';
import { IngredientService } from './ingredient.service';
import { environment } from '../../environments/environment';

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

  listarPizzasDisponibles(): Observable<PizzaDTO[]> {
    return this.pizzaService.listarPizzas().pipe(
      map(pizzas => pizzas.filter(p => p.isAvailable))
    );
  }

  listarPizzasPopulares(): Observable<PizzaDTO[]> {
    return this.pizzaService.listarPizzas().pipe(
      map(pizzas => pizzas.filter(p => p.isPopular))
    );
  }

  listarPizzasPorCategoria(categoryId: number): Observable<PizzaDTO[]> {
    return this.pizzaService.listarPizzas().pipe(
      map(pizzas => pizzas.filter((p: any) => p.categoryId === categoryId || p.category?.id === categoryId))
    );
  }

  listarPizzasDisponiblesYPopulares(): Observable<PizzaDTO[]> {
    return this.pizzaService.listarPizzas().pipe(
      map(pizzas => pizzas.filter(p => p.isAvailable && p.isPopular))
    );
  }

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

  crearPizzaPersonalizada(pizzaData: any): Observable<PizzaDTO> {
    return this.pizzaService.crearPizza(pizzaData);
  }
}

