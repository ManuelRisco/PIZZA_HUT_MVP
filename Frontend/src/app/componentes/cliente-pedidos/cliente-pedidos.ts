import { Component, OnInit, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Order } from '../../services/order.service';
import { OrderDTO, OrderCompleteDTO } from '../../models/admin.interface';
import { AuthService } from '../../services/auth.service';
import { Review } from '../../services/review.service';
import { ReviewDTO } from '../../models/admin.interface';
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
  mensaje: string = '';
  error: boolean = false;
  showModal: boolean = false;
  showReviewModal: boolean = false;
  
  filtroDeliveryType: string = 'TODOS';
  filtroEstado: string = 'TODOS';
  
  estados = ['PENDING', 'CONFIRMED', 'PREPARING', 'OUT_FOR_DELIVERY', 'DELIVERED', 'CANCELLED'];

  // Para el modal de reseÃ±a
  nuevaReview: ReviewDTO = {
    orderId: 0,
    userId: 0,
    rating: 5,
    comment: ''
  };
  orderParaReview: OrderCompleteDTO | null = null;
  pedidosConReview: Set<number> = new Set();
  reviewsDelUsuario: Map<number, ReviewDTO> = new Map(); // Mapeo orderId -> ReviewDTO
  editandoReview: boolean = false;

  constructor(
    private readonly orderService: Order,
    private readonly authService: AuthService,
    private readonly reviewService: Review
  ) {}

  ngOnInit(): void {
    this.cargarPedidos();
    this.cargarReviewsDelUsuario();
  }

  cargarReviewsDelUsuario(): void {
    const currentUser = this.authService.getCurrentUser();
    if (currentUser?.id) {
      this.reviewService.obtenerPorUsuario(currentUser.id).subscribe({
        next: (reviews) => {
          // Guardar los IDs de pedidos que ya tienen reseÃ±a
          this.pedidosConReview = new Set(reviews.map(r => r.orderId));
          // Guardar las reseÃ±as completas en un mapa
          this.reviewsDelUsuario = new Map(reviews.map(r => [r.orderId, r]));
        },
        error: (err) => {
          console.error('Error al cargar reseÃ±as:', err);
        }
      });
    }
  }

  cargarPedidos(): void {
    this.loading = true;
    const currentUser = this.authService.getCurrentUser();
    
    if (!currentUser?.id) {
      this.mostrarMensaje('Error: Usuario no autenticado', true);
      this.loading = false;
      return;
    }

    // Obtener todos los pedidos completos y filtrar los del usuario
    this.orderService.obtenerTodosCompletos().subscribe({
      next: (data) => {
        // Filtrar solo los pedidos del usuario actual
        this.orders = data
          .filter(order => order.userId === currentUser.id)
          .toSorted((a, b) => {
            return new Date(b.createdAt!).getTime() - new Date(a.createdAt!).getTime();
          });
        this.aplicarFiltro();
        this.loading = false;
      },
      error: (error) => {
        console.error('Error al cargar pedidos:', error);
        console.error('Error status:', error.status);
        console.error('Error message:', error.message);
        this.mostrarMensaje('Error al cargar los pedidos', true);
        this.loading = false;
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

  verDetalles(order: OrderCompleteDTO): void {
    this.selectedOrder = order;
    
    // Cargar items del pedido
    if (order.id) {
      this.orderService.obtenerItemsCompletos(order.id).subscribe({
        next: (items) => {
          this.selectedOrderItems = items;
          this.showModal = true;
          this.enfocarPrimerElementoModal();
        },
        error: (err) => {
          console.error('Error al cargar items:', err);
          this.showModal = true; // Mostrar modal aunque no se carguen los items
          this.enfocarPrimerElementoModal();
        }
      });
    } else {
      this.showModal = true;
      this.enfocarPrimerElementoModal();
    }
  }

  cerrarModal(): void {
    this.showModal = false;
    this.selectedOrder = null;
    this.selectedOrderItems = [];
    this.devolverFocoElementoAnterior();
  }

  // Los clientes NO pueden cambiar el estado de sus pedidos
  cambiarEstado(order: OrderCompleteDTO, nuevoEstado: string): void {
    // Este mÃ©todo estÃ¡ aquÃ­ para compatibilidad con el template pero no hace nada
    // Solo los administradores pueden cambiar estados
  }

  esPickup(order: OrderCompleteDTO): boolean {
    return order.deliveryType === 'PICKUP';
  }

  esDelivery(order: OrderCompleteDTO): boolean {
    return order.deliveryType === 'DELIVERY';
  }

  getBadgeClass(status: string): string {
    const statusClasses: { [key: string]: string } = {
      'PENDING': 'bg-warning text-dark',
      'CONFIRMED': 'bg-info',
      'PREPARING': 'bg-primary',
      'OUT_FOR_DELIVERY': 'bg-secondary',
      'DELIVERED': 'bg-success',
      'CANCELLED': 'bg-danger'
    };
    return statusClasses[status] || 'bg-secondary';
  }

  getStatusBadgeClass(status: string): string {
    return this.getBadgeClass(status);
  }

  getEstadoTexto(status: string): string {
    const statusLabels: { [key: string]: string } = {
      'PENDING': 'Pendiente',
      'CONFIRMED': 'Confirmado',
      'PREPARING': 'Preparando',
      'OUT_FOR_DELIVERY': 'En camino',
      'DELIVERED': 'Entregado',
      'CANCELLED': 'Cancelado'
    };
    return statusLabels[status] || status;
  }

  getStatusLabel(status: string): string {
    return this.getEstadoTexto(status);
  }

  getDeliveryTypeLabel(type: string): string {
    return type === 'PICKUP' ? 'ðŸª Recojo en tienda' : 'ðŸ›µ Delivery';
  }

  getDeliveryTypeBadge(type: string): string {
    return type === 'PICKUP' ? 'badge bg-secondary' : 'badge bg-info';
  }

  mostrarMensaje(mensaje: string, esError: boolean = false): void {
    this.mensaje = mensaje;
    this.error = esError;
    setTimeout(() => {
      this.mensaje = '';
    }, 5000);
  }

  formatDate(date: string): string {
    return new Date(date).toLocaleString('es-PE', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  // MÃ©todos para reseÃ±as
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
      this.mostrarMensaje('No se encontrÃ³ la reseÃ±a', true);
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
    this.reviewService.actualizar(this.nuevaReview.id as number, this.nuevaReview).subscribe({
      next: () => {
        this.mostrarMensaje('Â¡ReseÃ±a actualizada exitosamente!');
        this.reviewsDelUsuario.set(this.nuevaReview.orderId, this.nuevaReview);
        this.cerrarModalReview();
      },
      error: (err) => {
        console.error('Error al actualizar reseÃ±a:', err);
        this.mostrarMensaje('Error al actualizar la reseÃ±a', true);
      }
    });
  }

  private crearNuevaReview(): void {
    this.reviewService.crear(this.nuevaReview).subscribe({
      next: (reviewCreada) => {
        this.mostrarMensaje('Â¡ReseÃ±a enviada exitosamente!');
        this.pedidosConReview.add(this.nuevaReview.orderId);
        this.reviewsDelUsuario.set(this.nuevaReview.orderId, reviewCreada);
        this.cerrarModalReview();
      },
      error: (err) => {
        console.error('Error al guardar reseÃ±a:', err);
        this.mostrarMensaje('Error al enviar la reseÃ±a', true);
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
    const reviewExistente = this.reviewsDelUsuario.get(order.id || 0);
    if (!reviewExistente?.id) {
      this.mostrarMensaje('No se encontrÃ³ la reseÃ±a', true);
      return;
    }

    if (confirm('Â¿EstÃ¡s seguro de que deseas eliminar esta reseÃ±a?')) {
      this.reviewService.eliminar(reviewExistente.id).subscribe({
        next: () => {
          this.mostrarMensaje('ReseÃ±a eliminada exitosamente');
          this.pedidosConReview.delete(order.id || 0);
          this.reviewsDelUsuario.delete(order.id || 0);
        },
        error: (err) => {
          console.error('Error al eliminar reseÃ±a:', err);
          this.mostrarMensaje('Error al eliminar la reseÃ±a', true);
        }
      });
    }
  }

  // ACCESIBILIDAD: Control de Foco (Focus Trap)
  private elementoPrevioAlModal: HTMLElement | null = null;

  private enfocarPrimerElementoModal(): void {
    if (typeof document !== 'undefined') {
      this.elementoPrevioAlModal = document.activeElement as HTMLElement;
      setTimeout(() => {
        const modales = document.querySelectorAll('.modal.show');
        if (modales.length > 0) {
          const modalActivo = modales[modales.length - 1] as HTMLElement;
          modalActivo.focus();
        }
      }, 100);
    }
  }

  private devolverFocoElementoAnterior(): void {
    if (this.elementoPrevioAlModal && typeof document !== 'undefined') {
      setTimeout(() => {
        this.elementoPrevioAlModal?.focus();
        this.elementoPrevioAlModal = null;
      }, 100);
    }
  }

  @HostListener('document:keydown', ['$event'])
  manejarTecladoModal(event: KeyboardEvent): void {
    if (!this.showModal && !this.showReviewModal) {
      return;
    }

    if (event.key === 'Escape') {
      this.cerrarModal();
      this.cerrarModalReview();
      return;
    }

    if (event.key === 'Tab') {
      this.manejarTabModal(event);
    }
  }

  private manejarTabModal(event: KeyboardEvent): void {
    const modales = document.querySelectorAll('.modal.show');
    if (modales.length === 0) return;
    
    const modalActivo = modales[modales.length - 1] as HTMLElement;
    const focusableSelectors = 'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])';
    const focusableElements = Array.from(modalActivo.querySelectorAll(focusableSelectors)) as HTMLElement[];
    
    if (focusableElements.length === 0) return;

    const firstElement = focusableElements[0];
    const lastElement = focusableElements[focusableElements.length - 1];

    if (!modalActivo.contains(document.activeElement)) {
      firstElement.focus();
      event.preventDefault();
      return;
    }

    if (event.shiftKey) { // Shift + Tab
      if (document.activeElement === firstElement) {
        lastElement.focus();
        event.preventDefault();
      }
    } else if (document.activeElement === lastElement) { // Solo Tab
      firstElement.focus();
      event.preventDefault();
    }
  }
}


