import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PaymentMethodService } from '../../services/payment-method.service';
import { PaymentMethodDTO } from '../../models/payment-method.interface';
import { PaginationComponent } from '../pagination/pagination';

@Component({
  selector: 'app-metodos-pago',
  standalone: true,
  imports: [CommonModule, FormsModule, PaginationComponent],
  templateUrl: './metodos-pago.html',
  styleUrls: ['./metodos-pago.css']
})
export class MetodosPagoComponent implements OnInit {
  metodosPago: PaymentMethodDTO[] = [];
  metodosPagoFiltrados: PaymentMethodDTO[] = [];
  metodosPagoPaginados: PaymentMethodDTO[] = [];
  metodoSeleccionado: PaymentMethodDTO | null = null;
  modoEdicion: boolean = false;
  mostrarFormulario: boolean = false;
  loadError: string = '';
  mensajeExito: string = '';
  mensajeError: string = '';
  mensajeAdvertenciaOrden: string = '';
  mensajeAdvertenciaNombre: string = '';
  loading: boolean = false;
  guardando: boolean = false;
  terminoBusqueda: string = '';

  // KPIs
  kpiTotalMetodos = 0;
  kpiActivos = 0;
  kpiEnUso = 0;

  // Paginación
  currentPage = 1;
  itemsPerPage = 10;
  pageSizeOptions: number[] = [10, 20, 50];

  // Modelo para el formulario
  formulario: PaymentMethodDTO = {
    name: '',
    description: '',
    isActive: true,
    displayOrder: 0
  };

  constructor(private readonly paymentMethodService: PaymentMethodService) {}

  ngOnInit(): void {
    this.cargarMetodosPago();
  }

  recargar(): void {
    this.cargarMetodosPago();
  }

  reintentar(): void {
    this.cargarMetodosPago();
  }

  cargarMetodosPago(): void {
    this.loading = true;
    this.loadError = '';
    this.paymentMethodService.listarMetodosPago().subscribe({
      next: (data) => {
        this.metodosPago = (data || []).toSorted((a, b) => (a.displayOrder || 0) - (b.displayOrder || 0));
        this.recalcularKpis();
        this.filtrarLocalmente();
        this.loading = false;
      },
      error: (error) => {
        console.error('Error al cargar métodos de pago:', error);
        this.loadError = error?.status === 0
          ? 'No se pudo conectar con el servidor (Backend fuera de línea). Verifica el puerto 8089.'
          : (error?.error?.message || 'Error al cargar los métodos de pago.');
        this.loading = false;
      }
    });
  }

  private recalcularKpis(): void {
    let activos = 0;
    let enUso = 0;

    for (const m of this.metodosPago) {
      if (m.isActive) activos++;
      if (m.inUse) enUso++;
    }

    this.kpiTotalMetodos = this.metodosPago.length;
    this.kpiActivos = activos;
    this.kpiEnUso = enUso;
  }

  filtrarLocalmente(): void {
    const term = this.terminoBusqueda.trim().toLowerCase();
    if (!term) {
      this.metodosPagoFiltrados = [...this.metodosPago];
    } else {
      this.metodosPagoFiltrados = this.metodosPago.filter(
        (m) =>
          m.name?.toLowerCase().includes(term) ||
          m.description?.toLowerCase().includes(term)
      );
    }
    this.aplicarPaginacion();
  }

  onBusquedaChange(): void {
    this.currentPage = 1;
    this.filtrarLocalmente();
  }

