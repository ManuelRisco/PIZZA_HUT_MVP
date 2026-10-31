import { Component, OnInit, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs/operators';
import { Order } from '../../services/order.service';
import { OrderCompleteDTO, ReviewDTO } from '../../models/admin.interface';
import { AuthService } from '../../services/auth.service';
import { Review } from '../../services/review.service';
import { PaginationComponent } from '../pagination/pagination';

@Component({
  selector: 'app-cliente-pedidos',
  standalone: true,
  imports: [CommonModule, FormsModule, PaginationComponent],
  templateUrl: './cliente-pedidos.html',
  styleUrls: ['./cliente-pedidos.css']
})
export class ClientePedidosComponent implements OnInit {
  orders: OrderCompleteDTO[] = [];
  filteredOrders: OrderCompleteDTO[] = [];
  ordersPaginados: OrderCompleteDTO[] = [];
  currentPage: number = 1;
  itemsPerPage: number = 10;

  selectedOrder: OrderCompleteDTO | null = null;
  selectedOrderItems: any[] = [];
  loading: boolean = true;
  loadingItems: boolean = false;
  guardandoReview: boolean = false;
  loadError: string = '';

  mensaje: string = '';
  error: boolean = false;
  showModal: boolean = false;
  showReviewModal: boolean = false;

  busqueda: string = '';
  filtroDeliveryType: string = 'TODOS';
  filtroEstado: string = 'TODOS';

  estados = ['PENDING', 'CONFIRMED', 'PREPARING', 'OUT_FOR_DELIVERY', 'DELIVERED', 'CANCELLED'];
  readonly stars = [1, 2, 3, 4, 5];

  nuevaReview: ReviewDTO = {
    orderId: 0,
    userId: 0,
    rating: 5,
    comment: ''
  };
  orderParaReview: OrderCompleteDTO | null = null;
  pedidosConReview: Set<number> = new Set();
  reviewsDelUsuario: Map<number, ReviewDTO> = new Map();
  editandoReview: boolean = false;

  constructor(
    private readonly orderService: Order,
    private readonly authService: AuthService,
    private readonly reviewService: Review
  ) {}

  ngOnInit(): void {
    this.cargarTodo();
  }

  cargarTodo(): void {
    this.cargarPedidos();
    this.cargarReviewsDelUsuario();
  }

  cargarReviewsDelUsuario(): void {
    const currentUser = this.authService.getCurrentUser();
    if (currentUser?.id) {
      this.reviewService.obtenerPorUsuario(currentUser.id).subscribe({
        next: (reviews) => {
          this.pedidosConReview = new Set(reviews.map(r => r.orderId));
          this.reviewsDelUsuario = new Map(reviews.map(r => [r.orderId, r]));
        },
        error: () => {}
      });
    }
  }

  cargarPedidos(): void {
    this.loading = true;
    this.loadError = '';
    const currentUser = this.authService.getCurrentUser();

    if (!currentUser?.id) {
      this.loadError = 'Usuario no autenticado. Por favor inicia sesión nuevamente.';
      this.loading = false;
      return;
    }

    this.orderService.obtenerTodosCompletos()
      .pipe(finalize(() => { this.loading = false; }))
      .subscribe({
        next: (data) => {
          this.orders = (data || [])
            .filter(order => order.userId === currentUser.id)
            .toSorted((a, b) => {
              const tB = b.createdAt ? new Date(b.createdAt).getTime() : 0;
              const tA = a.createdAt ? new Date(a.createdAt).getTime() : 0;
              return tB - tA;
            });
          this.aplicarFiltro();
        },
        error: (err) => {
          console.error('Error al cargar pedidos:', err);
          this.loadError = err?.status === 0
            ? 'No se pudo conectar con el servidor (Backend fuera de línea). Verifica tu conexión e inténtalo de nuevo.'
            : (err?.error?.message || 'Error al cargar el historial de pedidos.');
        }
      });
  }

  aplicarFiltro(): void {
    const term = this.busqueda.trim().toLowerCase();
    let resultado = this.orders;

    if (this.filtroEstado !== 'TODOS') {
      resultado = resultado.filter(o => o.status === this.filtroEstado);
    }

    if (this.filtroDeliveryType !== 'TODOS') {
      resultado = resultado.filter(o => o.deliveryType === this.filtroDeliveryType);
    }

    if (term) {
      resultado = resultado.filter(o => {
        const idStr = `#${o.id} #ph-${o.id?.toString().padStart(4, '0')}`.toLowerCase();
        const addr = `${o.addressLine1 || ''} ${o.addressDistrict || ''}`.toLowerCase();
        return idStr.includes(term) || addr.includes(term);
      });
    }

    this.filteredOrders = resultado;
    this.aplicarPaginacion();
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

  verDetalles(order: OrderCompleteDTO): void {
    this.selectedOrder = order;
    this.selectedOrderItems = order.items || [];
    this.showModal = true;
    this.enfocarPrimerElementoModal();

    if (order.id) {
      this.loadingItems = true;
      this.orderService.obtenerItemsCompletos(order.id)
        .pipe(finalize(() => { this.loadingItems = false; }))
        .subscribe({
          next: (items) => {
            this.selectedOrderItems = items;
          },
          error: (err) => {
            console.error('Error al cargar items:', err);
          }
        });
    }
  }

  cerrarModal(): void {
    this.showModal = false;
    this.selectedOrder = null;
    this.selectedOrderItems = [];
    this.devolverFocoElementoAnterior();
  }

  esPickup(order: OrderCompleteDTO): boolean {
    return order.deliveryType === 'PICKUP';
  }

  esDelivery(order: OrderCompleteDTO): boolean {
    return order.deliveryType === 'DELIVERY';
  }

  getEstadoTexto(status: string): string {
    const statusLabels: Record<string, string> = {
      'PENDING': 'Pendiente',
      'CONFIRMED': 'Confirmado',
      'PREPARING': 'Preparando',
      'OUT_FOR_DELIVERY': 'En camino',
      'DELIVERED': 'Entregado',
      'CANCELLED': 'Cancelado'
    };
    return statusLabels[status] || status;
  }

  getDeliveryTypeLabel(type: string): string {
    return type === 'PICKUP' ? 'Recojo en tienda' : 'Delivery';
  }

  mostrarMensaje(mensaje: string, esError: boolean = false): void {
    this.mensaje = mensaje;
    this.error = esError;
    setTimeout(() => {
      this.mensaje = '';
    }, 4500);
  }

  abrirModalReview(order: OrderCompleteDTO): void {
    const currentUser = this.authService.getCurrentUser();
    if (!currentUser?.id) {
      this.mostrarMensaje('Error: Usuario no autenticado', true);
      return;
    }

    this.orderParaReview = order;
    this.editandoReview = false;
    this.nuevaReview = {
      orderId: order.id || 0,
      userId: currentUser.id,
      rating: 5,
      comment: ''
    };
    this.showReviewModal = true;
    this.enfocarPrimerElementoModal();
  }

  abrirModalEditarReview(order: OrderCompleteDTO): void {
    const currentUser = this.authService.getCurrentUser();
    if (!currentUser?.id) {
      this.mostrarMensaje('Error: Usuario no autenticado', true);
      return;
    }

    const reviewExistente = this.reviewsDelUsuario.get(order.id || 0);
    if (!reviewExistente) {
      this.mostrarMensaje('No se encontró la reseña', true);
      return;
    }

    this.orderParaReview = order;
    this.editandoReview = true;
    this.nuevaReview = {
      id: reviewExistente.id,
      orderId: reviewExistente.orderId,
      userId: reviewExistente.userId,
      rating: reviewExistente.rating,
      comment: reviewExistente.comment
    };
    this.showReviewModal = true;
    this.enfocarPrimerElementoModal();
  }

  cerrarModalReview(): void {
    this.showReviewModal = false;
    this.orderParaReview = null;
    this.editandoReview = false;
    this.nuevaReview = {
      orderId: 0,
      userId: 0,
      rating: 5,
      comment: ''
    };
    this.devolverFocoElementoAnterior();
  }

  guardarReview(): void {
    if (this.guardandoReview) return;
    if (!this.nuevaReview.comment || this.nuevaReview.comment.trim() === '') {
      this.mostrarMensaje('Por favor escribe un comentario', true);
      return;
    }

    if (this.editandoReview && this.nuevaReview.id) {
      this.actualizarReview();
    } else {
      this.crearNuevaReview();
    }
  }

  private actualizarReview(): void {
    this.guardandoReview = true;
    this.reviewService.actualizar(this.nuevaReview.id as number, this.nuevaReview)
      .pipe(finalize(() => { this.guardandoReview = false; }))
      .subscribe({
        next: () => {
          this.mostrarMensaje('¡Reseña actualizada exitosamente!');
          this.reviewsDelUsuario.set(this.nuevaReview.orderId, { ...this.nuevaReview });
          this.cerrarModalReview();
        },
        error: (err) => {
          console.error('Error al actualizar reseña:', err);
          this.mostrarMensaje(err?.error?.message || 'Error al actualizar la reseña', true);
        }
      });
  }

  private crearNuevaReview(): void {
    this.guardandoReview = true;
    this.reviewService.crear(this.nuevaReview)
      .pipe(finalize(() => { this.guardandoReview = false; }))
      .subscribe({
        next: (reviewCreada) => {
          this.mostrarMensaje('¡Reseña enviada exitosamente!');
          this.pedidosConReview.add(this.nuevaReview.orderId);
          this.reviewsDelUsuario.set(this.nuevaReview.orderId, reviewCreada);
          this.cerrarModalReview();
        },
        error: (err) => {
          console.error('Error al guardar reseña:', err);
          this.mostrarMensaje(err?.error?.message || 'Error al enviar la reseña', true);
        }
      });
  }

  puedeDejarReview(order: OrderCompleteDTO): boolean {
    return order.status === 'DELIVERED' && !this.yaHaResenadoPedido(order.id || 0);
  }

  yaHaResenadoPedido(orderId: number): boolean {
    return this.pedidosConReview.has(orderId);
  }

  eliminarReview(order: OrderCompleteDTO): void {
    if (this.guardandoReview) return;
    const reviewExistente = this.reviewsDelUsuario.get(order.id || 0);
    if (!reviewExistente?.id) {
      this.mostrarMensaje('No se encontró la reseña', true);
      return;
    }

    if (confirm('¿Estás seguro de que deseas eliminar esta reseña?')) {
      this.guardandoReview = true;
      this.reviewService.eliminar(reviewExistente.id)
        .pipe(finalize(() => { this.guardandoReview = false; }))
        .subscribe({
          next: () => {
            this.mostrarMensaje('Reseña eliminada exitosamente');
            this.pedidosConReview.delete(order.id || 0);
            this.reviewsDelUsuario.delete(order.id || 0);
            this.cerrarModalReview();
          },
          error: (err) => {
            console.error('Error al eliminar reseña:', err);
            this.mostrarMensaje(err?.error?.message || 'Error al eliminar la reseña', true);
          }
        });
    }
  }

  trackByOrder(index: number, order: OrderCompleteDTO): number {
    return order.id ?? index;
  }

  trackByItem(index: number, item: any): number {
    return item.id ?? index;
  }

  private elementoPrevioAlModal: HTMLElement | null = null;

  private enfocarPrimerElementoModal(): void {
    if (typeof document !== 'undefined') {
      this.elementoPrevioAlModal = document.activeElement as HTMLElement;
    }
  }

  private devolverFocoElementoAnterior(): void {
    if (this.elementoPrevioAlModal && typeof document !== 'undefined') {
      setTimeout(() => {
        this.elementoPrevioAlModal?.focus();
        this.elementoPrevioAlModal = null;
      }, 80);
    }
  }

  @HostListener('document:keydown', ['$event'])
  manejarTecladoModal(event: KeyboardEvent): void {
    if (!this.showModal && !this.showReviewModal) return;
    if (event.key === 'Escape') {
      if (this.showReviewModal) this.cerrarModalReview();
      else if (this.showModal) this.cerrarModal();
    }
  }
}
