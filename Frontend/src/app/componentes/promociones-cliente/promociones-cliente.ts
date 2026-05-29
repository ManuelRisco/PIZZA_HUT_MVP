import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PromocionService, Promotion } from '../../services/promocion.service';
import { CartService } from '../../services/cart.service';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-promociones-cliente',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './promociones-cliente.html',
  styleUrls: ['./promociones-cliente.css']
})
export class PromocionesClienteComponent implements OnInit {
  promociones: Promotion[] = [];
  promocionesFiltradas: Promotion[] = [];
  searchTerm: string = '';
  tipoSeleccionado: string = 'TODOS';
  loading = false;
  error = '';
  mensaje = '';
  esError = false;
  codigoPromocion = '';
  aplicandoCodigo = false;

  tiposDescuento = [
    { value: 'TODOS', label: 'Todas', icon: 'bi-percent' },
    { value: 'PERCENTAGE', label: 'Porcentaje', icon: 'bi-percent' },
    { value: 'FIXED_AMOUNT', label: 'Monto Fijo', icon: 'bi-cash-coin' },
    { value: 'BUNDLE', label: 'Combos', icon: 'bi-basket3' }
  ];

  private promocionService = inject(PromocionService);
  private cartService = inject(CartService);
  private authService = inject(AuthService);

  ngOnInit(): void {
    this.cargarPromociones();
  }

  cargarPromociones(): void {
    this.loading = true;
    this.error = '';

    this.promocionService.listarActivas().subscribe({
      next: (data) => {
        // Usar el campo 'currentlyActive' que calcula el backend
        // que es más confiable que recalcular en frontend
        this.promociones = data.filter(p => {
          // Si currentlyActive viene del backend, usarlo como principal indicador
          if (p.currentlyActive !== undefined) {
            return p.currentlyActive;
          }
          // Fallback a isActive si currentlyActive no viene
          return p.isActive && this.esVigente(p);
        });
        this.aplicarFiltros();
        this.loading = false;

        // Debug: mostrar cuántas promociones se cargaron
        console.log(`Promociones cargadas: ${this.promociones.length}`);
      },
      error: (err) => {
        console.error('Error al cargar promociones:', err);
        this.error = 'Error al cargar las promociones';
        this.loading = false;
      }
    });
  }

  cambiarTipo(tipo: string): void {
    this.tipoSeleccionado = tipo;
    this.aplicarFiltros();
  }

  aplicarFiltros(): void {
    let resultado = [...this.promociones];

    // Filtrar por tipo
    if (this.tipoSeleccionado !== 'TODOS') {
      resultado = resultado.filter(p => p.discountType === this.tipoSeleccionado);
    }

    // Filtrar por búsqueda
    if (this.searchTerm.trim()) {
      const term = this.searchTerm.toLowerCase();
      resultado = resultado.filter(p =>
        p.name.toLowerCase().includes(term) ||
        p.code.toLowerCase().includes(term) ||
        (p.description && p.description.toLowerCase().includes(term))
      );
    }

    this.promocionesFiltradas = resultado;
  }

  limpiarBuscador(): void {
    this.searchTerm = '';
    this.aplicarFiltros();
  }

  copiarCodigo(codigo: string): void {
    navigator.clipboard.writeText(codigo).then(() => {
      this.mostrarMensaje(`Código ${codigo} copiado al portapapeles`, false);
    });
  }

  aplicarPromocion(): void {
    if (!this.codigoPromocion.trim()) {
      this.mostrarMensaje('Ingrese un código de promoción', true);
      return;
    }

    this.aplicandoCodigo = true;
    const total = this.cartService.getTotals().subtotal;

    this.promocionService.validarPromocion(this.codigoPromocion, total).subscribe({
      next: (response) => {
        this.mostrarMensaje('Código de promoción válido. Aplícalo en el checkout', false);
        this.codigoPromocion = '';
        this.aplicandoCodigo = false;
      },
      error: (err) => {
        this.mostrarMensaje(err.error?.message || 'Código de promoción inválido', true);
        this.aplicandoCodigo = false;
      }
    });
  }

  esVigente(promo: Promotion): boolean {
    if (!promo.startDate || !promo.endDate) {
      console.warn('Promoción sin fechas:', promo);
      return false;
    }

    try {
      const ahora = new Date();
      // Convertir strings de fecha a Date, asumiendo formato ISO
      const inicio = new Date(promo.startDate);
      const fin = new Date(promo.endDate);

      // Validar que las fechas sean válidas
      if (isNaN(inicio.getTime()) || isNaN(fin.getTime())) {
        console.warn('Fechas inválidas para promoción:', promo);
        return false;
      }

      // Comparar: ahora debe estar entre inicio y fin (inclusive)
      const esVigente = ahora >= inicio && ahora <= fin;

      if (!esVigente) {
        console.debug(`Promoción ${promo.code} no vigente:`, {
          ahora: ahora.toISOString(),
          inicio: inicio.toISOString(),
          fin: fin.toISOString(),
          isActive: promo.isActive
        });
      }

      return esVigente;
    } catch (error) {
      console.error('Error al verificar si promoción es vigente:', error, promo);
      return false;
    }
  }

  obtenerTextoDescuento(promo: Promotion): string {
    switch (promo.discountType) {
      case 'PERCENTAGE':
        return `${promo.discountValue}% OFF`;
      case 'FIXED_AMOUNT':
        return `S/ ${promo.discountValue} OFF`;
      case 'BUNDLE':
        return `S/ ${promo.finalPrice}`;
      default:
        return 'Descuento';
    }
  }

  obtenerColorTipo(tipo: string): string {
    switch (tipo) {
      case 'PERCENTAGE':
        return '#e74c3c';
      case 'FIXED_AMOUNT':
        return '#27ae60';
      case 'BUNDLE':
        return '#3498db';
      default:
        return '#95a5a6';
    }
  }

  formatearFecha(fecha: string): string {
    const date = new Date(fecha);
    return date.toLocaleDateString('es-PE', { day: '2-digit', month: 'short', year: 'numeric' });
  }

  private mostrarMensaje(texto: string, error: boolean): void {
    this.mensaje = texto;
    this.esError = error;
    setTimeout(() => {
      this.mensaje = '';
    }, 3000);
  }
}
