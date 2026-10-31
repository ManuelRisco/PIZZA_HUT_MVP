import { Component, OnInit, inject, ViewChild, ElementRef, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute, RouterModule } from '@angular/router';
import { forkJoin } from 'rxjs';
import { PizzaService } from '../../services/pizza.service';
import { PizzaDTO } from '../../models/pizza.interface';
import { CartService } from '../../services/cart.service';
import { Size } from '../../services/size.service';
import { IngredientService } from '../../services/ingredient.service';
import { ImageCacheService } from '../../services/image-cache.service';
import { ImageOptimizerService } from '../../services/image-optimizer.service';
import { FavoriteService } from '../../services/favorite.service';
import { AuthService } from '../../services/auth.service';
import { AccessibilityService } from '../../services/accessibility.service';
import { ToastService } from '../../services/toast.service';
import { TranslateService } from '../../services/translate.service';
import { SizeDTO, IngredientDTO } from '../../models/admin.interface';
import { PaginationComponent } from '../pagination/pagination';

@Component({
  selector: 'app-menu',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, PaginationComponent],
  templateUrl: './menu.html',
  styleUrls: ['./menu.css']
})
export class MenuComponent implements OnInit, AfterViewInit {
  pizzas: PizzaDTO[] = [];
  pizzasFiltradas: PizzaDTO[] = [];
  pizzasPaginadas: PizzaDTO[] = [];
  currentPage: number = 1;
  itemsPerPage: number = 6;
  pageSizeOptions: number[] = [6, 9, 12];
  searchTerm: string = '';
  loading = false;
  error = '';

  // Control de carga de imágenes
  imagenesCargadas = new Set<number>();

  // Modal de personalización
  mostrarModal = false;
  mostrarModalRapido = false;
  pizzaSeleccionada?: PizzaDTO;
  tamanosDisponibles: SizeDTO[] = [];
  ingredientesDisponibles: IngredientDTO[] = [];
  tamanoSeleccionado?: SizeDTO;
  extrasSeleccionados: string[] = [];
  precioCalculado = 0;
  descripcionPersonalizacion = '';

  // Variables para favoritos
  favoritosPizzaIds: Set<number> = new Set();
  isLoggedIn: boolean = false;
  currentUserId: number | null = null;

  // Variable para highlight
  pizzaHighlightId: number | null = null;

