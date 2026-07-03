import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FavoriteService } from '../../services/favorite.service';
import { AuthService } from '../../services/auth.service';
import { PizzaService } from '../../services/pizza.service';
import { CartService } from '../../services/cart.service';
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
  mensaje = '';
  esError = false;
  imagenesCargadas = new Set<number>();
  private readonly imageCache= new Set<string>();

  // Paginación
  currentPage = 1;
  itemsPerPage = 10;

  private readonly favoriteService = inject(FavoriteService);
  private readonly authService = inject(AuthService);
  private readonly pizzaService = inject(PizzaService);
  private readonly cartService = inject(CartService);
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

    this.favoriteService.obtenerFavoritosPorUserId(userId).subscribe({
      next: (favoritos) => {
        // Obtener los detalles de cada pizza favorita
        const pizzaIds = favoritos.map(f => f.pizzaId);
        
        if (pizzaIds.length === 0) {
          this.pizzasFavoritas = [];
          this.loading = false;
          return;
        }

        this.pizzaService.listarPizzas().subscribe({
          next: (todasLasPizzas) => {
            this.pizzasFavoritas = todasLasPizzas.filter(pizza => 
              pizza.id && pizzaIds.includes(pizza.id)
            );
            this.loading = false;
            this.aplicarPaginacion();
          },
          error: (error) => {
            console.error('Error al cargar pizzas:', error);
            this.mostrarMensaje('Error al cargar los detalles de las pizzas', true);
            this.loading = false;
          }
        });
      },
      error: (error) => {
        console.error('Error al cargar favoritos:', error);
        this.mostrarMensaje('Error al cargar favoritos', true);
        this.loading = false;
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

  eliminarFavorito(pizzaId: number): void {
    const userId = this.authService.obtenerUsuarioId();
    
    if (!userId) return;

    if (confirm('¿Deseas eliminar esta pizza de tus favoritos?')) {
      this.favoriteService.eliminarFavorito(userId, pizzaId).subscribe({
        next: () => {
          this.mostrarMensaje('Pizza eliminada de favoritos', false);
          this.cargarFavoritos(); // Recargar la lista
        },
        error: (error) => {
          console.error('Error al eliminar favorito:', error);
          this.mostrarMensaje('Error al eliminar de favoritos', true);
        }
      });
    }
  }

  verPizza(pizzaId: number): void {
    this.router.navigate(['/menu'], { 
      queryParams: { highlight: pizzaId }
    });
  }

  formatearPrecio(precio: number): string {
    return `S/ ${precio.toFixed(2)}`;
  }

  // Optimización de imágenes
  getOptimizedImageUrl(url: string | undefined): string {
    if (!url) return 'assets/pizza-placeholder.png';
    
    // Si la URL ya tiene parámetros, agregar con &, sino con ?
    const separator = url.includes('?') ? '&' : '?';
    return `${url}${separator}w=400&q=80&format=webp`;
  }

  onImageLoad(pizzaId: number): void {
    this.imagenesCargadas.add(pizzaId);
  }

  onImageError(event: Event): void {
    const img = event.target as HTMLImageElement;
    img.src = 'assets/pizza-placeholder.png';
  }

  isImageCached(url: string): boolean {
    if (!url) return false;
    
    if (this.imageCache.has(url)) {
      return true;
    }
    
    // Verificar si la imagen está en caché del navegador
    const img = new Image();
    img.src = this.getOptimizedImageUrl(url);
    
    if (img.complete) {
      this.imageCache.add(url);
      return true;
    }
    
    return false;
  }

  private mostrarMensaje(texto: string, esError = false): void {
    this.mensaje = texto;
    this.esError = esError;
    
    setTimeout(() => {
      this.mensaje = '';
      this.esError = false;
    }, 3000);
  }
}
