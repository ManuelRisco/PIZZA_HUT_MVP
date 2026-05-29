import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Order } from '../../services/order.service';
import { OrderCompleteDTO } from '../../models/admin.interface';
import { PaginationComponent } from '../pagination/pagination';

@Component({
  selector: 'app-orders',
  standalone: true,
  imports: [CommonModule, FormsModule, PaginationComponent],
  templateUrl: './orders.html',
  styleUrl: './orders.css'
})
export class Orders implements OnInit {
  orders: OrderCompleteDTO[] = [];
  filteredOrders: OrderCompleteDTO[] = [];
  ordersPaginados: OrderCompleteDTO[] = [];
  currentPage: number = 1;
  itemsPerPage: number = 10;
  selectedOrder: OrderCompleteDTO | null = null;
  filtroEstado: string = 'TODOS';
  filtroDeliveryType: string = 'TODOS';
  showModal = false;
  mensaje = '';
  error = false;

  estados = ['PENDING', 'CONFIRMED', 'PREPARING', 'OUT_FOR_DELIVERY', 'DELIVERED', 'CANCELLED'];

  constructor(private orderService: Order) {}

  ngOnInit(): void {
    this.cargarOrders();
  }

  cargarOrders(): void {
    this.orderService.obtenerTodosCompletos().subscribe({
      next: (data) => {
        this.orders = data.sort((a, b) => 
          new Date(b.createdAt!).getTime() - new Date(a.createdAt!).getTime()
        );
        this.aplicarFiltro();
      },
      error: (err) => {
        console.error('Error al cargar pedidos:', err);
        this.mostrarMensaje('Error al cargar pedidos', true);
      }
    });
  }

  aplicarFiltro(): void {
    let resultado = this.orders;
    
    // Filtrar por estado
    if (this.filtroEstado !== 'TODOS') {
      resultado = resultado.filter(o => o.status === this.filtroEstado);
    }
    
    // Filtrar por tipo de delivery
    if (this.filtroDeliveryType !== 'TODOS') {
      resultado = resultado.filter(o => o.deliveryType === this.filtroDeliveryType);
    }
    
    this.filteredOrders = resultado;
    this.aplicarPaginacion();
  }

  aplicarPaginacion(): void {
    const startIndex = (this.currentPage - 1) * this.itemsPerPage;
    const endIndex = startIndex + this.itemsPerPage;
    this.ordersPaginados = this.filteredOrders.slice(startIndex, endIndex);
  }

  onPageChange(page: number): void {
    this.currentPage = page;
    this.aplicarPaginacion();
  }

  verDetalles(order: OrderCompleteDTO): void {
    this.selectedOrder = order;
    this.showModal = true;
  }

  cerrarModal(): void {
    this.showModal = false;
    this.selectedOrder = null;
  }

  cambiarEstado(order: OrderCompleteDTO, nuevoEstado: string): void {
    this.orderService.cambiarEstado(order.id!, nuevoEstado).subscribe({
      next: (orderActualizado) => {
        // Actualizar el estado del pedido localmente sin recargar toda la lista
        order.status = orderActualizado.status;
        this.mostrarMensaje('Estado actualizado correctamente');
      },
      error: (err) => {
        console.error('Error al actualizar estado:', err);
        console.error('Error details:', err.error);
        this.mostrarMensaje('Error al actualizar estado', true);
      }
    });
  }
  
  esPickup(order: OrderCompleteDTO): boolean {
    return order.deliveryType === 'PICKUP';
  }
  
  esDelivery(order: OrderCompleteDTO): boolean {
    return order.deliveryType === 'DELIVERY';
  }

  getBadgeClass(status: string): string {
    const badges: any = {
      'PENDING': 'bg-warning text-dark',
      'CONFIRMED': 'bg-info',
      'PREPARING': 'bg-primary',
      'OUT_FOR_DELIVERY': 'bg-secondary',
      'DELIVERED': 'bg-success',
      'CANCELLED': 'bg-danger'
    };
    return badges[status] || 'bg-secondary';
  }

  getEstadoTexto(status: string): string {
    const textos: any = {
      'PENDING': 'Pendiente',
      'CONFIRMED': 'Confirmado',
      'PREPARING': 'Preparando',
      'OUT_FOR_DELIVERY': 'En camino',
      'DELIVERED': 'Entregado',
      'CANCELLED': 'Cancelado'
    };
    return textos[status] || status;
  }

  mostrarMensaje(msg: string, esError = false): void {
    this.mensaje = msg;
    this.error = esError;
    setTimeout(() => this.mensaje = '', 3000);
  }
}
