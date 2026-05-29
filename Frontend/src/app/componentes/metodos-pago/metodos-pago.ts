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
  metodosPagoPaginados: PaymentMethodDTO[] = [];
  metodoSeleccionado: PaymentMethodDTO | null = null;
  modoEdicion: boolean = false;
  mostrarFormulario: boolean = false;
  mensajeExito: string = '';
  mensajeError: string = '';
  mensajeAdvertenciaOrden: string = '';
  mensajeAdvertenciaNombre: string = '';

  // Paginación
  currentPage = 1;
  itemsPerPage = 10;

  // Modelo para el formulario
  formulario: PaymentMethodDTO = {
    name: '',
    description: '',
    isActive: true,
    displayOrder: 0
  };

  constructor(private paymentMethodService: PaymentMethodService) { }

  ngOnInit(): void {
    this.cargarMetodosPago();
  }

  cargarMetodosPago(): void {
    this.paymentMethodService.listarMetodosPago().subscribe({
      next: (data) => {
        this.metodosPago = data.sort((a, b) => a.displayOrder - b.displayOrder);
        this.aplicarPaginacion();
      },
      error: (error) => {
        this.mostrarError('Error al cargar los métodos de pago');
        console.error(error);
      }
    });
  }

  aplicarPaginacion(): void {
    const startIndex = (this.currentPage - 1) * this.itemsPerPage;
    const endIndex = startIndex + this.itemsPerPage;
    this.metodosPagoPaginados = this.metodosPago.slice(startIndex, endIndex);
  }

  onPageChange(page: number): void {
    this.currentPage = page;
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
    // Validar antes de enviar
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

    if (this.modoEdicion && this.metodoSeleccionado?.id) {
      // Actualizar
      this.paymentMethodService.actualizarMetodoPago(this.metodoSeleccionado.id, this.formulario).subscribe({
        next: () => {
          this.mostrarExito('Método de pago actualizado correctamente');
          this.cargarMetodosPago();
          this.cerrarFormulario();
        },
        error: (error) => {
          const mensaje = error.error?.message || 'Error al actualizar el método de pago';
          this.mostrarError(mensaje);
          console.error(error);
        }
      });
    } else {
      // Crear
      this.paymentMethodService.crearMetodoPago(this.formulario).subscribe({
        next: () => {
          this.mostrarExito('Método de pago creado correctamente');
          this.cargarMetodosPago();
          this.cerrarFormulario();
        },
        error: (error) => {
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
        this.mostrarError('Error al cambiar el estado');
        console.error(error);
      }
    });
  }

  eliminarMetodoPago(metodo: PaymentMethodDTO): void {
    if (!metodo.id) return;

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
    setTimeout(() => this.mensajeExito = '', 3000);
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
    
    const ordenExiste = this.metodosPago.some(metodo => 
      metodo.displayOrder === this.formulario.displayOrder && 
      metodo.id !== this.metodoSeleccionado?.id
    );
    
    if (ordenExiste) {
      this.mensajeAdvertenciaOrden = '⚠️ Este orden ya está en uso por otro método de pago';
    }
  }

  validarNombre(): void {
    this.mensajeAdvertenciaNombre = '';
    
    if (this.formulario.name.trim()) {
      const nombreExiste = this.metodosPago.some(metodo => 
        metodo.name.toLowerCase() === this.formulario.name.trim().toLowerCase() && 
        metodo.id !== this.metodoSeleccionado?.id
      );
      
      if (nombreExiste) {
        this.mensajeAdvertenciaNombre = '⚠️ Este nombre ya está en uso';
      }
    }
  }

  puedeGuardar(): boolean {
    return !this.mensajeAdvertenciaOrden && 
           !this.mensajeAdvertenciaNombre && 
           this.formulario.name.trim() !== '';
  }
}
