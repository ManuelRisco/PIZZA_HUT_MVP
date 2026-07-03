import { Component, OnInit, ChangeDetectorRef, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { PizzaService } from '../../services/pizza.service';
import { PizzaPatronesService } from '../../services/pizza-patrones.service';
import { IngredientService } from '../../services/ingredient.service';
import { Size } from '../../services/size.service';
import { ImageOptimizerService } from '../../services/image-optimizer.service';
import { PizzaDTO, PizzaCreateDTO, CategoryDTO } from '../../models/pizza.interface';
import { IngredientDTO, SizeDTO } from '../../models/admin.interface';
import { PaginationComponent } from '../pagination/pagination';

@Component({
  selector: 'app-pizzas',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, PaginationComponent],
  templateUrl: './pizzas.html',
  styleUrls: ['./pizzas.css']
})
export class Pizzas implements OnInit {
  pizzas: PizzaDTO[] = [];
  pizzasPaginadas: PizzaDTO[] = [];
  currentPage: number = 1;
  itemsPerPage: number = 10;
  categorias: CategoryDTO[] = [];
  pizzaForm: FormGroup;
  editMode = false;
  selectedPizzaId?: number;
  showForm = false;
  loading = false;
  error = '';
  success = '';
  filtroActivo: 'todas' | 'disponibles' | 'populares' | 'disponibles-populares' = 'todas';
  
  // Variables para Decorator Pattern (extras)
  pizzaSeleccionadaExtras?: PizzaDTO;
  extrasSeleccionados: string[] = [];
  precioConExtras: number = 0;
  descripcionConExtras: string = '';
  mostrarExtras: boolean = false;
  ingredientesDisponibles: IngredientDTO[] = [];
  tamanosDisponibles: SizeDTO[] = [];
  tamanoSeleccionado?: SizeDTO;

  private readonly pizzaService = inject(PizzaService);
  private readonly pizzaPatronesService = inject(PizzaPatronesService);
  private readonly ingredientService = inject(IngredientService);
  private readonly sizeService = inject(Size);
  private readonly formBuilder = inject(FormBuilder);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly router = inject(Router);
  private readonly imageOptimizer = inject(ImageOptimizerService);

  constructor() {
    this.pizzaForm = this.formBuilder.group({
      name: ['', [Validators.required, Validators.maxLength(150)]],
      description: [''],
      imageUrl: ['', Validators.maxLength(500)],
      price: ['', [Validators.required, Validators.min(0.01)]],
      categoryId: ['', Validators.required],
      isAvailable: [true],
      isPopular: [false]
    });
  }

  ngOnInit(): void {
    this.cargarPizzas();
    this.cargarCategorias();
  }

