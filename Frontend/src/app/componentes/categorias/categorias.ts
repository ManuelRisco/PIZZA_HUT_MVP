import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { CategoriaService } from '../../services/categoria.service';
import { PizzaService } from '../../services/pizza.service';
import { CategoryDTO, PizzaDTO } from '../../models/pizza.interface';
import { PaginationComponent } from '../pagination/pagination';

@Component({
  selector: 'app-categorias',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    PaginationComponent
  ],
  templateUrl: './categorias.html',
  styleUrl: './categorias.css'
})
export class Categorias implements OnInit {
  categorias: CategoryDTO[] = [];
  categoriasFiltradas: CategoryDTO[] = [];
  categoriasPaginadas: CategoryDTO[] = [];
  categoriasEnUsoIds = new Set<number>();
  categoriasEnUso = this.categoriasEnUsoIds;
  pizzasPorCategoriaMap = new Map<number, number>();
  fechasFormateadasMap = new Map<number, string>();
  pizzas: PizzaDTO[] = [];

  categoriaForm: FormGroup;
  editMode = false;
  selectedCategoriaId?: number;
  showForm = false;
  loading = false;
  loadingPizzas = false;
  guardando = false;
  loadError = '';
  error = '';
  success = '';

  // Filtros y búsqueda
  terminoBusqueda = '';
  filtroUso: 'TODAS' | 'EN_USO' | 'SIN_USO' = 'TODAS';

  // KPIs precalculados
  kpiTotalCategorias = 0;
  kpiCategoriasEnUso = 0;
  kpiCategoriasSinPizzas = 0;

  // Paginación (múltiplo de 1, 2 y 3 columnas)
  currentPage = 1;
  itemsPerPage = 12;
  pageSizeOptions: number[] = [12, 24, 48];

  constructor(
    private readonly categoriaService: CategoriaService,
    private readonly pizzaService: PizzaService,
    private readonly formBuilder: FormBuilder,
    private readonly cdr: ChangeDetectorRef,
    private readonly router: Router
  ) {
    this.categoriaForm = this.formBuilder.group({
      name: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
      description: [''],
      displayOrder: [1, [Validators.required, Validators.min(1)]]
    });
  }

  ngOnInit(): void {
    this.cargarCategorias();
    this.cargarPizzasEnUso();
  }

  recargar(): void {
    this.cargarCategorias();
    this.cargarPizzasEnUso();
  }

  reintentar(): void {
    this.recargar();
  }

  abrirNuevo(): void {
    this.abrirFormulario();
  }

