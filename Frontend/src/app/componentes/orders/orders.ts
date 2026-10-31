import { Component, OnInit, OnChanges, SimpleChanges, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
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
export class Orders implements OnInit, OnChanges {
  @Input() orderIdToOpen?: number | null = null;
  @Output() orderOpened = new EventEmitter<number>();

  orders: OrderCompleteDTO[] = [];
  filteredOrders: OrderCompleteDTO[] = [];
  ordersPaginados: OrderCompleteDTO[] = [];
  currentPage: number = 1;
  itemsPerPage: number = 10;
  pageSizeOptions: number[] = [10, 25, 50];

  selectedOrder: OrderCompleteDTO | null = null;
  filtroEstado: string = 'TODOS';
  filtroDeliveryType: string = 'TODOS';
  terminoBusqueda: string = '';
  showModal = false;
  loadError = '';
  mensaje = '';
  error = false;
  loading = true;
  updatingOrderId: number | null = null;

  // KPIs
  kpiTotalOrders = 0;
  kpiPendientes = 0;
  kpiEntregados = 0;
  kpiTotalVentas = 0;

  estados = ['PENDING', 'CONFIRMED', 'PREPARING', 'OUT_FOR_DELIVERY', 'DELIVERED', 'CANCELLED'];

  constructor(
    private readonly orderService: Order,
    private readonly route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    const queryId = this.route.snapshot.queryParams['id'] || this.route.snapshot.queryParams['orderId'];
    if (queryId && !isNaN(Number(queryId))) {
      this.orderIdToOpen = Number(queryId);
    }
    this.cargarOrders();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['orderIdToOpen'] && this.orderIdToOpen && this.orders && this.orders.length > 0) {
      this.abrirPedidoPorIdSiExiste(this.orderIdToOpen);
    }
  }

  recargar(): void {
    this.cargarOrders();
  }

  reintentar(): void {
    this.cargarOrders();
  }

  cargarOrders(): void {
    this.loading = true;
    this.loadError = '';
    this.orderService.obtenerTodosCompletos().subscribe({
      next: (data) => {
        this.orders = (data || []).toSorted((a, b) =>
          new Date(b.createdAt!).getTime() - new Date(a.createdAt!).getTime()
        );
        this.recalcularKpis();
        this.aplicarFiltro();
        this.loading = false;
        if (this.orderIdToOpen) {
          this.abrirPedidoPorIdSiExiste(this.orderIdToOpen);
        }
      },
      error: (err) => {
        console.error('Error al cargar pedidos:', err);
        this.loadError = err?.status === 0
          ? 'No se pudo conectar con el servidor (Backend fuera de línea). Verifica el puerto 8089.'
          : (err?.error?.message || 'Error al cargar los pedidos del sistema.');
        this.loading = false;
      }
    });
  }

  private recalcularKpis(): void {
    let pendientes = 0;
    let entregados = 0;
    let totalVentas = 0;

    for (const o of this.orders) {
      if (['PENDING', 'CONFIRMED', 'PREPARING', 'OUT_FOR_DELIVERY'].includes(o.status)) {
        pendientes++;
      }
      if (o.status === 'DELIVERED') {
        entregados++;
      }
      if (o.status !== 'CANCELLED') {
        totalVentas += (o.total ?? 0);
      }
    }

    this.kpiTotalOrders = this.orders.length;
    this.kpiPendientes = pendientes;
    this.kpiEntregados = entregados;
    this.kpiTotalVentas = totalVentas;
  }

  aplicarFiltro(): void {
    let resultado = [...this.orders];

    if (this.filtroEstado !== 'TODOS') {
      resultado = resultado.filter(o => o.status === this.filtroEstado);
    }

    if (this.filtroDeliveryType !== 'TODOS') {
      resultado = resultado.filter(o => o.deliveryType === this.filtroDeliveryType);
    }

    const term = this.terminoBusqueda.trim().toLowerCase();
    if (term) {
      resultado = resultado.filter(
        o =>
          String(o.id).includes(term) ||
          o.userName?.toLowerCase().includes(term) ||
          o.userEmail?.toLowerCase().includes(term) ||
          o.userPhone?.toLowerCase().includes(term) ||
          o.addressLine1?.toLowerCase().includes(term)
      );
    }

    this.filteredOrders = resultado;
    this.aplicarPaginacion();
  }

  onFiltroChange(): void {
    this.currentPage = 1;
    this.aplicarFiltro();
  }

  limpiarFiltros(): void {
    this.filtroEstado = 'TODOS';
    this.filtroDeliveryType = 'TODOS';
    this.terminoBusqueda = '';
    this.currentPage = 1;
    this.aplicarFiltro();
  }

  aplicarPaginacion(): void {
    const totalPages = Math.ceil(this.filteredOrders.length / this.itemsPerPage);
    if (this.currentPage > totalPages && totalPages > 0) {
      this.currentPage = totalPages;
    } else if (this.currentPage < 1) {
      this.currentPage = 1;
    }
    const startIndex = (this.currentPage - 1) * this.itemsPerPage;
    const endIndex = startIndex + this.itemsPerPage;
    this.ordersPaginados = this.filteredOrders.slice(startIndex, endIndex);
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

  abrirPedidoPorIdSiExiste(id: number): void {
    if (!id || !this.orders || this.orders.length === 0) return;
    const order = this.orders.find(o => o.id === id);
    if (order) {
      // Si el pedido está oculto por filtros actuales, restablecerlos
      if (this.filtroEstado !== 'TODOS' && order.status !== this.filtroEstado) {
        this.filtroEstado = 'TODOS';
      }
      if (this.filtroDeliveryType !== 'TODOS' && order.deliveryType !== this.filtroDeliveryType) {
        this.filtroDeliveryType = 'TODOS';
      }
      this.terminoBusqueda = '';
      this.aplicarFiltro();

      // Ajustar la página si el pedido no está en la página 1
      const orderIndex = this.filteredOrders.findIndex(o => o.id === id);
      if (orderIndex !== -1) {
        this.currentPage = Math.floor(orderIndex / this.itemsPerPage) + 1;
        this.aplicarPaginacion();
      }

      this.verDetalles(order);
      this.orderOpened.emit(id);
    }
  }

  verDetalles(order: OrderCompleteDTO): void {
    this.selectedOrder = order;
    this.showModal = true;
  }

  cerrarModal(): void {
    this.showModal = false;
    this.selectedOrder = null;
    this.orderIdToOpen = null;
  }

  cambiarEstado(order: OrderCompleteDTO, nuevoEstado: string, estadoAnterior: OrderCompleteDTO['status']): void {
    if (!order.id || this.updatingOrderId === order.id) return;

    this.updatingOrderId = order.id;

    this.orderService.cambiarEstado(order.id, nuevoEstado).subscribe({
      next: (orderActualizado) => {
        order.status = orderActualizado.status;
        this.updatingOrderId = null;
        this.recalcularKpis();
        this.mostrarMensaje(
          nuevoEstado === 'CANCELLED'
            ? 'Pedido cancelado: el inventario y mermas fueron ajustados automáticamente'
            : 'Estado actualizado correctamente'
        );
      },
      error: (err) => {
        console.error('Error al actualizar estado:', err);
        // Rollback status
        order.status = estadoAnterior;
        this.updatingOrderId = null;
        this.mostrarMensaje(err?.error?.message || 'Error al actualizar el estado del pedido', true);
      }
    });
  }

  esPickup(order: OrderCompleteDTO): boolean {
    return order.deliveryType === 'PICKUP';
  }

  esDelivery(order: OrderCompleteDTO): boolean {
    return order.deliveryType === 'DELIVERY';
  }

  getEstadoTexto(status: string): string {
    const textos: Record<string, string> = {
      'PENDING': 'Pendiente',
      'CONFIRMED': 'Confirmado',
      'PREPARING': 'En Preparación',
      'OUT_FOR_DELIVERY': 'En Camino',
      'DELIVERED': 'Entregado',
      'CANCELLED': 'Cancelado'
    };
    return textos[status] || status;
  }

  mostrarMensaje(msg: string, esError = false): void {
    this.mensaje = msg;
    this.error = esError;
    setTimeout(() => (this.mensaje = ''), 3500);
  }

  trackByOrderId(index: number, item: OrderCompleteDTO): number {
    return item.id ?? index;
  }
}
