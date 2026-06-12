import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { HttpClientModule } from '@angular/common/http';
import { Router } from '@angular/router';
import { CategoriaService } from '../../services/categoria.service';
import { PizzaService } from '../../services/pizza.service';
import { ImageOptimizerService } from '../../services/image-optimizer.service';
import { CategoryDTO, PizzaDTO } from '../../models/pizza.interface';
import { PaginationComponent } from '../pagination/pagination';

@Component({
  selector: 'app-categorias',
  standalone: true,
  imports: [
    CommonModule, 
    FormsModule, 
    ReactiveFormsModule, 
    HttpClientModule,
    PaginationComponent
  ],
  templateUrl: './categorias.html',
  styleUrl: './categorias.css'
})
export class Categorias implements OnInit {
  categorias: CategoryDTO[] = [];
  categoriasPaginadas: CategoryDTO[] = [];
  categoriasEnUso = new Set<number>();
  pizzas: PizzaDTO[] = [];
  categoriaForm: FormGroup;
  editMode = false;
  selectedCategoriaId?: number;
  showForm = false;
  loading = false;
  error = '';
  success = '';

  // Paginación
  currentPage = 1;
  itemsPerPage = 10;

  constructor(
    private categoriaService: CategoriaService,
    private pizzaService: PizzaService,
    private formBuilder: FormBuilder,
    private cdr: ChangeDetectorRef,
    private router: Router,
    private imageOptimizer: ImageOptimizerService
  ) {
    this.categoriaForm = this.formBuilder.group({
      name: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
      description: [''],
      imageUrl: [''],
      displayOrder: [1, [Validators.required, Validators.min(1)]]
    });
  }

  ngOnInit(): void {
    this.cargarCategorias();
    this.cargarPizzasEnUso();
  }