  cargarCategorias(): void {
    this.loading = true;
    this.loadError = '';
    this.limpiarMensajes();

    this.categoriaService.listarCategorias(true).subscribe({
      next: (categorias) => {
        this.categorias = [...(categorias || [])].toSorted(
          (a, b) => (a.displayOrder ?? 0) - (b.displayOrder ?? 0)
        );
        this.precomputarFechas();
        this.actualizarCategoriasEnUso();
        this.aplicarFiltro();
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (error) => {
        this.loadError = this.obtenerMensajeErrorCarga(
          error,
          'Error al cargar las categorías. Verifique la conexión e intente nuevamente.'
        );
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  private precomputarFechas(): void {
    this.fechasFormateadasMap.clear();
    for (const cat of this.categorias) {
      if (cat.id && cat.updatedAt) {
        this.fechasFormateadasMap.set(cat.id, this.formatearFecha(cat.updatedAt));
      }
    }
  }

  private recalcularKpis(): void {
    this.kpiTotalCategorias = this.categorias.length;
    let enUsoCount = 0;
    for (const cat of this.categorias) {
      if (cat.id && this.categoriasEnUsoIds.has(cat.id)) {
        enUsoCount++;
      }
    }
    this.kpiCategoriasEnUso = enUsoCount;
    this.kpiCategoriasSinPizzas = Math.max(0, this.kpiTotalCategorias - enUsoCount);
  }

  onBusquedaChange(): void {
    this.currentPage = 1;
    this.aplicarFiltro();
  }

  cambiarFiltroUso(filtro: 'TODAS' | 'EN_USO' | 'SIN_USO'): void {
    this.filtroUso = filtro;
    this.currentPage = 1;
    this.aplicarFiltro();
  }

  limpiarFiltros(): void {
    this.terminoBusqueda = '';
    this.filtroUso = 'TODAS';
    this.currentPage = 1;
    this.aplicarFiltro();
  }

  aplicarFiltro(): void {
    let lista = [...this.categorias];

    if (this.filtroUso === 'EN_USO') {
      lista = lista.filter(c => !!c.id && this.categoriasEnUsoIds.has(c.id));
    } else if (this.filtroUso === 'SIN_USO') {
      lista = lista.filter(c => !c.id || !this.categoriasEnUsoIds.has(c.id));
    }

    const q = this.terminoBusqueda.trim().toLowerCase();
    if (q) {
      lista = lista.filter(
        c =>
          c.name?.toLowerCase().includes(q) ||
          c.description?.toLowerCase().includes(q) ||
          String(c.id ?? '').includes(q)
      );
    }

    this.categoriasFiltradas = lista;
    this.aplicarPaginacion();
  }

  aplicarPaginacion(): void {
    const totalPages = Math.ceil(this.categoriasFiltradas.length / this.itemsPerPage);
    if (this.currentPage > totalPages && totalPages > 0) {
      this.currentPage = totalPages;
    } else if (this.currentPage < 1) {
      this.currentPage = 1;
    }
    const startIndex = (this.currentPage - 1) * this.itemsPerPage;
    const endIndex = startIndex + this.itemsPerPage;
    this.categoriasPaginadas = this.categoriasFiltradas.slice(startIndex, endIndex);
  }

  onPageChange(page: number): void {
    this.currentPage = page;
    this.aplicarPaginacion();
  }

  onItemsPerPageChange(size: number): void {
    this.itemsPerPage = size;
    this.currentPage = 1;
    this.aplicarPaginacion();
  }

  recargarListaSinLoading(): void {
    this.categoriaService.listarCategorias(true).subscribe({
      next: (categorias) => {
        this.categorias = [...(categorias || [])].toSorted(
          (a, b) => (a.displayOrder ?? 0) - (b.displayOrder ?? 0)
        );
        this.loadError = '';
        this.precomputarFechas();
        this.actualizarCategoriasEnUso();
        this.aplicarFiltro();
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (error) => {
        this.mostrarMensajeError(error.error?.message || 'Error al recargar las categorías');
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
    if (this.guardando) {
      return;
    }

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

    this.guardando = true;
    this.limpiarMensajes();

    const formValue = this.categoriaForm.getRawValue();
    const payload: CategoryDTO = {
      name: (formValue.name ?? '').trim(),
      description: formValue.description?.trim() || '',
      displayOrder: Math.max(1, Number(formValue.displayOrder) || 1)
    };

    if (this.editMode && this.selectedCategoriaId) {
      payload.id = this.selectedCategoriaId;

      this.categoriaService.actualizarCategoria(this.selectedCategoriaId, payload).subscribe({
        next: () => {
          this.guardando = false;
          this.cancelar();
          this.mostrarMensajeExito('Categoría actualizada correctamente');
          this.recargarListaSinLoading();
        },
        error: (error) => {
          this.mostrarMensajeError(error.error?.message || 'Error al actualizar la categoría');
          this.guardando = false;
        }
      });
    } else {
      this.categoriaService.crearCategoria(payload).subscribe({
        next: () => {
          this.guardando = false;
          this.cancelar();
          this.mostrarMensajeExito('Categoría creada correctamente');
          this.recargarListaSinLoading();
        },
        error: (error) => {
          this.mostrarMensajeError(error.error?.message || 'Error al crear la categoría');
          this.guardando = false;
        }
      });
    }
  }


  restaurarCategoria(id: number, nombre: string): void {
    if (!confirm(`¿Desea restaurar la categoría "${nombre}"?`)) {
      return;
    }

    this.limpiarMensajes();

    this.categoriaService.restaurarCategoria(id).subscribe({
      next: () => {
        this.mostrarMensajeExito(`Categoría "${nombre}" restaurada con éxito`);
        this.recargarListaSinLoading();
        this.cargarPizzasEnUso();
      },
      error: (error) => {
        this.mostrarMensajeError(error.error?.message || 'Error al restaurar la categoría');
      }
    });
  }

  eliminarCategoria(id: number, nombre: string): void {
    if (!confirm(`¿Está seguro de que desea eliminar la categoría "${nombre}"?`)) {
      return;
    }

    this.limpiarMensajes();

    this.categoriaService.eliminarCategoria(id).subscribe({
      next: () => {
        this.mostrarMensajeExito('Categoría eliminada correctamente');
        this.recargarListaSinLoading();
        this.cargarPizzasEnUso();
      },
      error: (error) => {
        this.mostrarMensajeError(error.error?.message || 'Error al eliminar la categoría');
      }
    });
  }

  trackByCategoriaId(index: number, categoria: CategoryDTO): number {
    return categoria.id ?? index;
  }

  verificarNombreDuplicado(): boolean {
    const nombreFormulario = this.categoriaForm.get('name')?.value?.trim().toLowerCase();

    if (!nombreFormulario) {
      return false;
    }

    return this.categorias.some(categoria => {
      if (this.editMode && categoria.id === this.selectedCategoriaId) {
        return false;
      }
      return categoria.name?.trim().toLowerCase() === nombreFormulario;
    });
  }

  verificarOrdenDuplicado(): boolean {
    const ordenFormulario = Number(this.categoriaForm.get('displayOrder')?.value);

    if (!ordenFormulario || ordenFormulario < 1) {
      return false;
    }

    return this.categorias.some(categoria => {
      if (this.editMode && categoria.id === this.selectedCategoriaId) {
        return false;
      }
      return (categoria.displayOrder ?? 0) === ordenFormulario;
    });
  }

  categoriaEnUso(categoriaId?: number): boolean {
    if (!categoriaId) {
      return false;
    }
    return this.categoriasEnUsoIds.has(categoriaId);
  }

  puedeEliminar(categoriaId?: number): boolean {
    if (!categoriaId) {
      return false;
    }
    return !this.categoriasEnUsoIds.has(categoriaId);
  }

  obtenerSiguienteOrden(): number {
    if (!this.categorias.length) {
      return 1;
    }

    const ordenes = this.categorias
      .map(categoria => categoria.displayOrder ?? 0)
      .toSorted((a, b) => b - a);

    return Math.max(1, (ordenes[0] ?? 0) + 1);
  }

  limpiarMensajes(): void {
    this.error = '';
    this.success = '';
  }

  private obtenerMensajeErrorCarga(err: any, fallback: string): string {
    if (err?.status === 0) {
      return 'No se pudo conectar con el servidor (Backend sin conexión). Verifique que el servicio esté activo e intente nuevamente.';
    }
    return err?.error?.message || fallback;
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
    this.loadingPizzas = true;
    this.pizzaService.listarPizzas().subscribe({
      next: (pizzas) => {
        this.pizzas = pizzas || [];
        this.actualizarCategoriasEnUso();
        this.aplicarFiltro();
        this.loadingPizzas = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.pizzas = [];
        this.categoriasEnUsoIds.clear();
        this.pizzasPorCategoriaMap.clear();
        this.recalcularKpis();
        this.loadingPizzas = false;
        this.cdr.detectChanges();
      }
    });
  }

  private actualizarCategoriasEnUso(): void {
    this.categoriasEnUsoIds.clear();
    this.pizzasPorCategoriaMap.clear();

    const nombreAId = new Map<string, number>();
    for (const cat of this.categorias) {
      if (cat.id && cat.name) {
        nombreAId.set(cat.name.trim().toLowerCase(), cat.id);
      }
    }

    for (const pizza of this.pizzas) {
      const catId =
        pizza.categoryId ||
        (pizza.categoryName ? nombreAId.get(pizza.categoryName.trim().toLowerCase()) : undefined);
      if (catId) {
        this.categoriasEnUsoIds.add(catId);
        this.pizzasPorCategoriaMap.set(catId, (this.pizzasPorCategoriaMap.get(catId) || 0) + 1);
      }
    }

    this.recalcularKpis();
  }

  formatearFecha(fechaString: string): string {
    try {
      const fecha = new Date(fechaString);
      // Verificar si la fecha es válida
      if (Number.isNaN(fecha.getTime())) {
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
    } catch {
      return 'N/D';
    }
  }
}
