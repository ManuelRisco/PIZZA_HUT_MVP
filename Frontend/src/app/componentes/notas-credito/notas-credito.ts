import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Order } from '../../services/order.service';
import { SunatService } from '../../services/sunat.service';
import { OrderCompleteDTO } from '../../models/admin.interface';
import { PaginationComponent } from '../pagination/pagination';
import { UiSkeletonComponent } from '../ui-skeleton/ui-skeleton';

@Component({
  selector: 'app-notas-credito',
  standalone: true,
  imports: [CommonModule, FormsModule, PaginationComponent, UiSkeletonComponent],
  templateUrl: './notas-credito.html',
  styleUrl: './notas-credito.css'
})
export class NotasCreditoComponent implements OnInit {
  orders: OrderCompleteDTO[] = [];
  activeTab: 'EMITIDAS' | 'ANULABLES' = 'EMITIDAS';
  terminoBusqueda = '';
  loading = true;
  loadError = '';
  mensaje = '';
  error = false;

  currentPage = 1;
  itemsPerPage = 10;
  pageSizeOptions = [10, 25, 50];

  filteredList: OrderCompleteDTO[] = [];
  paginatedList: OrderCompleteDTO[] = [];

  kpiEmitidas = 0;
  kpiMontoAnulado = 0;
  kpiBoletasAnulables = 0;
  kpiCanceladosSinNc = 0;

  showEmitModal = false;
  orderParaAnular: OrderCompleteDTO | null = null;
  motivoAnulacion = '';
  enviarCorreoCliente = true;
  emitiendo = false;

  constructor(
    private readonly orderService: Order,
    private readonly sunatService: SunatService
  ) {}

  ngOnInit(): void {
    this.cargarDatos();
  }

  cargarDatos(): void {
    this.loading = true;
    this.loadError = '';
    this.orderService.obtenerTodosCompletos().subscribe({
      next: (data) => {
        this.orders = (data || []).toSorted((a, b) => {
          const tB = new Date(b.ncEmitidaAt || b.createdAt || 0).getTime();
          const tA = new Date(a.ncEmitidaAt || a.createdAt || 0).getTime();
          return tB - tA;
        });
        this.recalcularKpis();
        this.aplicarFiltro();
        this.loading = false;
      },
      error: (err) => {
        this.loadError = err?.status === 0
          ? 'No se pudo conectar con el servidor (Backend fuera de línea).'
          : (err?.error?.message || 'Error al cargar comprobantes SUNAT.');
        this.loading = false;
      }
    });
  }

  tieneBoleta(order: OrderCompleteDTO): boolean {
    return Boolean(order?.boletaComprobante || order?.boletaNumero || order?.boletaPdfUrl);
  }

  esFactura(order: OrderCompleteDTO): boolean {
    return Boolean(
      order?.boletaTipoComprobante === 1 ||
      (order?.boletaSerie || '').toUpperCase().startsWith('F') ||
      (order?.boletaComprobante || '').toUpperCase().startsWith('F')
    );
  }

  tieneNotaCredito(order: OrderCompleteDTO): boolean {
    return Boolean(order?.ncComprobante || order?.ncNumero || order?.ncPdfUrl);
  }

  private recalcularKpis(): void {
    let emitidas = 0;
    let montoAnulado = 0;
    let anulables = 0;
    let canceladosSinNc = 0;

    for (const o of this.orders) {
      if (this.tieneNotaCredito(o)) {
        emitidas++;
        montoAnulado += Number(o.total ?? 0);
      } else if (this.tieneBoleta(o)) {
        anulables++;
        if (o.status === 'CANCELLED') {
          canceladosSinNc++;
        }
      }
    }

    this.kpiEmitidas = emitidas;
    this.kpiMontoAnulado = montoAnulado;
    this.kpiBoletasAnulables = anulables;
    this.kpiCanceladosSinNc = canceladosSinNc;
  }

  cambiarTab(tab: 'EMITIDAS' | 'ANULABLES'): void {
    this.activeTab = tab;
    this.currentPage = 1;
    this.aplicarFiltro();
  }

  onBusquedaChange(): void {
    this.currentPage = 1;
    this.aplicarFiltro();
  }

