import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Size } from '../../services/size.service';
import { SizeDTO } from '../../models/admin.interface';
import { PaginationComponent } from '../pagination/pagination';

@Component({
  selector: 'app-sizes',
  imports: [CommonModule, FormsModule, PaginationComponent],
  templateUrl: './sizes.html',
  styleUrl: './sizes.css',
})
export class Sizes implements OnInit {
  sizes: SizeDTO[] = [];
  sizesFiltrados: SizeDTO[] = [];
  sizesPaginados: SizeDTO[] = [];
  selectedSize: SizeDTO = {
    name: '',
    extraCost: 0,
    recipeMultiplier: 1,
    stockQuantity: 50,
    minStock: 10,
    description: '',
    displayOrder: 0,
  };
  isEditing = false;
  showModal = false;
  loadError = '';
  mensaje = '';
  error = false;
  loading = true;
  guardando = false;
  terminoBusqueda = '';

  // KPIs
  kpiTotalSizes = 0;
  kpiAvgMultiplier = 1;
  kpiAvgExtraCost = 0;
  kpiTotalDough = 0;

  // Paginación
  currentPage = 1;
  itemsPerPage = 10;
  pageSizeOptions: number[] = [10, 20, 50];

  constructor(private readonly sizeService: Size) {}

  ngOnInit(): void {
    this.cargarSizes();
  }

  recargar(): void {
    this.cargarSizes();
  }

  reintentar(): void {
    this.cargarSizes();
  }

  abrirNuevo(): void {
    this.abrirModal();
  }

  cargarSizes(): void {
    this.loading = true;
    this.loadError = '';
    this.sizeService.obtenerTodos(true).subscribe({
      next: (data) => {
        this.sizes = (data || []).toSorted((a, b) => (a.displayOrder || 0) - (b.displayOrder || 0));
        this.recalcularKpis();
        this.filtrarLocalmente();
        this.loading = false;
      },
      error: (err) => {
        console.error('Error al cargar tamaños:', err);
        this.loadError =
          err?.status === 0
            ? 'No se pudo conectar con el servidor (Backend fuera de línea). Verifica el servicio en el puerto 8089.'
            : err?.error?.message || 'Error al cargar los tamaños de pizza.';
        this.loading = false;
      },
    });
  }

  private recalcularKpis(): void {
    this.kpiTotalSizes = this.sizes.length;
    if (this.kpiTotalSizes > 0) {
      const sumMult = this.sizes.reduce((acc, s) => acc + (s.recipeMultiplier || 1), 0);
      const sumCost = this.sizes.reduce((acc, s) => acc + (s.extraCost || 0), 0);
      this.kpiAvgMultiplier = sumMult / this.kpiTotalSizes;
      this.kpiAvgExtraCost = sumCost / this.kpiTotalSizes;
      this.kpiTotalDough = this.sizes.reduce((acc, s) => acc + (s.stockQuantity || 0), 0);
    } else {
      this.kpiAvgMultiplier = 1;
      this.kpiAvgExtraCost = 0;
      this.kpiTotalDough = 0;
    }
  }

  filtrarLocalmente(): void {
    const term = this.terminoBusqueda.trim().toLowerCase();
    if (!term) {
      this.sizesFiltrados = [...this.sizes];
    } else {
      this.sizesFiltrados = this.sizes.filter(
        (s) => s.name?.toLowerCase().includes(term) || s.description?.toLowerCase().includes(term),
      );
    }
    this.aplicarPaginacion();
  }

  onBusquedaChange(): void {
    this.currentPage = 1;
    this.filtrarLocalmente();
  }

