import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { forkJoin } from 'rxjs';
import { finalize } from 'rxjs/operators';
import { FavoriteService } from '../../services/favorite.service';
import { AuthService } from '../../services/auth.service';
import { PizzaService } from '../../services/pizza.service';
import { ImageOptimizerService } from '../../services/image-optimizer.service';
import { PizzaDTO } from '../../models/pizza.interface';
import { PaginationComponent } from '../pagination/pagination';

@Component({
  selector: 'app-favoritos',
  standalone: true,
  imports: [CommonModule, PaginationComponent],
  templateUrl: './favoritos.html',
  styleUrls: ['./favoritos.css']
})
export class FavoritosComponent implements OnInit {
  pizzasFavoritas: PizzaDTO[] = [];
  pizzasFavoritasPaginadas: PizzaDTO[] = [];
  loading = false;
  eliminandoId: number | null = null;
  loadError = '';
  mensaje = '';
  esError = false;
  imagenesCargadas = new Set<number>();

  // Paginación (12 llena filas exactas en 1, 2, 3 y 4 columnas)
  currentPage = 1;
  itemsPerPage = 12;

  private readonly favoriteService = inject(FavoriteService);
  private readonly authService = inject(AuthService);
  private readonly pizzaService = inject(PizzaService);
  private readonly imageOptimizer = inject(ImageOptimizerService);
  private readonly router = inject(Router);

  ngOnInit(): void {
    this.cargarFavoritos();
  }

  cargarFavoritos(): void {
    const userId = this.authService.obtenerUsuarioId();

    if (!userId) {
      this.router.navigate(['/join']);
      return;
    }

    this.loading = true;
    this.loadError = '';

    forkJoin({
      favoritos: this.favoriteService.obtenerFavoritosPorUserId(userId),
      todasLasPizzas: this.pizzaService.listarPizzas()
    })
      .pipe(finalize(() => { this.loading = false; }))
      .subscribe({
        next: ({ favoritos, todasLasPizzas }) => {
          const pizzaIds = new Set(favoritos.map(f => f.pizzaId));
          this.pizzasFavoritas = pizzaIds.size === 0
            ? []
            : todasLasPizzas.filter(pizza => pizza.id !== undefined && pizzaIds.has(pizza.id));
          this.aplicarPaginacion();
        },
        error: (error) => {
          console.error('Error al cargar favoritos:', error);
          this.loadError = error?.status === 0
            ? 'No se pudo conectar con el servidor (Backend fuera de línea). Verifica tu conexión e inténtalo de nuevo.'
            : (error?.error?.message || 'Error al cargar tus pizzas favoritas.');
        }
      });
  }

  aplicarPaginacion(): void {
    const totalPages = Math.ceil(this.pizzasFavoritas.length / this.itemsPerPage);
    if (this.currentPage > totalPages && totalPages > 0) {
      this.currentPage = totalPages;
    } else if (this.currentPage < 1) {
      this.currentPage = 1;
    }
    const startIndex = (this.currentPage - 1) * this.itemsPerPage;
    const endIndex = startIndex + this.itemsPerPage;
    this.pizzasFavoritasPaginadas = this.pizzasFavoritas.slice(startIndex, endIndex);
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

  eliminarFavorito(pizzaId: number): void {
    if (this.eliminandoId !== null) return;
    const userId = this.authService.obtenerUsuarioId();
    if (!userId) return;

    if (confirm('¿Deseas eliminar esta pizza de tus favoritos?')) {
      this.eliminandoId = pizzaId;
      this.favoriteService.eliminarFavorito(userId, pizzaId)
        .pipe(finalize(() => { this.eliminandoId = null; }))
        .subscribe({
          next: () => {
            this.pizzasFavoritas = this.pizzasFavoritas.filter(p => p.id !== pizzaId);
            this.aplicarPaginacion();
            this.mostrarMensaje('Pizza eliminada de favoritos', false);
          },
          error: (error) => {
            console.error('Error al eliminar favorito:', error);
            this.mostrarMensaje('Error al eliminar de favoritos', true);
          }
        });
    }
  }

  verPizza(pizzaId: number): void {
    if (pizzaId > 0) {
      this.router.navigate(['/menu'], { queryParams: { highlight: pizzaId } });
    } else {
      this.router.navigate(['/menu']);
    }
  }

  formatearPrecio(precio: number): string {
    return `S/ ${(precio ?? 0).toFixed(2)}`;
  }

  getOptimizedImageUrl(url: string | undefined): string {
    return this.imageOptimizer.optimizeImageUrl(url, 'medium');
  }

  onImageLoad(pizzaId: number): void {
    this.imagenesCargadas.add(pizzaId);
  }

  onImageError(event: Event): void {
    const img = event.target as HTMLImageElement;
    img.src = '/combo1.webp';
  }

  private mostrarMensaje(texto: string, esError = false): void {
    this.mensaje = texto;
    this.esError = esError;

    setTimeout(() => {
      this.mensaje = '';
      this.esError = false;
    }, 3500);
  }
}