  aplicarFiltro(): void {
    let base: OrderCompleteDTO[];
    if (this.activeTab === 'EMITIDAS') {
      base = this.orders.filter(o => this.tieneNotaCredito(o));
    } else {
      base = this.orders
        .filter(o => this.tieneBoleta(o) && !this.tieneNotaCredito(o))
        .toSorted((a, b) => {
          const aCancelled = a.status === 'CANCELLED' ? 1 : 0;
          const bCancelled = b.status === 'CANCELLED' ? 1 : 0;
          if (bCancelled !== aCancelled) return bCancelled - aCancelled;
          return new Date(b.createdAt || 0).getTime() - new Date(a.createdAt || 0).getTime();
        });
    }

    const term = this.terminoBusqueda.trim().toLowerCase();
    if (term) {
      base = base.filter(o =>
        String(o.id || '').includes(term) ||
        (o.ncComprobante || '').toLowerCase().includes(term) ||
        (o.boletaComprobante || '').toLowerCase().includes(term) ||
        (o.userName || '').toLowerCase().includes(term) ||
        (o.boletaClienteNombre || '').toLowerCase().includes(term) ||
        (o.boletaClienteDni || o.userDni || '').toLowerCase().includes(term) ||
        (o.ncMotivo || '').toLowerCase().includes(term)
      );
    }

    this.filteredList = base;
    this.aplicarPaginacion();
  }

  aplicarPaginacion(): void {
    const totalPages = Math.ceil(this.filteredList.length / this.itemsPerPage);
    if (this.currentPage > totalPages && totalPages > 0) {
      this.currentPage = totalPages;
    } else if (this.currentPage < 1) {
      this.currentPage = 1;
    }
    const start = (this.currentPage - 1) * this.itemsPerPage;
    this.paginatedList = this.filteredList.slice(start, start + this.itemsPerPage);
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

  abrirModalEmitir(order: OrderCompleteDTO): void {
    this.orderParaAnular = order;
    this.motivoAnulacion = `Anulación de la operación - Pedido #${order.id} (Boleta ${order.boletaComprobante || ''})`;
    this.enviarCorreoCliente = true;
    this.showEmitModal = true;
  }

  cerrarModalEmitir(): void {
    if (this.emitiendo) return;
    this.showEmitModal = false;
    this.orderParaAnular = null;
    this.motivoAnulacion = '';
  }

  confirmarEmisionNc(): void {
    if (!this.orderParaAnular?.id || this.emitiendo) return;
    const motivoLimpio = this.motivoAnulacion.trim() || `Anulación de pedido #${this.orderParaAnular.id}`;
    this.emitiendo = true;

    this.sunatService.emitirNotaCreditoPedido(this.orderParaAnular.id, {
      motivo: motivoLimpio,
      enviarCorreo: this.enviarCorreoCliente
    }).subscribe({
      next: (res) => {
        const order = this.orderParaAnular!;
        order.status = 'CANCELLED';
        order.boletaEstado = 'ANULADA_NC';
        order.ncSerie = res.serie;
        order.ncNumero = res.numero;
        order.ncComprobante = res.comprobante;
        order.ncTipoNota = res.tipoNota;
        order.ncMotivo = res.motivo;
        order.ncPdfUrl = res.pdfUrl;
        order.ncXmlUrl = res.xmlUrl;
        order.ncCdrUrl = res.cdrUrl;
        order.ncEnlace = res.enlace;
        order.ncHash = res.hash;
        order.ncQr = res.qr;
        order.ncEstado = res.estado;
        order.ncEmitidaAt = res.emitidaAt || new Date().toISOString();

        this.emitiendo = false;
        this.showEmitModal = false;
        this.orderParaAnular = null;
        this.recalcularKpis();
        this.activeTab = 'EMITIDAS';
        this.currentPage = 1;
        this.aplicarFiltro();
        this.mostrarMensaje(
          `Nota de Crédito ${res.comprobante} emitida correctamente anulando la Boleta ${res.boletaModificadaComprobante}.`
        );
      },
      error: (err) => {
        this.emitiendo = false;
        this.mostrarMensaje(
          err?.error?.message || 'Error al emitir la Nota de Crédito Electrónica ante SUNAT.',
          true
        );
      }
    });
  }

  verNotaCreditoPdf(order: OrderCompleteDTO): void {
    if (!order?.id) return;
    this.sunatService.abrirNotaCreditoPdf(order.id, order.ncPdfUrl);
  }

  verBoletaPdf(order: OrderCompleteDTO): void {
    if (!order?.id) return;
    this.sunatService.abrirBoletaPdf(order.id, order.boletaPdfUrl);
  }

  getEstadoTexto(status: string): string {
    const textos: Record<string, string> = {
      PENDING: 'Pendiente',
      CONFIRMED: 'Confirmado',
      PREPARING: 'En Preparación',
      OUT_FOR_DELIVERY: 'En Camino',
      DELIVERED: 'Entregado',
      CANCELLED: 'Cancelado'
    };
    return textos[status] || status;
  }

  mostrarMensaje(msg: string, esError = false): void {
    this.mensaje = msg;
    this.error = esError;
    setTimeout(() => (this.mensaje = ''), 5000);
  }

  trackByOrderId(index: number, item: OrderCompleteDTO): number {
    return item.id ?? index;
  }
}