  aplicarPaginacion(): void {
    const totalPages = Math.ceil(this.sizesFiltrados.length / this.itemsPerPage);
    if (this.currentPage > totalPages && totalPages > 0) {
      this.currentPage = totalPages;
    } else if (this.currentPage < 1) {
      this.currentPage = 1;
    }
    const startIndex = (this.currentPage - 1) * this.itemsPerPage;
    const endIndex = startIndex + this.itemsPerPage;
    this.sizesPaginados = this.sizesFiltrados.slice(startIndex, endIndex);
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

  abrirModal(size?: SizeDTO): void {
    this.isEditing = !!size;
    if (size) {
      this.selectedSize = {
        ...size,
        recipeMultiplier: size.recipeMultiplier ?? 1,
        stockQuantity: size.stockQuantity ?? 50,
        minStock: size.minStock ?? 10,
      };
    } else {
      this.selectedSize = {
        name: '',
        extraCost: 0,
        recipeMultiplier: 1,
        stockQuantity: 50,
        minStock: 10,
        description: '',
        displayOrder: this.sizes.length + 1,
      };
    }
    this.showModal = true;
  }

  cerrarModal(): void {
    this.showModal = false;
    this.selectedSize = {
      name: '',
      extraCost: 0,
      recipeMultiplier: 1,
      stockQuantity: 50,
      minStock: 10,
      description: '',
      displayOrder: 0,
    };
  }

  guardar(): void {
    if (this.guardando) {
      return;
    }
    if (
      !this.selectedSize.name ||
      this.selectedSize.extraCost < 0 ||
      (this.selectedSize.recipeMultiplier ?? 1) <= 0
    ) {
      this.mostrarMensaje(
        'Complete los campos requeridos correctamente. El multiplicador debe ser mayor a 0.',
        true,
      );
      return;
    }

    this.guardando = true;

    if (this.isEditing && this.selectedSize.id) {
      this.sizeService.actualizar(this.selectedSize.id, this.selectedSize).subscribe({
        next: () => {
          this.guardando = false;
          this.mostrarMensaje('Tamaño actualizado correctamente');
          this.cargarSizes();
          this.cerrarModal();
        },
        error: (err) => {
          console.error('Error al actualizar tamaño:', err);
          this.guardando = false;
          this.mostrarMensaje(err?.error?.message || 'Error al actualizar tamaño', true);
        },
      });
    } else {
      this.sizeService.crear(this.selectedSize).subscribe({
        next: () => {
          this.guardando = false;
          this.mostrarMensaje('Tamaño creado correctamente');
          this.cargarSizes();
          this.cerrarModal();
        },
        error: (err) => {
          console.error('Error al crear tamaño:', err);
          this.guardando = false;
          this.mostrarMensaje(err?.error?.message || 'Error al crear tamaño', true);
        },
      });
    }
  }


  restaurar(id: number, nombre?: string): void {
    if (confirm('¿Desea restaurar el tamaño' + (nombre ? ` "${nombre}"` : '') + '?')) {
      this.sizeService.restaurar(id).subscribe({
        next: () => {
          this.mostrarMensaje('Tamaño' + (nombre ? ` "${nombre}"` : '') + ' restaurado con éxito');
          this.cargarSizes();
        },
        error: (err) => {
          console.error('Error al restaurar tamaño:', err);
          this.mostrarMensaje(err?.error?.message || 'Error al restaurar tamaño', true);
        }
      });
    }
  }

  eliminar(id: number): void {
    if (confirm('¿Está seguro de eliminar este tamaño?')) {
      this.sizeService.eliminar(id).subscribe({
        next: () => {
          this.mostrarMensaje('Tamaño eliminado correctamente');
          this.cargarSizes();
        },
        error: (err) => {
          console.error('Error al eliminar tamaño:', err);
          this.mostrarMensaje(err?.error?.message || 'Error al eliminar tamaño', true);
        },
      });
    }
  }

  mostrarMensaje(msg: string, esError = false): void {
    this.mensaje = msg;
    this.error = esError;
    setTimeout(() => (this.mensaje = ''), 3500);
  }

  trackBySizeId(index: number, item: SizeDTO): number {
    return item.id ?? index;
  }
}