  cargarCategorias(): void {
    this.loading = true;
    this.limpiarMensajes();

    this.categoriaService.listarCategorias().subscribe({
      next: (categorias) => {
        this.categorias = [...categorias];
        this.loading = false;
        this.cdr.detectChanges();
        this.actualizarCategoriasEnUso();
        this.aplicarPaginacion();
      },
      error: (error) => {
        this.mostrarMensajeError(error.error?.message || 'Error al cargar las categorías');
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  aplicarPaginacion(): void {
    const startIndex = (this.currentPage - 1) * this.itemsPerPage;
    const endIndex = startIndex + this.itemsPerPage;
    this.categoriasPaginadas = this.categorias.slice(startIndex, endIndex);
  }

  onPageChange(page: number): void {
    this.currentPage = page;
    this.aplicarPaginacion();
  }

  recargarListaSinLoading(): void {
    this.categoriaService.listarCategorias().subscribe({
      next: (categorias) => {
        this.categorias = [...categorias];
        this.aplicarPaginacion();
        this.loading = false;
        this.cdr.detectChanges();
        setTimeout(() => this.cdr.markForCheck(), 10);
        this.actualizarCategoriasEnUso();
      },
      error: (error) => {
        this.mostrarMensajeError(error.error?.message || 'Error al recargar las categorías');
        this.loading = false;
        this.cdr.detectChanges();
      },
      complete: () => {
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  abrirFormulario(): void {
    this.showForm = true;
    this.editMode = false;
    this.selectedCategoriaId = undefined;
    this.categoriaForm.reset({
      name: '',
      description: '',
      imageUrl: '',
      displayOrder: this.obtenerSiguienteOrden()
    });
    this.limpiarMensajes();
  }

  editarCategoria(categoria: CategoryDTO): void {
    this.showForm = true;
    this.editMode = true;
    this.selectedCategoriaId = categoria.id;
    this.categoriaForm.patchValue({
      name: categoria.name,
      description: categoria.description || '',
      imageUrl: categoria.imageUrl || '',
      displayOrder: categoria.displayOrder ?? 1
    });
    this.limpiarMensajes();
  }

  cancelar(): void {
    this.showForm = false;
    this.editMode = false;
    this.selectedCategoriaId = undefined;
    this.categoriaForm.reset();
    this.limpiarMensajes();
  }

  volverAlPanel(): void {
    this.router.navigate(['/panel-admin']);
  }

  guardarCategoria(): void {
    if (this.categoriaForm.invalid) {
      this.mostrarMensajeError('Por favor, complete los campos requeridos correctamente');
      this.categoriaForm.markAllAsTouched();
      return;
    }

    if (this.verificarNombreDuplicado()) {
      this.mostrarMensajeError('Ya existe una categoría con ese nombre');
      return;
    }

    if (this.verificarOrdenDuplicado()) {
      this.mostrarMensajeError('El orden de visualización ya está en uso');
      return;
    }

    this.loading = true;
    this.limpiarMensajes();

    const formValue = this.categoriaForm.getRawValue();
    const payload: CategoryDTO = {
      name: (formValue.name ?? '').trim(),
      description: formValue.description?.trim() || '',
      imageUrl: formValue.imageUrl?.trim() || '',
      displayOrder: Math.max(1, Number(formValue.displayOrder) || 1)
    };

    if (this.editMode && this.selectedCategoriaId) {
      payload.id = this.selectedCategoriaId;

      this.categoriaService.actualizarCategoria(this.selectedCategoriaId, payload).subscribe({
        next: () => {
          this.mostrarMensajeExito('Categoría actualizada correctamente');
          this.cancelar();
          this.recargarListaSinLoading();
        },
        error: (error) => {
          this.mostrarMensajeError(error.error?.message || 'Error al actualizar la categoría');
          this.loading = false;
        },
        complete: () => {
          this.loading = false;
        }
      });
    } else {
      this.categoriaService.crearCategoria(payload).subscribe({
        next: () => {
          this.mostrarMensajeExito('Categoría creada correctamente');
          this.cancelar();
          this.recargarListaSinLoading();
        },
        error: (error) => {
          this.mostrarMensajeError(error.error?.message || 'Error al crear la categoría');
          this.loading = false;
        },
        complete: () => {
          this.loading = false;
        }
      });
    }
  }

  eliminarCategoria(id: number, nombre: string): void {
    if (!confirm(`¿Está seguro de que desea eliminar la categoría "${nombre}"?`)) {
      return;
    }

    this.loading = true;
    this.limpiarMensajes();

    this.categoriaService.eliminarCategoria(id).subscribe({
      next: () => {
        this.mostrarMensajeExito('Categoría eliminada correctamente');
        this.recargarListaSinLoading();
        this.cargarPizzasEnUso();
      },
      error: (error) => {
        this.mostrarMensajeError(error.error?.message || 'Error al eliminar la categoría');
        this.loading = false;
      },
      complete: () => {
        this.loading = false;
      }
    });
  }

  trackByCategoriaId(_: number, categoria: CategoryDTO): number | undefined {
    return categoria.id;
  }

  verificarNombreDuplicado(): boolean {
    const nombreFormulario = this.categoriaForm.get('name')?.value?.trim().toLowerCase();

    if (!nombreFormulario) {
      return false;
    }

    const categoriasConMismoNombre = this.categorias.filter(categoria => {
      if (this.editMode && categoria.id === this.selectedCategoriaId) {
        return false;
      }

      return categoria.name?.trim().toLowerCase() === nombreFormulario;
    });

    return categoriasConMismoNombre.length > 0;
  }

  verificarOrdenDuplicado(): boolean {
    const ordenFormulario = Number(this.categoriaForm.get('displayOrder')?.value);

    if (!ordenFormulario || ordenFormulario < 1) {
      return false;
    }

    const categoriasConMismoOrden = this.categorias.filter(categoria => {
      if (this.editMode && categoria.id === this.selectedCategoriaId) {
        return false;
      }

      return (categoria.displayOrder ?? 0) === ordenFormulario;
    });

    return categoriasConMismoOrden.length > 0;
  }

  puedeEliminar(categoriaId?: number): boolean {
    if (!categoriaId) {
      return false;
    }
    return !this.categoriasEnUso.has(categoriaId);
  }

  obtenerSiguienteOrden(): number {
    if (!this.categorias.length) {
      return 1;
    }

    const ordenes = this.categorias
      .map(categoria => categoria.displayOrder ?? 0)
      .sort((a, b) => b - a);

    return Math.max(1, (ordenes[0] ?? 0) + 1);
  }

  limpiarMensajes(): void {
    this.error = '';
    this.success = '';
  }

  mostrarMensajeExito(mensaje: string): void {
    this.success = mensaje;
    this.error = '';
    setTimeout(() => {
      this.success = '';
    }, 3000);
  }

  mostrarMensajeError(mensaje: string): void {
    this.error = mensaje;
    this.success = '';
  }

  private cargarPizzasEnUso(): void {
    this.pizzaService.listarPizzas().subscribe({
      next: (pizzas) => {
        this.pizzas = pizzas;
        this.actualizarCategoriasEnUso();
      },
      error: () => {
        this.pizzas = [];
        this.categoriasEnUso.clear();
      }
    });
  }

  private actualizarCategoriasEnUso(): void {
    this.categoriasEnUso.clear();
    // Buscar categorías en uso por nombre
    this.pizzas.forEach(pizza => {
      if (pizza.categoryName) {
        // Encontrar la categoría por nombre para obtener su ID
        const categoria = this.categorias.find(cat => cat.name === pizza.categoryName);
        if (categoria?.id) {
          this.categoriasEnUso.add(categoria.id);
        }
      }
    });
  }

  formatearFecha(fechaString: string): string {
    try {
      const fecha = new Date(fechaString);
      // Verificar si la fecha es válida
      if (isNaN(fecha.getTime())) {
        return 'N/D';
      }
      
      // Formatear la fecha en español
      return fecha.toLocaleDateString('es-ES', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
      });
    } catch (error) {
      return 'N/D';
    }
  }

  /**
   * Obtiene la URL optimizada de una imagen
   */
  getOptimizedImageUrl(imageUrl: string | undefined): string {
    return this.imageOptimizer.optimizeImageUrl(imageUrl, 'low');
  }
}

