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
  ingredienteSeleccionado: IngredientDTO = this.nuevoIngrediente();
  modoEdicion: boolean = false;
  mostrarModal: boolean = false;
  filtroDisponibilidad: string = 'TODOS';
  loading: boolean = true;

  // PaginaciÃ³n
  currentPage = 1;
  itemsPerPage = 10;

  constructor(private readonly ingredientService: IngredientService) {}

  ngOnInit(): void {
    this.cargarIngredientes();
  }

  cargarIngredientes(): void {
    this.loading = true;
    this.ingredientService.obtenerTodos().subscribe({
      next: (data) => {
        this.ingredientes = data;
        this.aplicarFiltro();
        this.loading = false;
      },
      error: (error) => {
        console.error('Error al cargar ingredientes:', error);
        alert('Error al cargar los ingredientes');
        this.loading = false;
      }
    });
  }

  aplicarFiltro(): void {
    if (this.filtroDisponibilidad === 'DISPONIBLES') {
      this.ingredientesFiltrados = this.ingredientes.filter(i => i.isAvailable);
    } else if (this.filtroDisponibilidad === 'NO_DISPONIBLES') {
      this.ingredientesFiltrados = this.ingredientes.filter(i => !i.isAvailable);
    } else {
      this.ingredientesFiltrados = [...this.ingredientes];
    }
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

  abrirModal(ingrediente?: IngredientDTO): void {
    if (ingrediente) {
      this.modoEdicion = true;
      this.ingredienteSeleccionado = { ...ingrediente };
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
    if (!this.validarIngrediente()) {
      return;
    }

    if (this.modoEdicion && this.ingredienteSeleccionado.id) {
      this.ingredientService.actualizar(this.ingredienteSeleccionado.id, this.ingredienteSeleccionado).subscribe({
        next: () => {
          alert('Ingrediente actualizado exitosamente');
          this.cargarIngredientes();
          this.cerrarModal();
        },
        error: (error) => {
          console.error('Error al actualizar ingrediente:', error);
          alert('Error al actualizar el ingrediente');
        }
      });
    } else {
      this.ingredientService.crear(this.ingredienteSeleccionado).subscribe({
        next: () => {
          alert('Ingrediente creado exitosamente');
          this.cargarIngredientes();
          this.cerrarModal();
        },
        error: (error) => {
          console.error('Error al crear ingrediente:', error);
          alert('Error al crear el ingrediente');
        }
      });
    }
  }

  eliminar(id: number): void {
    if (confirm('Â¿EstÃ¡ seguro de eliminar este ingrediente?')) {
      this.ingredientService.eliminar(id).subscribe({
        next: () => {
          alert('Ingrediente eliminado exitosamente');
          this.cargarIngredientes();
        },
        error: (error) => {
          console.error('Error al eliminar ingrediente:', error);
          alert('Error al eliminar el ingrediente');
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
        this.aplicarFiltro();
      },
      error: (error) => {
        console.error('Error al cambiar disponibilidad:', error);
        alert('Error al cambiar la disponibilidad');
      }
    });
  }

  validarIngrediente(): boolean {
    if (!this.ingredienteSeleccionado.name?.trim()) {
      alert('El nombre es obligatorio');
      return false;
    }
    if (this.ingredienteSeleccionado.extraCost < 0) {
      alert('El costo extra no puede ser negativo');
      return false;
    }
    return true;
  }

  nuevoIngrediente(): IngredientDTO {
    return {
      name: '',
      extraCost: 0,
      isAvailable: true
    };
  }

  getCountDisponibles(): number {
    return this.ingredientes.filter(i => i.isAvailable).length;
  }

  getCountNoDisponibles(): number {
    return this.ingredientes.filter(i => !i.isAvailable).length;
  }
}

