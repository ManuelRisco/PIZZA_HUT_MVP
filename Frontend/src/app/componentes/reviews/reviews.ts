import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Review } from '../../services/review.service';
import { ReviewDTO } from '../../models/admin.interface';
import { AuthService } from '../../services/auth.service';
import { PaginationComponent } from '../pagination/pagination';

@Component({
  selector: 'app-reviews',
  standalone: true,
  imports: [CommonModule, FormsModule, PaginationComponent],
  templateUrl: './reviews.html',
  styleUrl: './reviews.css'
})
export class Reviews implements OnInit {
  todasLasReviews: ReviewDTO[] = [];
  reviews: ReviewDTO[] = [];
  reviewsPaginados: ReviewDTO[] = [];
  mensaje = '';
  error = false;
  cargando = true;
  isAdmin = false;
  loadError = '';
  procesandoId: number | null = null;

  // Filtros
  filtroRating: string = 'TODOS';
  filtroVisibilidad: string = 'TODAS';
  terminoBusqueda: string = '';

  // Precomputed ratings O(1)
  ratingCounts: Record<number, number> = { 1: 0, 2: 0, 3: 0, 4: 0, 5: 0 };
  promedioRating = 0;

  // Static stars array
  readonly starNumbers = [5, 4, 3, 2, 1];

  // Paginación (12 items fills 3-col grid evenly)
  currentPage = 1;
  itemsPerPage = 12;
  pageSizeOptions: number[] = [12, 24, 48];

  constructor(
    private readonly reviewService: Review,
    private readonly authService: AuthService
  ) {}

  ngOnInit(): void {
    this.isAdmin = this.authService.isAdmin();
    this.cargarReviews();
  }

  recargar(): void {
    this.cargarReviews();
  }

  reintentar(): void {
    this.cargarReviews();
  }

  cargarReviews(): void {
    this.cargando = true;
    this.loadError = '';

    const req = this.isAdmin
      ? this.reviewService.obtenerTodos()
      : this.reviewService.obtenerPorUsuario(this.authService.getCurrentUser()?.id ?? 0);

    req.subscribe({
      next: (data) => {
        this.todasLasReviews = (data || []).toSorted((a, b) =>
          new Date(b.createdAt!).getTime() - new Date(a.createdAt!).getTime()
        );
        this.recalcularMetricas();
        this.aplicarFiltro();
        this.cargando = false;
      },
      error: (err) => {
        console.error('Error al cargar reseñas:', err);
        this.loadError = err?.status === 0
          ? 'No se pudo conectar con el servidor (Backend fuera de línea). Verifica el puerto 8089.'
          : (err?.error?.message || 'Error al cargar las reseñas.');
        this.cargando = false;
      }
    });
  }

  private recalcularMetricas(): void {
    const counts: Record<number, number> = { 1: 0, 2: 0, 3: 0, 4: 0, 5: 0 };
    let suma = 0;

    for (const r of this.todasLasReviews) {
      const star = Math.min(5, Math.max(1, Math.round(r.rating || 5)));
      counts[star] = (counts[star] || 0) + 1;
      suma += (r.rating || 0);
    }

    this.ratingCounts = counts;
    this.promedioRating = this.todasLasReviews.length > 0 ? (suma / this.todasLasReviews.length) : 0;
  }

  aplicarFiltro(): void {
    let resultado = [...this.todasLasReviews];

    if (this.filtroRating !== 'TODOS') {
      const targetRating = Number(this.filtroRating);
      resultado = resultado.filter(r => Math.round(r.rating) === targetRating);
    }

    if (this.filtroVisibilidad === 'ACTIVAS') {
      resultado = resultado.filter(r => r.active !== false);
    } else if (this.filtroVisibilidad === 'OCULTAS') {
      resultado = resultado.filter(r => r.active === false);
    }

    const term = this.terminoBusqueda.trim().toLowerCase();
    if (term) {
      resultado = resultado.filter(
        r =>
          String(r.id).includes(term) ||
          String(r.orderId).includes(term) ||
          String(r.userId).includes(term) ||
          r.comment?.toLowerCase().includes(term)
      );
    }

    this.reviews = resultado;
    this.aplicarPaginacion();
  }

  onFiltroChange(): void {
    this.currentPage = 1;
    this.aplicarFiltro();
  }

  limpiarFiltros(): void {
    this.filtroRating = 'TODOS';
    this.filtroVisibilidad = 'TODAS';
    this.terminoBusqueda = '';
    this.currentPage = 1;
    this.aplicarFiltro();
  }

  aplicarPaginacion(): void {
    const totalPages = Math.ceil(this.reviews.length / this.itemsPerPage);
    if (this.currentPage > totalPages && totalPages > 0) {
      this.currentPage = totalPages;
    } else if (this.currentPage < 1) {
      this.currentPage = 1;
    }
    const startIndex = (this.currentPage - 1) * this.itemsPerPage;
    const endIndex = startIndex + this.itemsPerPage;
    this.reviewsPaginados = this.reviews.slice(startIndex, endIndex);
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

  desactivarReview(id: number): void {
    if (!confirm('¿Está seguro de ocultar esta reseña del catálogo público?')) return;

    this.procesandoId = id;
    this.reviewService.desactivar(id).subscribe({
      next: () => {
        this.procesandoId = null;
        this.mostrarMensaje('Reseña oculta correctamente');
        this.cargarReviews();
      },
      error: (err) => {
        this.procesandoId = null;
        console.error('Error al desactivar:', err);
        this.mostrarMensaje(err?.error?.message || 'Error al ocultar reseña', true);
      }
    });
  }

  activarReview(id: number): void {
    if (!confirm('¿Está seguro de volver a publicar esta reseña?')) return;

    this.procesandoId = id;
    this.reviewService.activar(id).subscribe({
      next: () => {
        this.procesandoId = null;
        this.mostrarMensaje('Reseña publicada correctamente');
        this.cargarReviews();
      },
      error: (err) => {
        this.procesandoId = null;
        console.error('Error al activar:', err);
        this.mostrarMensaje(err?.error?.message || 'Error al publicar reseña', true);
      }
    });
  }

  eliminarReview(id: number): void {
    if (!confirm('¿Está seguro de eliminar definitivamente esta reseña?')) return;

    this.procesandoId = id;
    this.reviewService.eliminar(id).subscribe({
      next: () => {
        this.procesandoId = null;
        this.mostrarMensaje('Reseña eliminada correctamente');
        this.cargarReviews();
      },
      error: (err) => {
        this.procesandoId = null;
        console.error('Error al eliminar:', err);
        this.mostrarMensaje(err?.error?.message || 'Error al eliminar reseña', true);
      }
    });
  }

  getStarArray(rating: number): boolean[] {
    const stars: boolean[] = [];
    const r = Math.round(rating || 0);
    for (let i = 1; i <= 5; i++) {
      stars.push(i <= r);
    }
    return stars;
  }

  mostrarMensaje(msg: string, esError = false): void {
    this.mensaje = msg;
    this.error = esError;
    setTimeout(() => (this.mensaje = ''), 3500);
  }

  trackByReviewId(index: number, item: ReviewDTO): number {
    return item.id ?? index;
  }
}
