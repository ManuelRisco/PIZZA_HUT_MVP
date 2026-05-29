import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Review } from '../../services/review.service';
import { ReviewDTO } from '../../models/admin.interface';
import { AuthService } from '../../services/auth.service';
import { PaginationComponent } from '../pagination/pagination';

@Component({
  selector: 'app-reviews',
  imports: [CommonModule, PaginationComponent],
  templateUrl: './reviews.html',
  styleUrl: './reviews.css'
})
export class Reviews implements OnInit {
  reviews: ReviewDTO[] = [];
  reviewsPaginados: ReviewDTO[] = [];
  mensaje = '';
  error = false;
  cargando = true;
  isAdmin = false;

  // Paginación
  currentPage = 1;
  itemsPerPage = 10;

  constructor(
    private reviewService: Review,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.isAdmin = this.authService.isAdmin();
    this.cargarReviews();
  }

  cargarReviews(): void {
    this.cargando = true;
    
    if (this.isAdmin) {
      // Admin ve todas las reseñas
      this.reviewService.obtenerTodos().subscribe({
        next: (data) => {
          console.log('Reseñas cargadas:', data);
          this.reviews = data.sort((a, b) => new Date(b.createdAt!).getTime() - new Date(a.createdAt!).getTime());
          this.cargando = false;
          this.aplicarPaginacion();
        },
        error: (err) => {
          console.error('Error al cargar reseñas:', err);
          this.mostrarMensaje('Error al cargar reseñas. Verifica que el servidor esté corriendo.', true);
          this.cargando = false;
        }
      });
    } else {
      // Cliente ve solo sus reseñas
      const currentUser = this.authService.getCurrentUser();
      if (currentUser && currentUser.id) {
        this.reviewService.obtenerPorUsuario(currentUser.id).subscribe({
          next: (data) => {
            console.log('Reseñas del cliente cargadas:', data);
            this.reviews = data.sort((a, b) => new Date(b.createdAt!).getTime() - new Date(a.createdAt!).getTime());
            this.cargando = false;
            this.aplicarPaginacion();
          },
          error: (err) => {
            console.error('Error al cargar reseñas:', err);
            this.mostrarMensaje('Error al cargar tus reseñas.', true);
            this.cargando = false;
          }
        });
      } else {
        this.cargando = false;
        this.mostrarMensaje('Error: Usuario no autenticado', true);
      }
    }
  }

  aplicarPaginacion(): void {
    const startIndex = (this.currentPage - 1) * this.itemsPerPage;
    const endIndex = startIndex + this.itemsPerPage;
    this.reviewsPaginados = this.reviews.slice(startIndex, endIndex);
  }

  onPageChange(page: number): void {
    this.currentPage = page;
    this.aplicarPaginacion();
  }

  eliminarReview(id: number): void {
    if (confirm('¿Está seguro de eliminar esta reseña?')) {
      this.reviewService.eliminar(id).subscribe({
        next: () => {
          this.mostrarMensaje('Reseña eliminada correctamente');
          this.cargarReviews();
        },
        error: (err) => {
          console.error('Error al eliminar:', err);
          this.mostrarMensaje('Error al eliminar reseña', true);
        }
      });
    }
  }

  desactivarReview(id: number): void {
    if (confirm('¿Está seguro de desactivar esta reseña?')) {
      this.reviewService.desactivar(id).subscribe({
        next: () => {
          this.mostrarMensaje('Reseña desactivada correctamente');
          this.cargarReviews();
        },
        error: (err) => {
          console.error('Error al desactivar:', err);
          this.mostrarMensaje('Error al desactivar reseña', true);
        }
      });
    }
  }

  activarReview(id: number): void {
    if (confirm('¿Está seguro de activar esta reseña?')) {
      this.reviewService.activar(id).subscribe({
        next: () => {
          this.mostrarMensaje('Reseña activada correctamente');
          this.cargarReviews();
        },
        error: (err) => {
          console.error('Error al activar:', err);
          this.mostrarMensaje('Error al activar reseña', true);
        }
      });
    }
  }

  getEstrellas(rating: number): string[] {
    const estrellas = [];
    for (let i = 1; i <= 5; i++) {
      estrellas.push(i <= rating ? 'fa-star' : 'fa-star text-muted');
    }
    return estrellas;
  }

  getRatingColor(rating: number): string {
    if (rating >= 4) return 'bg-success';
    if (rating >= 3) return 'bg-warning';
    return 'bg-danger';
  }

  getCountByRating(rating: number): number {
    return this.reviews.filter(r => r.rating === rating).length;
  }

  mostrarMensaje(msg: string, esError = false): void {
    this.mensaje = msg;
    this.error = esError;
    setTimeout(() => this.mensaje = '', 3000);
  }
}
