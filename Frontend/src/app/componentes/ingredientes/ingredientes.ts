import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { IngredientService } from '../../services/ingredient.service';
import { IngredientDTO } from '../../models/admin.interface';
import { PaginationComponent } from '../pagination/pagination';

@Component({
  selector: 'app-ingredientes',
  standalone: true,
  imports: [CommonModule, FormsModule, PaginationComponent],
  templateUrl: './ingredientes.html',
  styleUrls: ['./ingredientes.css']
})
export class Ingredientes implements OnInit {
  ingredientes: IngredientDTO[] = [];
  ingredientesFiltrados: IngredientDTO[] = [];
  ingredientesPaginados: IngredientDTO[] = [];
  valorStockMap = new Map<number, number>();
  ingredienteSeleccionado: IngredientDTO = this.nuevoIngrediente();
  modoEdicion: boolean = false;
  mostrarModal: boolean = false;
  filtroDisponibilidad: string = 'TODOS';
  terminoBusqueda: string = '';
  loadError: string = '';
  mensaje: string = '';
  esError: boolean = false;
  loading: boolean = true;
  guardando: boolean = false;

  // KPIs precalculados
  kpiTotalIngredientes = 0;
  kpiDisponibles = 0;
  kpiAlertasStock = 0;
  kpiValorAlmacen = 0;

  unidades = ['g', 'ml', 'und', 'kg', 'L'];

  // Paginación
  currentPage = 1;
  itemsPerPage = 10;
  pageSizeOptions: number[] = [10, 25, 50];

  constructor(private readonly ingredientService: IngredientService) {}

  ngOnInit(): void {
    this.cargarIngredientes();
  }

  recargar(): void {
    this.cargarIngredientes();
  }

  reintentar(): void {
    this.cargarIngredientes();
  }

  abrirNuevo(): void {
    this.abrirModal();
  }

  cargarIngredientes(): void {
    this.loading = true;
    this.loadError = '';
    this.ingredientService.obtenerTodos(true).subscribe({
      next: data => {
        this.ingredientes = data || [];
        this.recalcularKpis();
        this.aplicarFiltro();
        this.loading = false;
      },
      error: error => {
        console.error('Error al cargar ingredientes:', error);
        this.loadError = this.obtenerMensajeErrorCarga(
          error,
          'No se pudieron cargar los ingredientes e insumos del almacén.'
        );
        this.loading = false;
      }
    });
  }

  private recargarSinSpinner(): void {
    this.ingredientService.obtenerTodos(true).subscribe({
      next: data => {
        this.ingredientes = data || [];
        this.loadError = '';
        this.recalcularKpis();
        this.aplicarFiltro();
      },
      error: error => {
        console.error('Error al recargar ingredientes:', error);
      }
    });
  }

  private recalcularKpis(): void {
    let disponibles = 0;
    let alertas = 0;
    let valorTotal = 0;
    this.valorStockMap.clear();

    for (const ing of this.ingredientes) {
      if (ing.isAvailable && !ing.outOfStock) {
        disponibles++;
      }
      if (ing.lowStock || ing.outOfStock) {
        alertas++;
      }
      const valorLinea = (ing.stockQuantity || 0) * (ing.unitCost || 0);
      valorTotal += valorLinea;
      if (ing.id !== undefined) {
        this.valorStockMap.set(ing.id, valorLinea);
      }
    }

    this.kpiTotalIngredientes = this.ingredientes.length;
    this.kpiDisponibles = disponibles;
    this.kpiAlertasStock = alertas;
    this.kpiValorAlmacen = valorTotal;
  }

  cambiarFiltroEstado(estado: string): void {
    this.filtroDisponibilidad = estado;
    this.currentPage = 1;
    this.aplicarFiltro();
  }

  limpiarFiltros(): void {
    this.terminoBusqueda = '';
    this.filtroDisponibilidad = 'TODOS';
    this.currentPage = 1;
    this.aplicarFiltro();
  }

  aplicarFiltro(): void {
    let lista = [...this.ingredientes];

    if (this.filtroDisponibilidad === 'DISPONIBLES') {
      lista = lista.filter(i => i.isAvailable && !i.outOfStock);
    } else if (this.filtroDisponibilidad === 'STOCK_BAJO') {
      lista = lista.filter(i => i.lowStock || i.outOfStock);
    } else if (this.filtroDisponibilidad === 'NO_DISPONIBLES') {
      lista = lista.filter(i => !i.isAvailable || i.outOfStock);
    }

    if (this.terminoBusqueda.trim()) {
      const q = this.terminoBusqueda.toLowerCase().trim();
      lista = lista.filter(
        i =>
          i.name?.toLowerCase().includes(q) ||
          (i.unit || '').toLowerCase().includes(q) ||
          String(i.id ?? '').includes(q)
      );
    }

    this.ingredientesFiltrados = lista;
    this.aplicarPaginacion();
  }

