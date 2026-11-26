import { Component, OnInit, OnChanges, SimpleChanges, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { Order } from '../../services/order.service';
import { SunatService } from '../../services/sunat.service';
import { StoreSettingsService } from '../../services/store-settings.service';
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
  emitiendoBoletaOrderId: number | null = null;

  deliveryFeeActual = 5.0;
  deliveryFeeInput = 5.0;
  guardandoDeliveryFee = false;

  kpiTotalOrders = 0;
  kpiPendientes = 0;
  kpiEntregados = 0;
  kpiTotalVentas = 0;

  estados = ['PENDING', 'CONFIRMED', 'PREPARING', 'OUT_FOR_DELIVERY', 'DELIVERED', 'CANCELLED'];

  constructor(
    private readonly orderService: Order,
    private readonly sunatService: SunatService,
    private readonly storeSettingsService: StoreSettingsService,
    private readonly route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    const queryId = this.route.snapshot.queryParams['id'] || this.route.snapshot.queryParams['orderId'];
    if (queryId && !isNaN(Number(queryId))) {
      this.orderIdToOpen = Number(queryId);
    }
    this.storeSettingsService.loadDeliveryFee().subscribe((cfg) => {
      const fee = Number(cfg?.deliveryFee);
      if (!Number.isNaN(fee) && fee >= 0) {
        this.deliveryFeeActual = fee;
        this.deliveryFeeInput = fee;
      }
    });
    this.cargarOrders();
  }

  guardarDeliveryFee(): void {
    const valor = Number(this.deliveryFeeInput);
    if (Number.isNaN(valor) || valor < 0 || valor > 500) {
      this.error = true;
      this.mensaje = 'Ingresa un costo de delivery válido entre S/ 0.00 y S/ 500.00';
      return;
    }
    const normalizado = Number(valor.toFixed(2));
    this.guardandoDeliveryFee = true;
    this.storeSettingsService.updateDeliveryFee(normalizado).subscribe({
      next: (res) => {
        const guardado = Number(res?.deliveryFee ?? normalizado);
        this.deliveryFeeActual = guardado;
        this.deliveryFeeInput = guardado;
        this.guardandoDeliveryFee = false;
        this.error = false;
        this.mensaje = guardado === 0
          ? 'Delivery actualizado a gratis (S/ 0.00)'
          : `Costo de delivery actualizado a S/ ${guardado.toFixed(2)}`;
      },
      error: (err) => {
        this.guardandoDeliveryFee = false;
        this.error = true;
        this.mensaje = err?.error?.message || 'No se pudo actualizar el costo de delivery';
      }
    });
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
      if (this.filtroEstado !== 'TODOS' && order.status !== this.filtroEstado) {
        this.filtroEstado = 'TODOS';
      }
      if (this.filtroDeliveryType !== 'TODOS' && order.deliveryType !== this.filtroDeliveryType) {
        this.filtroDeliveryType = 'TODOS';
      }
      this.terminoBusqueda = '';
      this.aplicarFiltro();

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

  tieneBoleta(order: OrderCompleteDTO): boolean {
    return Boolean(order?.boletaComprobante || order?.boletaNumero || order?.boletaPdfUrl);
  }

  verBoletaPdf(order: OrderCompleteDTO): void {
    if (!order?.id) return;
    this.sunatService.abrirBoletaPdf(order.id, order.boletaPdfUrl);
  }

  emitirBoletaAdmin(order: OrderCompleteDTO): void {
    if (!order?.id || this.emitiendoBoletaOrderId === order.id) return;
    let dni = (order.boletaClienteDni || order.userDni || '').trim();
    if (!/^\d{8}$/.test(dni)) {
      const promptDni = window.prompt('Ingresa el DNI de 8 dígitos del cliente para emitir la Boleta Electrónica SUNAT:', '');
      if (!promptDni) return;
      dni = promptDni.trim();
      if (!/^\d{8}$/.test(dni)) {
        this.mostrarMensaje('El DNI debe contener exactamente 8 dígitos numéricos', true);
        return;
      }
    }
    this.emitiendoBoletaOrderId = order.id;
    this.sunatService.emitirBoletaPedido(order.id, { dni, nombre: order.userName, enviarCorreo: true }).subscribe({
      next: (res) => {
        this.emitiendoBoletaOrderId = null;
        order.wantsBoleta = true;
        order.boletaSerie = res.serie;
        order.boletaNumero = res.numero;
        order.boletaComprobante = res.comprobante;
        order.boletaClienteDni = res.clienteDni;
        order.boletaClienteNombre = res.clienteNombre;
        order.boletaPdfUrl = res.pdfUrl;
        order.boletaXmlUrl = res.xmlUrl;
        order.boletaHash = res.hash;
        order.boletaEstado = res.estado;
        order.boletaTotalGravada = res.totalGravada;
        order.boletaTotalIgv = res.totalIgv;
        this.mostrarMensaje(`Boleta Electrónica ${res.comprobante} emitida y enviada al cliente por correo`);
      },
      error: (err) => {
        this.emitiendoBoletaOrderId = null;
        this.mostrarMensaje(err?.error?.message || 'Error al emitir la Boleta Electrónica', true);
      }
    });
  }
}
