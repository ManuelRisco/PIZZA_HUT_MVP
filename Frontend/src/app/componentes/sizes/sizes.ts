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
  styleUrl: './sizes.css'
})
export class Sizes implements OnInit {
  sizes: SizeDTO[] = [];
  sizesPaginados: SizeDTO[] = [];
  selectedSize: SizeDTO = { name: '', extraCost: 0, description: '', displayOrder: 0 };
  isEditing = false;
  showModal = false;
  mensaje = '';
  error = false;
  loading = true;

  // PaginaciÃ³n
  currentPage = 1;
  itemsPerPage = 10;

  constructor(private readonly sizeService: Size) {}

  ngOnInit(): void {
    this.cargarSizes();
  }

  cargarSizes(): void {
    this.loading = true;
    this.sizeService.obtenerTodos().subscribe({
      next: (data) => {
        this.sizes = data.toSorted((a, b) => (a.displayOrder || 0) - (b.displayOrder || 0));
        this.aplicarPaginacion();
        this.loading = false;
      },
      error: (err) => {
        console.error('Error al cargar tamaÃ±os:', err);
        this.mostrarMensaje('Error al cargar tamaÃ±os', true);
        this.loading = false;
      }
    });
  }

  aplicarPaginacion(): void {
    const totalPages = Math.ceil(this.sizes.length / this.itemsPerPage);
    if (this.currentPage > totalPages && totalPages > 0) {
      this.currentPage = totalPages;
    } else if (this.currentPage < 1) {
      this.currentPage = 1;
    }
    const startIndex = (this.currentPage - 1) * this.itemsPerPage;
    const endIndex = startIndex + this.itemsPerPage;
    this.sizesPaginados = this.sizes.slice(startIndex, endIndex);
  }

  onPageChange(page: number): void {
    this.currentPage = page;
    this.aplicarPaginacion();
  }

  abrirModal(size?: SizeDTO): void {
    this.isEditing = !!size;
    if (size) {
      this.selectedSize = { ...size };
    } else {
      this.selectedSize = { name: '', extraCost: 0, description: '', displayOrder: 0 };
    }
    this.showModal = true;
  }

  cerrarModal(): void {
    this.showModal = false;
    this.selectedSize = { name: '', extraCost: 0, description: '', displayOrder: 0 };
  }

  guardar(): void {
    if (!this.selectedSize.name || this.selectedSize.extraCost < 0) {
      this.mostrarMensaje('Complete los campos requeridos', true);
      return;
    }

    if (this.isEditing && this.selectedSize.id) {
      this.sizeService.actualizar(this.selectedSize.id, this.selectedSize).subscribe({
        next: () => {
          this.mostrarMensaje('TamaÃ±o actualizado correctamente');
          this.cargarSizes();
          this.cerrarModal();
        },
        error: (err) => {
          console.error('Error al actualizar:', err);
          this.mostrarMensaje('Error al actualizar tamaÃ±o', true);
        }
      });
    } else {
      this.sizeService.crear(this.selectedSize).subscribe({
        next: () => {
          this.mostrarMensaje('TamaÃ±o creado correctamente');
          this.cargarSizes();
          this.cerrarModal();
        },
        error: (err) => {
          console.error('Error al crear:', err);
          this.mostrarMensaje('Error al crear tamaÃ±o', true);
        }
      });
    }
  }

  eliminar(id: number): void {
    if (confirm('Â¿EstÃ¡ seguro de eliminar este tamaÃ±o?')) {
      this.sizeService.eliminar(id).subscribe({
        next: () => {
          this.mostrarMensaje('TamaÃ±o eliminado correctamente');
          this.cargarSizes();
        },
        error: (err) => {
          console.error('Error al eliminar:', err);
          this.mostrarMensaje('Error al eliminar tamaÃ±o', true);
        }
      });
    }
  }

  mostrarMensaje(msg: string, esError = false): void {
    this.mensaje = msg;
    this.error = esError;
    setTimeout(() => this.mensaje = '', 3000);
  }
}