  aplicarPaginacion(): void {
    const totalPages = Math.ceil(this.metodosPagoFiltrados.length / this.itemsPerPage);
    if (this.currentPage > totalPages && totalPages > 0) {
      this.currentPage = totalPages;
    } else if (this.currentPage < 1) {
      this.currentPage = 1;
    }
    const startIndex = (this.currentPage - 1) * this.itemsPerPage;
    const endIndex = startIndex + this.itemsPerPage;
    this.metodosPagoPaginados = this.metodosPagoFiltrados.slice(startIndex, endIndex);
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

  abrirFormularioNuevo(): void {
    this.modoEdicion = false;
    this.mostrarFormulario = true;
    this.formulario = {
      name: '',
      description: '',
      isActive: true,
      displayOrder: this.metodosPago.length + 1
    };
    this.limpiarMensajes();
  }

  abrirFormularioEditar(metodo: PaymentMethodDTO): void {
    this.modoEdicion = true;
    this.mostrarFormulario = true;
    this.metodoSeleccionado = metodo;
    this.formulario = { ...metodo };
    this.limpiarMensajes();
  }

  cerrarFormulario(): void {
    this.mostrarFormulario = false;
    this.metodoSeleccionado = null;
    this.limpiarMensajes();
  }

  guardarMetodoPago(): void {
    if (this.guardando) return;

    if (!this.formulario.name.trim()) {
      this.mostrarError('El nombre es obligatorio');
      return;
    }

    if (this.mensajeAdvertenciaOrden) {
      this.mostrarError('El orden de visualización ya está en uso');
      return;
    }

    if (this.mensajeAdvertenciaNombre) {
      this.mostrarError('El nombre ya está en uso');
      return;
    }

    this.guardando = true;

    if (this.modoEdicion && this.metodoSeleccionado?.id) {
      this.paymentMethodService.actualizarMetodoPago(this.metodoSeleccionado.id, this.formulario).subscribe({
        next: () => {
          this.guardando = false;
          this.mostrarExito('Método de pago actualizado correctamente');
          this.cargarMetodosPago();
          this.cerrarFormulario();
        },
        error: (error) => {
          this.guardando = false;
          const mensaje = error.error?.message || 'Error al actualizar el método de pago';
          this.mostrarError(mensaje);
          console.error(error);
        }
      });
    } else {
      this.paymentMethodService.crearMetodoPago(this.formulario).subscribe({
        next: () => {
          this.guardando = false;
          this.mostrarExito('Método de pago creado correctamente');
          this.cargarMetodosPago();
          this.cerrarFormulario();
        },
        error: (error) => {
          this.guardando = false;
          const mensaje = error.error?.message || 'Error al crear el método de pago';
          this.mostrarError(mensaje);
          console.error(error);
        }
      });
    }
  }

  cambiarEstado(metodo: PaymentMethodDTO): void {
    if (!metodo.id) return;

    const nuevoEstado = !metodo.isActive;
    this.paymentMethodService.cambiarEstado(metodo.id, nuevoEstado).subscribe({
      next: () => {
        this.mostrarExito(`Método de pago ${nuevoEstado ? 'activado' : 'desactivado'} correctamente`);
        this.cargarMetodosPago();
      },
      error: (error) => {
        this.mostrarError('Error al cambiar el estado del método de pago');
        console.error(error);
      }
    });
  }

  eliminarMetodoPago(metodo: PaymentMethodDTO): void {
    if (!metodo.id) return;
    if (metodo.inUse) {
      this.mostrarError('Este método de pago no se puede eliminar porque ya está en uso');
      return;
    }

    if (confirm(`¿Está seguro de eliminar el método de pago "${metodo.name}"?`)) {
      this.paymentMethodService.eliminarMetodoPago(metodo.id).subscribe({
        next: () => {
          this.mostrarExito('Método de pago eliminado correctamente');
          this.cargarMetodosPago();
        },
        error: (error) => {
          this.mostrarError('Error al eliminar el método de pago. Puede estar en uso.');
          console.error(error);
        }
      });
    }
  }

  mostrarExito(mensaje: string): void {
    this.mensajeExito = mensaje;
    this.mensajeError = '';
    setTimeout(() => (this.mensajeExito = ''), 3500);
  }

  mostrarError(mensaje: string): void {
    this.mensajeError = mensaje;
    this.mensajeExito = '';
  }

  limpiarMensajes(): void {
    this.mensajeExito = '';
    this.mensajeError = '';
    this.mensajeAdvertenciaOrden = '';
    this.mensajeAdvertenciaNombre = '';
  }

  validarDisplayOrder(): void {
    this.mensajeAdvertenciaOrden = '';

    const ordenExiste = this.metodosPago.some(
      (metodo) =>
        metodo.displayOrder === this.formulario.displayOrder &&
        metodo.id !== this.metodoSeleccionado?.id
    );

    if (ordenExiste) {
      this.mensajeAdvertenciaOrden = 'Este orden de visualización ya está en uso';
    }
  }

  validarNombre(): void {
    this.mensajeAdvertenciaNombre = '';

    if (this.formulario.name.trim()) {
      const nombreExiste = this.metodosPago.some(
        (metodo) =>
          metodo.name.toLowerCase() === this.formulario.name.trim().toLowerCase() &&
          metodo.id !== this.metodoSeleccionado?.id
      );

      if (nombreExiste) {
        this.mensajeAdvertenciaNombre = 'Este nombre ya está en uso';
      }
    }
  }

  puedeGuardar(): boolean {
    return (
      !this.mensajeAdvertenciaOrden &&
      !this.mensajeAdvertenciaNombre &&
      this.formulario.name.trim() !== '' &&
      !this.guardando
    );
  }

  trackByMetodoId(index: number, item: PaymentMethodDTO): number {
    return item.id ?? index;
  }
}