  cargarPizzas(): void {
    this.loading = true;
    
    this.pizzaService.listarPizzas().subscribe({
      next: (pizzas) => {
        this.pizzas = pizzas;
        this.aplicarPaginacion();
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (error) => {
        console.error('Error al cargar pizzas:', error);
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  /**
   * SPECIFICATION PATTERN - Aplicar filtros a las pizzas (Filtrado local)
   */
  aplicarFiltro(filtro: 'todas' | 'disponibles' | 'populares' | 'disponibles-populares'): void {
    this.filtroActivo = filtro;
    this.loading = true;
    this.limpiarMensajes();

    this.pizzaService.listarPizzas().subscribe({
      next: (pizzas) => {
        let pizzasFiltradas: typeof pizzas;
        
        switch(filtro) {
          case 'disponibles':
            pizzasFiltradas = pizzas.filter(p => p.isAvailable);
            break;
          case 'populares':
            pizzasFiltradas = pizzas.filter(p => p.isPopular);
            break;
          case 'disponibles-populares':
            pizzasFiltradas = pizzas.filter(p => p.isAvailable && p.isPopular);
            break;
          case 'todas':
          default:
            pizzasFiltradas = pizzas;
            break;
        }
        
        this.pizzas = pizzasFiltradas;
        this.aplicarPaginacion();
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (error) => {
        console.error('❌ Error al cargar/filtrar pizzas:', error);
        this.mostrarMensajeError('Error al cargar las pizzas');
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  /**
   * Aplica paginación a las pizzas
   */
  aplicarPaginacion(): void {
    const totalPages = Math.ceil(this.pizzas.length / this.itemsPerPage);
    if (this.currentPage > totalPages && totalPages > 0) {
      this.currentPage = totalPages;
    } else if (this.currentPage < 1) {
      this.currentPage = 1;
    }
    const startIndex = (this.currentPage - 1) * this.itemsPerPage;
    const endIndex = startIndex + this.itemsPerPage;
    this.pizzasPaginadas = this.pizzas.slice(startIndex, endIndex);
  }

  /**
   * Cambia de página
   */
  onPageChange(page: number): void {
    this.currentPage = page;
    this.aplicarPaginacion();
  }

  /**
   * Aplicar filtro sin mostrar el spinner de carga
   * Usado después de crear/editar para mantener el filtro activo (Filtrado local)
   */
  private aplicarFiltroSinLoading(): void {
    this.pizzaService.listarPizzas().subscribe({
      next: (pizzas) => {
        let pizzasFiltradas: typeof pizzas;
        
        switch(this.filtroActivo) {
          case 'disponibles':
            pizzasFiltradas = pizzas.filter(p => p.isAvailable);
            break;
          case 'populares':
            pizzasFiltradas = pizzas.filter(p => p.isPopular);
            break;
          case 'disponibles-populares':
            pizzasFiltradas = pizzas.filter(p => p.isAvailable && p.isPopular);
            break;
          case 'todas':
          default:
            pizzasFiltradas = pizzas;
            break;
        }
        
        this.pizzas = pizzasFiltradas;
        this.aplicarPaginacion();
        this.cdr.detectChanges();
      },
      error: (error) => {
        console.error('Error al recargar pizzas:', error);
      }
    });
  }

  /**
   * DECORATOR PATTERN - Mostrar opciones de extras
   */
  mostrarOpcionesExtras(pizza: PizzaDTO): void {
    this.pizzaSeleccionadaExtras = pizza;
    this.extrasSeleccionados = [];
    this.precioConExtras = pizza.price;
    this.descripcionConExtras = pizza.name;
    this.mostrarExtras = true;
    this.tamanoSeleccionado = undefined;
    
    // Cargar ingredientes disponibles si aún no se han cargado
    if (this.ingredientesDisponibles.length === 0) {
      this.cargarIngredientesDisponibles();
    }
    
    // Cargar tamaños disponibles si aún no se han cargado
    if (this.tamanosDisponibles.length === 0) {
      this.cargarTamanosDisponibles();
    }
  }

  /**
   * Cargar ingredientes disponibles desde el backend
   */
  cargarIngredientesDisponibles(): void {
    this.ingredientService.obtenerDisponibles().subscribe({
      next: (ingredientes) => {
        this.ingredientesDisponibles = ingredientes;
      },
      error: (error) => {
        console.error('❌ Error al cargar ingredientes:', error);
        // Si falla, usar los valores por defecto
        this.ingredientesDisponibles = [];
      }
    });
  }

  /**
   * Cargar tamaños disponibles desde el backend
   */
  cargarTamanosDisponibles(): void {
    this.sizeService.obtenerTodos().subscribe({
      next: (tamanos) => {
        this.tamanosDisponibles = tamanos.toSorted((a, b) => (a.displayOrder || 0) - (b.displayOrder || 0));
      },
      error: (error) => {
        console.error('❌ Error al cargar tamaños:', error);
        this.tamanosDisponibles = [];
      }
    });
  }

  /**
   * Seleccionar tamaño
   */
  seleccionarTamano(tamano: SizeDTO): void {
    this.tamanoSeleccionado = tamano;
    this.calcularPrecioConExtras();
  }

  /**
   * DECORATOR PATTERN - Agregar/quitar extra
   */
  toggleExtra(extra: string): void {
    const index = this.extrasSeleccionados.indexOf(extra);
    if (index > -1) {
      this.extrasSeleccionados.splice(index, 1);
    } else {
      this.extrasSeleccionados.push(extra);
    }
    
    if (this.extrasSeleccionados.length > 0 && this.pizzaSeleccionadaExtras) {
      this.calcularPrecioConExtras();
    } else if (this.pizzaSeleccionadaExtras) {
      this.precioConExtras = this.pizzaSeleccionadaExtras.price;
      this.descripcionConExtras = this.pizzaSeleccionadaExtras.name;
    }
  }

  /**
   * DECORATOR PATTERN - Calcular precio total con extras
   */
  calcularPrecioConExtras(): void {
    if (!this.pizzaSeleccionadaExtras?.id) return;
    
    if (this.extrasSeleccionados.length > 0) {
      this.pizzaPatronesService.calcularPrecioConExtras(
        this.pizzaSeleccionadaExtras.id, 
        this.extrasSeleccionados
      ).subscribe({
        next: (response) => {
          let precioTotal = response.precioFinal;
          let descripcion = response.descripcion;
          
          // Agregar el costo del tamaño si hay uno seleccionado
          if (this.tamanoSeleccionado) {
            precioTotal += this.tamanoSeleccionado.extraCost;
            descripcion += ` - Tamaño: ${this.tamanoSeleccionado.name}`;
          }
          
          this.precioConExtras = precioTotal;
          this.descripcionConExtras = descripcion;
        },
        error: (error) => {
          console.error('❌ Error al calcular extras:', error);
        }
      });
    } else {
      // Solo precio base + tamaño
      let precioTotal = this.pizzaSeleccionadaExtras.price;
      let descripcion = this.pizzaSeleccionadaExtras.name;
      
      if (this.tamanoSeleccionado) {
        precioTotal += this.tamanoSeleccionado.extraCost;
        descripcion += ` - Tamaño: ${this.tamanoSeleccionado.name}`;
      }
      
      this.precioConExtras = precioTotal;
      this.descripcionConExtras = descripcion;
    }
  }

  cerrarExtras(): void {
    this.mostrarExtras = false;
    this.pizzaSeleccionadaExtras = undefined;
    this.extrasSeleccionados = [];
  }

  cargarCategorias(): void {
    this.pizzaService.listarCategorias().subscribe({
      next: (categorias) => {
        this.categorias = categorias;
      },
      error: (error) => {
        console.error('Error al cargar categorías:', error);
      }
    });
  }

  abrirFormulario(): void {
    this.showForm = true;
    this.editMode = false;
    this.pizzaForm.reset();
    this.pizzaForm.patchValue({ isAvailable: true, isPopular: false });
    this.selectedPizzaId = undefined;
    this.limpiarMensajes();
  }

  editarPizza(pizza: PizzaDTO): void {
    this.showForm = true;
    this.editMode = true;
    this.selectedPizzaId = pizza.id;
    
    // Buscar el categoryId basado en el categoryName
    const categoria = this.categorias.find(cat => cat.name === pizza.categoryName);
    
    this.pizzaForm.patchValue({
      name: pizza.name,
      description: pizza.description,
      imageUrl: pizza.imageUrl,
      price: pizza.price,
      categoryId: categoria?.id || '',
      isAvailable: pizza.isAvailable,
      isPopular: pizza.isPopular
    });
    this.limpiarMensajes();
  }

  cancelar(): void {
    this.showForm = false;
    this.editMode = false;
    this.selectedPizzaId = undefined;
    this.pizzaForm.reset();
    this.limpiarMensajes();
  }

  volverAlPanel(): void {
    this.router.navigate(['/panel-admin']);
  }

  guardarPizza(): void {
    if (this.pizzaForm.valid) {
      this.loading = true;
      const formValue = this.pizzaForm.value;
      
      // Crear el objeto pizza con PizzaCreateDTO
      const pizzaData: PizzaCreateDTO = {
        name: formValue.name,
        description: formValue.description,
        imageUrl: formValue.imageUrl,
        price: Number.parseFloat(formValue.price),
        isAvailable: formValue.isAvailable,
        isPopular: formValue.isPopular,
        categoryId: Number.parseInt(formValue.categoryId)
      };

      if (this.editMode && this.selectedPizzaId) {
        // Actualizar pizza existente
        this.pizzaService.actualizarPizza(this.selectedPizzaId, pizzaData).subscribe({
          next: (pizzaActualizada) => {
            this.mostrarMensajeExito('Pizza actualizada correctamente');
            // Actualizar la pizza específica en la lista sin recargar todo
            this.actualizarPizzaEnLista(pizzaActualizada);
            this.cancelar();
            this.loading = false;
          },
          error: (error) => {
            console.error('❌ Error al actualizar pizza:', error);
            this.mostrarMensajeError(error.error?.message || 'Error al actualizar la pizza');
            this.loading = false;
          }
        });
      } else {
        // Crear nueva pizza
        this.pizzaService.crearPizza(pizzaData).subscribe({
          next: (nuevaPizza) => {
            this.mostrarMensajeExito('Pizza creada correctamente');
            // Agregar la nueva pizza al principio de la lista
            this.agregarPizzaALista(nuevaPizza);
            this.cancelar();
            this.loading = false;
          },
          error: (error) => {
            console.error('❌ Error al crear pizza:', error);
            this.mostrarMensajeError(error.error?.message || 'Error al crear la pizza');
            this.loading = false;
          }
        });
      }
    } else {
      this.mostrarMensajeError('Por favor, complete todos los campos requeridos correctamente');
    }
  }

  eliminarPizza(id: number, nombre: string): void {
    if (confirm(`¿Está seguro de que desea eliminar la pizza "${nombre}"?`)) {
      this.loading = true;
      
      this.pizzaService.eliminarPizza(id).subscribe({
        next: (response) => {

          this.mostrarMensajeExito('Pizza eliminada correctamente');

          // Remover la pizza de la lista local inmediatamente
          this.removerPizzaDeLista(id);
          this.loading = false;
          
          // Forzar detección de cambios
          this.cdr.detectChanges();
        },
        error: (error) => {
          console.error('❌ Error al eliminar pizza:', error);
          this.mostrarMensajeError(error.error?.message || 'Error al eliminar la pizza');
          this.loading = false;
        }
      });
    }
  }

  limpiarMensajes(): void {
    this.error = '';
    this.success = '';
  }

  private mostrarMensajeExito(mensaje: string): void {
    this.success = mensaje;
    this.limpiarError();
    // Limpiar el mensaje de éxito después de 3 segundos
    setTimeout(() => {
      this.success = '';
    }, 3000);
  }

  private mostrarMensajeError(mensaje: string): void {
    this.error = mensaje;
    this.success = '';
  }

  private limpiarError(): void {
    this.error = '';
  }

  obtenerNombreCategoria(pizza: PizzaDTO): string {
    return pizza.categoryName || 'Sin categoría';
  }

  formatearPrecio(precio: number | undefined | null): string {
    if (precio === undefined || precio === null || Number.isNaN(precio)) {
      return 'S/ 0.00';
    }
    return `S/ ${Number(precio).toFixed(2)}`;
  }

  trackByPizzaId(index: number, pizza: PizzaDTO): number {
    return pizza.id || index;
  }

  onImageError(pizza: PizzaDTO): void {
    pizza.imageError = true;
  }

  /**
   * Obtiene la URL optimizada de una imagen
   */
  getOptimizedImageUrl(imageUrl: string | undefined): string {
    return this.imageOptimizer.optimizeImageUrl(imageUrl, 'low');
  }

  private actualizarPizzaEnLista(pizzaActualizada: PizzaDTO): void {
    // En lugar de modificar la lista local, volver a aplicar el filtro activo
    this.aplicarFiltroSinLoading();
  }

  private agregarPizzaALista(nuevaPizza: PizzaDTO): void {
    // En lugar de agregar a la lista local, volver a aplicar el filtro activo
    this.aplicarFiltroSinLoading();
  }

  private removerPizzaDeLista(pizzaId: number): void {
    // En lugar de modificar la lista local, volver a aplicar el filtro activo
    this.aplicarFiltroSinLoading();
  }
}


