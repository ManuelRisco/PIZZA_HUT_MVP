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
  mensaje = '';
  error = false;
  loading = false;

  // Paginación
  currentPage = 1;
  itemsPerPage = 10;

  constructor(private paymentService: Payment) {}

  ngOnInit(): void {
    this.cargarPayments();
  }

  cargarPayments(): void {
    this.loading = true;
    this.paymentService.obtenerTodos().subscribe({
      next: (data) => {
        this.payments = data.sort((a, b) => new Date(b.createdAt!).getTime() - new Date(a.createdAt!).getTime());
        this.aplicarFiltro();
        this.loading = false;
      },
      error: (err) => {
        console.error('Error al cargar pagos:', err);
        this.mostrarMensaje('Error al cargar pagos', true);
        this.loading = false;
      }
    });
  }

  aplicarFiltro(): void {
    if (this.filtroEstado === 'TODOS') {
      this.filteredPayments = this.payments;
    } else {
      this.filteredPayments = this.payments.filter(p => p.status === this.filtroEstado);
    }
    this.aplicarPaginacion();
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

  getBadgeClass(status: string): string {
    const badges: any = {
      'PENDING': 'bg-warning text-dark',
      'PAID': 'bg-success',
      'FAILED': 'bg-danger',
      'REFUNDED': 'bg-secondary'
    };
    return badges[status] || 'bg-secondary';
  }

  getEstadoTexto(status: string): string {
    const textos: any = {
      'PENDING': 'Pendiente',
      'PAID': 'Pagado',
      'FAILED': 'Fallido',
      'REFUNDED': 'Reembolsado'
    };
    return textos[status] || status;
  }

  getCountByStatus(status: string): number {
    return this.payments.filter(p => p.status === status).length;
  }

  mostrarMensaje(msg: string, esError = false): void {
    this.mensaje = msg;
    this.error = esError;
    setTimeout(() => this.mensaje = '', 3000);
  }
}
