import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Pizzas } from './pizzas';

describe('Pizzas', () => {
  let component: Pizzas;
  let fixture: ComponentFixture<Pizzas>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideRouter([])],
      imports: [Pizzas]
    })
    .compileComponents();

    fixture = TestBed.createComponent(Pizzas);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('generarDescripcionDesdeReceta', () => {
    it('debe mostrar error si la receta no tiene ingredientes', () => {
      component.recetaFormulario = [];
      component.generarDescripcionDesdeReceta();
      expect(component.error).toContain('Agrega al menos un insumo');
    });

    it('debe generar una descripción comercial limpia y atractiva a partir de los insumos', () => {
      component.recetaFormulario = [
        { ingredientId: 1, ingredientName: 'Salsa de Tomate', quantity: 80, unitCost: 0.02, unit: 'g' },
        { ingredientId: 2, ingredientName: 'Queso Mozzarella', quantity: 150, unitCost: 0.04, unit: 'g' },
        { ingredientId: 3, ingredientName: 'Pepperoni', quantity: 60, unitCost: 0.05, unit: 'g' }
      ];

      component.generarDescripcionDesdeReceta();

      const desc = component.pizzaForm.get('description')?.value;
      expect(desc).toContain('salsa de tomate');
      expect(desc).toContain('queso mozzarella');
      expect(desc).toContain('pepperoni');
      expect(component.success).toContain('Descripción generada');
    });

    it('debe incluir mención a masa fresca artesanal si incluye masa en los ingredientes', () => {
      component.recetaFormulario = [
        { ingredientId: 10, ingredientName: 'Masa Fresca', quantity: 220, unitCost: 0.01, unit: 'g' },
        { ingredientId: 2, ingredientName: 'Queso Mozzarella', quantity: 150, unitCost: 0.04, unit: 'g' }
      ];

      component.generarDescripcionDesdeReceta();

      const desc = component.pizzaForm.get('description')?.value;
      expect(desc).toContain('queso mozzarella');
      expect(desc).toContain('masa fresca tradicional');
    });
  });

  describe('Simulador de Tamaños en Receta', () => {
    beforeEach(() => {
      component.todosIngredientes = [
        { id: 1, name: 'Queso Mozzarella', unit: 'g', unitCost: 0.04, stockQuantity: 3000, minStock: 200, extraCost: 3.5 }
      ];
      component.tamanosDisponibles = [
        { id: 1, name: 'Personal', extraCost: 0, recipeMultiplier: 1.0, stockQuantity: 40 },
        { id: 2, name: 'Familiar', extraCost: 20, recipeMultiplier: 2.0, stockQuantity: 25 }
      ];
      component.pizzaForm.patchValue({ price: 30 });
      component.recetaFormulario = [
        { ingredientId: 1, ingredientName: 'Queso Mozzarella', quantity: 100, unitCost: 0.04, unit: 'g', lineCost: 4.0 }
      ];
      component.costoRecetaForm = 4.0;
    });

    it('debe calcular valores base cuando no hay tamaño simulado seleccionado (Personal 1.0x)', () => {
      component.seleccionarTamanoSimuladorForm(null);
      expect(component.getMultiplicadorForm()).toBe(1.0);
      expect(component.getPrecioSimuladoForm()).toBe(30);
      expect(component.getCostoSimuladoForm()).toBe(4.0);
      expect(component.getStockProducibleSimuladoForm()).toBe(30);
    });

    it('debe escalar insumos, costo y precio al seleccionar un tamaño como Familiar (2.0x)', () => {
      const familiar = component.tamanosDisponibles[1];
      component.seleccionarTamanoSimuladorForm(familiar);

      expect(component.getMultiplicadorForm()).toBe(2.0);
      expect(component.getPrecioSimuladoForm()).toBe(50);
      expect(component.getCostoSimuladoForm()).toBe(8.0);
      expect(component.getCantidadEscalada(100, 2.0)).toBe(200);
      expect(component.getCostoEscalado(4.0, 2.0)).toBe(8.0);
      expect(component.getStockProducibleSimuladoForm()).toBe(15);
    });
  });
});