  aplicarPaginacion(): void {
    const totalPages = Math.ceil(this.ingredientesFiltrados.length / this.itemsPerPage);
    if (this.currentPage > totalPages && totalPages > 0) {
      this.currentPage = totalPages;
    } else if (this.currentPage < 1) {
      this.currentPage = 1;
    }
    const startIndex = (this.currentPage - 1) * this.itemsPerPage;
    const endIndex = startIndex + this.itemsPerPage;
    this.ingredientesPaginados = this.ingredientesFiltrados.slice(startIndex, endIndex);
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

  abrirModal(ingrediente?: IngredientDTO): void {
    this.mensaje = '';
    if (ingrediente) {
      this.modoEdicion = true;
      this.ingredienteSeleccionado = {
        ...ingrediente,
        unit: ingrediente.unit || 'g',
        stockQuantity: ingrediente.stockQuantity ?? 0,
        minStock: ingrediente.minStock ?? 500,
        unitCost: ingrediente.unitCost ?? 0.02,
        extraPortionQuantity: ingrediente.extraPortionQuantity ?? 50,
        allowAsExtra: ingrediente.allowAsExtra !== false
      };
    } else {
      this.modoEdicion = false;
      this.ingredienteSeleccionado = this.nuevoIngrediente();
    }
    this.mostrarModal = true;
  }

  cerrarModal(): void {
    this.mostrarModal = false;
    this.ingredienteSeleccionado = this.nuevoIngrediente();
  }

  guardar(): void {
    if (this.guardando) {
      return;
    }
    if (!this.validarIngrediente()) {
      return;
    }

    this.guardando = true;

    if (this.modoEdicion && this.ingredienteSeleccionado.id) {
      this.ingredientService.actualizar(this.ingredienteSeleccionado.id, this.ingredienteSeleccionado).subscribe({
        next: () => {
          this.guardando = false;
          this.cerrarModal();
          this.mostrarAlerta('Ingrediente e inventario actualizados exitosamente');
          this.recargarSinSpinner();
        },
        error: error => {
          console.error('Error al actualizar ingrediente:', error);
          this.guardando = false;
          this.mostrarAlerta(error.error?.message || 'Error al actualizar el ingrediente', true);
        }
      });
    } else {
      this.ingredientService.crear(this.ingredienteSeleccionado).subscribe({
        next: () => {
          this.guardando = false;
          this.cerrarModal();
          this.mostrarAlerta('Ingrediente creado exitosamente');
          this.recargarSinSpinner();
        },
        error: error => {
          console.error('Error al crear ingrediente:', error);
          this.guardando = false;
          this.mostrarAlerta(error.error?.message || 'Error al crear el ingrediente', true);
        }
      });
    }
  }


  restaurar(id: number, nombre?: string): void {
    if (confirm('¿Desea restaurar el ingrediente' + (nombre ? ` "${nombre}"` : '') + '?')) {
      this.ingredientService.restaurar(id).subscribe({
        next: () => {
          this.mostrarAlerta('Ingrediente restaurado exitosamente');
          this.recargarSinSpinner();
        },
        error: (error) => {
          console.error('Error al restaurar ingrediente:', error);
          this.mostrarAlerta(error.error?.message || 'Error al restaurar el ingrediente', true);
        }
      });
    }
  }

  eliminar(id: number): void {
    if (confirm('¿Está seguro de eliminar este ingrediente?')) {
      this.ingredientService.eliminar(id).subscribe({
        next: () => {
          this.mostrarAlerta('Ingrediente eliminado exitosamente');
          this.recargarSinSpinner();
        },
        error: error => {
          console.error('Error al eliminar ingrediente:', error);
          this.mostrarAlerta(error.error?.message || 'Error al eliminar el ingrediente', true);
        }
      });
    }
  }

  cambiarDisponibilidad(ingrediente: IngredientDTO): void {
    if (!ingrediente.id) return;

    const nuevoEstado = !ingrediente.isAvailable;
    this.ingredientService.cambiarDisponibilidad(ingrediente.id, nuevoEstado).subscribe({
      next: () => {
        ingrediente.isAvailable = nuevoEstado;
        this.recalcularKpis();
        this.aplicarFiltro();
      },
      error: error => {
        console.error('Error al cambiar disponibilidad:', error);
        this.mostrarAlerta('Error al cambiar la disponibilidad', true);
      }
    });
  }

  validarIngrediente(): boolean {
    if (!this.ingredienteSeleccionado.name?.trim()) {
      this.mostrarAlerta('El nombre es obligatorio', true);
      return false;
    }
    if ((this.ingredienteSeleccionado.extraCost ?? 0) < 0 || (this.ingredienteSeleccionado.unitCost ?? 0) < 0) {
      this.mostrarAlerta('Los costos no pueden ser negativos', true);
      return false;
    }
    if ((this.ingredienteSeleccionado.stockQuantity ?? 0) < 0) {
      this.mostrarAlerta('El stock no puede ser negativo', true);
      return false;
    }
    return true;
  }

  nuevoIngrediente(): IngredientDTO {
    return {
      name: '',
      extraCost: 3.5,
      isAvailable: true,
      unit: 'g',
      stockQuantity: 2000,
      minStock: 400,
      unitCost: 0.025,
      extraPortionQuantity: 50,
      allowAsExtra: true
    };
  }

  trackByIngredienteId(index: number, item: IngredientDTO): number {
    return item.id ?? index;
  }

  getCountDisponibles(): number {
    return this.kpiDisponibles;
  }

  getCountAlertasStock(): number {
    return this.kpiAlertasStock;
  }

  getValorTotalInsumos(): number {
    return this.kpiValorAlmacen;
  }

  private obtenerMensajeErrorCarga(err: any, fallback: string): string {
    if (err?.status === 0) {
      return 'No se pudo conectar con el servidor (Backend sin conexión). Verifique que el servicio esté activo e intente nuevamente.';
    }
    return err?.error?.message || fallback;
  }

  private mostrarAlerta(msg: string, error = false): void {
    this.mensaje = msg;
    this.esError = error;
    if (!error) {
      setTimeout(() => {
        if (!this.esError) {
          this.mensaje = '';
        }
      }, 3500);
    }
  }
}