  private readonly pizzaService = inject(PizzaService);
  private readonly cartService = inject(CartService);
  private readonly sizeService = inject(Size);
  private readonly ingredientService = inject(IngredientService);
  private readonly imageCacheService = inject(ImageCacheService);
  private readonly imageOptimizer = inject(ImageOptimizerService);
  private readonly favoriteService = inject(FavoriteService);
  private readonly authService = inject(AuthService);
  private readonly accessibility = inject(AccessibilityService);
  private readonly toastService = inject(ToastService);
  private readonly translateService = inject(TranslateService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  private readonly el = inject(ElementRef);
  private scrollObserver?: IntersectionObserver;

  @ViewChild('searchInput', { static: false }) searchInput?: ElementRef;

  ngOnInit(): void {
    this.cargarDatosIniciales();
    this.verificarAutenticacion();
    this.manejarHighlight();

    this.accessibility.announce('Menú de pizzas cargado. Usa la búsqueda para filtrar pizzas');
  }

  ngAfterViewInit(): void {
    // Inicialización de vista
  }

  private setupScrollAnimations(): void {
    // Animaciones gestionadas por CSS sin depender de IntersectionObserver
  }

  verificarAutenticacion(): void {
    this.isLoggedIn = this.authService.estaAutenticado();
    if (this.isLoggedIn) {
      this.currentUserId = this.authService.obtenerUsuarioId();
      if (this.currentUserId) {
        this.cargarFavoritos();
      }
    }
  }

  cargarFavoritos(): void {
    if (!this.currentUserId) return;

    this.favoriteService.obtenerFavoritosPorUserId(this.currentUserId).subscribe({
      next: favoritos => {
        this.favoritosPizzaIds = new Set(favoritos.map(f => f.pizzaId));
        this.aplicarOrdenamientoFavoritos();
      },
      error: error => {
        console.error('Error al cargar favoritos:', error);
      }
    });
  }

  esFavorito(pizzaId: number): boolean {
    return this.favoritosPizzaIds.has(pizzaId);
  }

  toggleFavorito(pizzaId: number, event: Event): void {
    event.stopPropagation();

    if (!this.isLoggedIn) {
      this.toastService.showError('Debes iniciar sesión para agregar favoritos');
      setTimeout(() => {
        this.router.navigate(['/join']);
      }, 1500);
      return;
    }

    if (!this.currentUserId) return;

    const esFav = this.esFavorito(pizzaId);
    const pizzaName = this.pizzas.find(p => p.id === pizzaId)?.name || 'Pizza';

    if (esFav) {
      this.favoriteService.eliminarFavorito(this.currentUserId, pizzaId).subscribe({
        next: () => {
          this.favoritosPizzaIds.delete(pizzaId);
          this.toastService.showSuccess('Eliminado de favoritos');
          this.accessibility.announceRemovedFromFavorites(pizzaName);
          this.pizzasFiltradas = this.ordenarPorFavoritos([...this.pizzasFiltradas]);
          this.aplicarPaginacion();
        },
        error: error => {
          console.error('Error al eliminar favorito:', error);
          this.toastService.showError('Error al eliminar de favoritos');
          this.accessibility.announceError('No se pudo eliminar de favoritos');
        }
      });
    } else {
      this.favoriteService.agregarFavorito(this.currentUserId, pizzaId).subscribe({
        next: () => {
          this.favoritosPizzaIds.add(pizzaId);
          this.toastService.showSuccess('Agregado a favoritos');
          this.accessibility.announceAddedToFavorites(pizzaName);
          this.pizzasFiltradas = this.ordenarPorFavoritos([...this.pizzasFiltradas]);
          this.aplicarPaginacion();
        },
        error: error => {
          console.error('Error al agregar favorito:', error);
          this.toastService.showError('Error al agregar a favoritos');
          this.accessibility.announceError('No se pudo agregar a favoritos');
        }
      });
    }
  }

  cargarDatosIniciales(): void {
    this.loading = true;

    forkJoin({
      pizzas: this.pizzaService.listarPizzas(),
      sizes: this.sizeService.obtenerTodos(),
      ingredientes: this.ingredientService.obtenerDisponibles()
    }).subscribe({
      next: response => {
        const pizzasDisponibles = response.pizzas.filter(pizza => pizza.isAvailable);
        this.pizzas = pizzasDisponibles.length > 0 ? pizzasDisponibles : response.pizzas;
        this.pizzasFiltradas = [...this.pizzas];

        this.tamanosDisponibles = response.sizes.toSorted(
          (a, b) => (a.displayOrder || 0) - (b.displayOrder || 0)
        );

        this.ingredientesDisponibles = response.ingredientes.filter(ing => ing.isAvailable && !ing.outOfStock);

        this.loading = false;
        this.precargarImagenes();
        this.aplicarOrdenamientoFavoritos();
        this.setupScrollAnimations();

        setTimeout(() => {
          this.translateService.retranslate();
        }, 300);
      },
      error: error => {
        console.error('Error al cargar datos:', error);
        this.error = 'Error al cargar el menú';
        this.loading = false;
      }
    });
  }

  private precargarImagenes(): void {
    const imageUrls = this.pizzas
      .map(p => p.imageUrl)
      .filter(url => url && url.trim() !== '') as string[];

    const optimizedUrls = imageUrls.map(url => this.imageOptimizer.optimizeImageUrl(url, 'medium'));

    this.imageCacheService
      .preloadImages(optimizedUrls)
      .then(() => {})
      .catch(() => {});
  }

  isImageCached(imageUrl: string | undefined): boolean {
    if (!imageUrl) return false;
    const optimizedUrl = this.imageOptimizer.optimizeImageUrl(imageUrl, 'medium');
    return this.imageCacheService.isImageCached(optimizedUrl);
  }

  filtrarPizzas(): void {
    const termino = this.searchTerm.toLowerCase().trim();

    let pizzasFiltradas: PizzaDTO[];

    if (!termino) {
      pizzasFiltradas = [...this.pizzas];
    } else {
      pizzasFiltradas = this.pizzas.filter(
        pizza =>
          pizza.name.toLowerCase().includes(termino) ||
          pizza.description?.toLowerCase().includes(termino)
      );
    }

    this.pizzasFiltradas = this.ordenarPorFavoritos(pizzasFiltradas);
    this.currentPage = 1;
    this.aplicarPaginacion();

    if (termino) {
      const resultCount = this.pizzasFiltradas.length;
      if (resultCount > 0) {
        this.accessibility.announce(
          `Búsqueda completada. Se encontraron ${resultCount} ${resultCount === 1 ? 'pizza' : 'pizzas'}`
        );
      } else {
        this.accessibility.announce('No se encontraron pizzas con ese criterio de búsqueda');
      }
    }

    this.setupScrollAnimations();
  }

  aplicarPaginacion(): void {
    const totalPages = Math.ceil(this.pizzasFiltradas.length / this.itemsPerPage);
    if (this.currentPage > totalPages && totalPages > 0) {
      this.currentPage = totalPages;
    } else if (this.currentPage < 1) {
      this.currentPage = 1;
    }
    const startIndex = (this.currentPage - 1) * this.itemsPerPage;
    const endIndex = startIndex + this.itemsPerPage;
    this.pizzasPaginadas = this.pizzasFiltradas.slice(startIndex, endIndex);
  }

  onPageChange(page: number): void {
    this.currentPage = page;
    this.aplicarPaginacion();
    if (typeof window !== 'undefined') {
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  }

  onItemsPerPageChange(size: number): void {
    this.itemsPerPage = size;
    this.currentPage = 1;
    this.aplicarPaginacion();
  }

  private ordenarPorFavoritos(pizzas: PizzaDTO[]): PizzaDTO[] {
    return pizzas.toSorted((a, b) => {
      const aEsFavorito = this.esFavorito(a.id || 0);
      const bEsFavorito = this.esFavorito(b.id || 0);
      if (aEsFavorito === bEsFavorito) {
        return 0;
      }
      return aEsFavorito ? -1 : 1;
    });
  }

  private aplicarOrdenamientoFavoritos(): void {
    if (this.pizzas.length > 0) {
      this.pizzasFiltradas = this.ordenarPorFavoritos([...this.pizzasFiltradas]);
      this.aplicarPaginacion();
    }
  }

  private manejarHighlight(): void {
    this.route.queryParams.subscribe(params => {
      const highlightId = params['highlight'];
      if (highlightId) {
        this.pizzaHighlightId = +highlightId;
        setTimeout(() => {
          this.scrollToPizza(this.pizzaHighlightId!);
        }, 500);
        setTimeout(() => {
          this.pizzaHighlightId = null;
        }, 3000);
      }
    });
  }

  private scrollToPizza(pizzaId: number): void {
    const pizzaElement = document.getElementById(`pizza-${pizzaId}`);
    if (pizzaElement) {
      pizzaElement.scrollIntoView({
        behavior: 'smooth',
        block: 'center',
        inline: 'nearest'
      });
    }
  }

  limpiarBuscador(): void {
    this.searchTerm = '';
    this.filtrarPizzas();
    this.accessibility.announce('Búsqueda borrada. Mostrando todas las pizzas disponibles');

    setTimeout(() => {
      this.searchInput?.nativeElement?.focus();
    }, 100);
  }

  getOptimizedImageUrl(imageUrl: string | undefined): string {
    return this.imageOptimizer.optimizeImageUrl(imageUrl, 'medium');
  }

  /**
   * Retorna el stock de masa disponible para el tamaño seleccionado.
   * Todas las pizzas comparten el stock de masa por tamaño.
   */
  getStockParaTamano(pizza: PizzaDTO | undefined, tamano?: SizeDTO): number {
    if (!pizza) return 0;
    if (!pizza.isAvailable || pizza.outOfStock) return 0;
    if (tamano && tamano.stockQuantity !== undefined) {
      return Math.max(0, tamano.stockQuantity);
    }
    return pizza.availableStock !== undefined ? Math.max(0, pizza.availableStock) : 0;
  }

  esPizzaAgotada(pizza: PizzaDTO): boolean {
    if (!pizza.isAvailable || pizza.outOfStock) return true;
    if (pizza.availableStock !== undefined && pizza.availableStock <= 0) return true;
    if (this.tamanosDisponibles.length > 0 && this.tamanosDisponibles.every(t => (t.stockQuantity ?? 0) <= 0)) {
      return true;
    }
    return false;
  }

  abrirModalAgregarRapido(pizza: PizzaDTO): void {
    if (this.esPizzaAgotada(pizza)) {
      this.toastService.showError('Esta pizza se encuentra agotada por falta de stock');
      return;
    }
    this.pizzaSeleccionada = pizza;
    this.tamanoSeleccionado = undefined;
    this.mostrarModalRapido = true;
    this.enfocarPrimerElementoModal();
  }

  cerrarModalRapido(): void {
    this.mostrarModalRapido = false;
    this.pizzaSeleccionada = undefined;
    this.tamanoSeleccionado = undefined;
    this.devolverFocoElementoAnterior();
  }

  agregarAlCarritoRapido(): void {
    if (!this.pizzaSeleccionada || !this.tamanoSeleccionado) {
      this.toastService.showError('Por favor selecciona un tamaño');
      return;
    }

    const maxStock = this.getStockParaTamano(this.pizzaSeleccionada, this.tamanoSeleccionado);
    if (maxStock <= 0) {
      this.toastService.showError('No hay suficiente stock para el tamaño seleccionado');
      return;
    }

    try {
      const precioTotal = this.pizzaSeleccionada.price + (this.tamanoSeleccionado.extraCost || 0);

      this.cartService.addItem({
        pizza: {
          ...this.pizzaSeleccionada,
          price: precioTotal
        },
        size: this.tamanoSeleccionado.name,
        sizeId: this.tamanoSeleccionado.id,
        quantity: 1,
        maxStock
      });

      this.toastService.showSuccess(`${this.pizzaSeleccionada.name} agregado al carrito`);
      this.accessibility.announceAddToCart(this.pizzaSeleccionada.name);
      this.cerrarModalRapido();
    } catch (error) {
      console.error('Error al agregar al carrito:', error);
      this.toastService.showError('No se pudo agregar al carrito');
      this.accessibility.announceError('No se pudo agregar al carrito');
    }
  }

  abrirModalPersonalizacion(pizza: PizzaDTO): void {
    if (this.esPizzaAgotada(pizza)) {
      this.toastService.showError('Esta pizza se encuentra agotada por falta de stock');
      return;
    }
    this.pizzaSeleccionada = pizza;
    this.tamanoSeleccionado = undefined;
    this.extrasSeleccionados = [];
    this.precioCalculado = pizza.price || 0;
    this.descripcionPersonalizacion = pizza.name;
    this.mostrarModal = true;
    this.enfocarPrimerElementoModal();
  }

  cerrarModal(): void {
    this.mostrarModal = false;
    this.pizzaSeleccionada = undefined;
    this.tamanoSeleccionado = undefined;
    this.extrasSeleccionados = [];
    this.devolverFocoElementoAnterior();
  }

  seleccionarTamano(tamano: SizeDTO): void {
    if (this.getStockParaTamano(this.pizzaSeleccionada, tamano) <= 0) {
      this.toastService.showError(`Stock insuficiente para tamaño ${tamano.name}`);
      return;
    }
    this.tamanoSeleccionado = tamano;
    this.calcularPrecio();
  }

  toggleExtra(ingredienteId: string): void {
    const index = this.extrasSeleccionados.indexOf(ingredienteId);
    if (index >= 0) {
      this.extrasSeleccionados.splice(index, 1);
    } else {
      this.extrasSeleccionados.push(ingredienteId);
    }
    this.calcularPrecio();
  }

  calcularPrecio(): void {
    if (!this.pizzaSeleccionada) return;

    const precioBasePizza = this.pizzaSeleccionada.price || 0;
    const costoTamano = this.tamanoSeleccionado?.extraCost || 0;

    let descripcion = this.pizzaSeleccionada.name;
    if (this.tamanoSeleccionado) {
      descripcion += ` - ${this.tamanoSeleccionado.name}`;
    }

    const costosExtras = this.extrasSeleccionados.reduce((total, extraId) => {
      const ingrediente = this.ingredientesDisponibles.find(i => i.id?.toString() === extraId);
      return total + (ingrediente?.extraCost || 0);
    }, 0);

    this.precioCalculado = precioBasePizza + costoTamano + costosExtras;
    this.descripcionPersonalizacion = descripcion;
  }

  agregarAlCarritoPersonalizado(): void {
    if (!this.pizzaSeleccionada || !this.tamanoSeleccionado) {
      this.toastService.showError('Por favor selecciona un tamaño');
      return;
    }

    const maxStock = this.getStockParaTamano(this.pizzaSeleccionada, this.tamanoSeleccionado);
    if (maxStock <= 0) {
      this.toastService.showError('No hay suficiente stock para el tamaño seleccionado');
      return;
    }

    try {
      this.cartService.addItem({
        pizza: {
          ...this.pizzaSeleccionada,
          price: this.precioCalculado
        },
        size: this.tamanoSeleccionado.name,
        sizeId: this.tamanoSeleccionado.id,
        quantity: 1,
        extras: this.extrasSeleccionados,
        maxStock
      });

      this.toastService.showSuccess(`${this.pizzaSeleccionada.name} agregado al carrito`);
      this.accessibility.announceAddToCart(this.pizzaSeleccionada.name);
      this.cerrarModal();
    } catch (error) {
      console.error('Error al agregar al carrito:', error);
      this.toastService.showError('No se pudo agregar al carrito');
      this.accessibility.announceError('No se pudo agregar al carrito');
    }
  }

  formatearPrecio(precio: number): string {
    return `S/. ${precio.toFixed(2)}`;
  }

  onImageLoad(pizzaId: number): void {
    this.imagenesCargadas.add(pizzaId);

    const pizza = this.pizzas.find(p => p.id === pizzaId);
    if (pizza?.imageUrl) {
      const optimizedUrl = this.imageOptimizer.optimizeImageUrl(pizza.imageUrl, 'medium');
      this.imageCacheService.markAsLoaded(optimizedUrl);
    }
  }

  onImageError(event: any): void {
    event.target.src = '/combo1.webp';
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
}
