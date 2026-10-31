import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Payment } from '../../services/payment.service';
import { PaymentDTO } from '../../models/admin.interface';
import { PaginationComponent } from '../pagination/pagination';

@Component({
  selector: 'app-payments',
  imports: [CommonModule, FormsModule, PaginationComponent],
  templateUrl: './payments.html',
  styleUrl: './payments.css'
})
export class Payments implements OnInit {
  payments: PaymentDTO[] = [];
  filteredPayments: PaymentDTO[] = [];
  paymentsPaginados: PaymentDTO[] = [];
  filtroEstado: string = 'TODOS';
  terminoBusqueda: string = '';
  loadError = '';
  mensaje = '';
  error = false;
  loading = false;

  // KPIs precalculados
  kpiTotalPagos = 0;
  kpiPagados = 0;
  kpiPendientes = 0;
  kpiFallidosReembolsos = 0;
  kpiTotalMonto = 0;

  // Paginación
  currentPage = 1;
  itemsPerPage = 10;
  pageSizeOptions: number[] = [10, 25, 50];

  constructor(private readonly paymentService: Payment) {}

  ngOnInit(): void {
    this.cargarPayments();
  }

  recargar(): void {
    this.cargarPayments();
  }

  reintentar(): void {
    this.cargarPayments();
  }

  cargarPayments(): void {
    this.loading = true;
    this.loadError = '';
    this.paymentService.obtenerTodos().subscribe({
      next: (data) => {
        this.payments = (data || []).toSorted((a, b) =>
          new Date(b.createdAt!).getTime() - new Date(a.createdAt!).getTime()
        );
        this.recalcularKpis();
        this.aplicarFiltro();
        this.loading = false;
      },
      error: (err) => {
        console.error('Error al cargar pagos:', err);
        this.loadError = err?.status === 0
          ? 'No se pudo conectar con el servidor (Backend fuera de línea). Verifica el puerto 8089.'
          : (err?.error?.message || 'Error al cargar los registros de pagos.');
        this.loading = false;
      }
    });
  }

  private recalcularKpis(): void {
    let pagados = 0;
    let pendientes = 0;
    let fallidos = 0;
    let monto = 0;

    for (const p of this.payments) {
      if (p.status === 'PAID') {
        pagados++;
        monto += (p.amount ?? 0);
      } else if (p.status === 'PENDING') {
        pendientes++;
      } else if (p.status === 'FAILED' || p.status === 'REFUNDED') {
        fallidos++;
      }
    }

    this.kpiTotalPagos = this.payments.length;
    this.kpiPagados = pagados;
    this.kpiPendientes = pendientes;
    this.kpiFallidosReembolsos = fallidos;
    this.kpiTotalMonto = monto;
  }

  aplicarFiltro(): void {
    let resultado = [...this.payments];

    if (this.filtroEstado !== 'TODOS') {
      resultado = resultado.filter(p => p.status === this.filtroEstado);
    }

    const term = this.terminoBusqueda.trim().toLowerCase();
    if (term) {
      resultado = resultado.filter(
        p =>
          String(p.id).includes(term) ||
          String(p.orderId).includes(term) ||
          p.transactionId?.toLowerCase().includes(term)
      );
    }

    this.filteredPayments = resultado;
    this.aplicarPaginacion();
  }

  onFiltroChange(): void {
    this.currentPage = 1;
    this.aplicarFiltro();
  }

  limpiarFiltros(): void {
    this.filtroEstado = 'TODOS';
    this.terminoBusqueda = '';
    this.currentPage = 1;
    this.aplicarFiltro();
  }

  aplicarPaginacion(): void {
    const totalPages = Math.ceil(this.filteredPayments.length / this.itemsPerPage);
    if (this.currentPage > totalPages && totalPages > 0) {
      this.currentPage = totalPages;
    } else if (this.currentPage < 1) {
      this.currentPage = 1;
    }
    const startIndex = (this.currentPage - 1) * this.itemsPerPage;
    const endIndex = startIndex + this.itemsPerPage;
    this.paymentsPaginados = this.filteredPayments.slice(startIndex, endIndex);
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

  getEstadoTexto(status: string): string {
    const textos: Record<string, string> = {
      'PENDING': 'Pendiente',
      'PAID': 'Completado',
      'FAILED': 'Fallido',
      'REFUNDED': 'Reembolsado'
    };
    return textos[status] || status;
  }

  mostrarMensaje(msg: string, esError = false): void {
    this.mensaje = msg;
    this.error = esError;
    setTimeout(() => (this.mensaje = ''), 3500);
  }

  trackByPaymentId(index: number, item: PaymentDTO): number {
    return item.id ?? index;
  }
}
