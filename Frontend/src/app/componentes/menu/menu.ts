import { Component, OnInit, inject, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { forkJoin } from 'rxjs';
import { PizzaService } from '../../services/pizza.service';
import { PizzaDTO } from '../../models/pizza.interface';
import { CartService } from '../../services/cart.service';
import { Size } from '../../services/size.service';
import { IngredientService } from '../../services/ingredient.service';
import { PizzaPatronesService } from '../../services/pizza-patrones.service';
import { ImageCacheService } from '../../services/image-cache.service';
import { ImageOptimizerService } from '../../services/image-optimizer.service';
import { FavoriteService } from '../../services/favorite.service';
import { AuthService } from '../../services/auth.service';
import { AccessibilityService } from '../../services/accessibility.service';
import { SizeDTO, IngredientDTO } from '../../models/admin.interface';

@Component({
  selector: 'app-menu',
  imports: [CommonModule, FormsModule],
  templateUrl: './menu.html',
  styleUrls: ['./menu.css']
})
export class MenuComponent implements OnInit {
  pizzas: PizzaDTO[] = [];
  pizzasFiltradas: PizzaDTO[] = [];
  searchTerm: string = '';
  loading = false;
  error = '';
  mensaje = '';
  esError = false;

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

  private pizzaService = inject(PizzaService);
  private cartService = inject(CartService);
  private sizeService = inject(Size);
  private ingredientService = inject(IngredientService);
  private pizzaPatronesService = inject(PizzaPatronesService);
  private imageCacheService = inject(ImageCacheService);
  private imageOptimizer = inject(ImageOptimizerService);
  private favoriteService = inject(FavoriteService);
  private authService = inject(AuthService);
  private accessibility = inject(AccessibilityService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  
  @ViewChild('searchInput', { static: false }) searchInput?: ElementRef;

  ngOnInit(): void {
    this.cargarDatosIniciales();
    this.verificarAutenticacion();
    this.manejarHighlight();
    
    // Announce menu is ready for screen readers
    this.accessibility.announce('Menú de pizzas cargado. Usa la búsqueda para filtrar pizzas');
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
      next: (favoritos) => {
        this.favoritosPizzaIds = new Set(favoritos.map(f => f.pizzaId));
        console.log('⭐ Favoritos cargados:', this.favoritosPizzaIds.size);
        
        // Aplicar ordenamiento por favoritos
        this.aplicarOrdenamientoFavoritos();
      },
      error: (error) => {
        console.error('Error al cargar favoritos:', error);
      }
    });
  }

  esFavorito(pizzaId: number): boolean {
    return this.favoritosPizzaIds.has(pizzaId);
  }

  toggleFavorito(pizzaId: number, event: Event): void {
    event.stopPropagation(); // Evitar que se active el click del contenedor

    if (!this.isLoggedIn) {
      this.mostrarMensaje('Debes iniciar sesión para agregar favoritos', true);
      setTimeout(() => {
        this.router.navigate(['/join']);
      }, 1500);
      return;
    }

    if (!this.currentUserId) return;

    const esFav = this.esFavorito(pizzaId);
    const pizzaName = this.pizzas.find(p => p.id === pizzaId)?.name || 'Pizza';

    if (esFav) {
      // Eliminar de favoritos
      this.favoriteService.eliminarFavorito(this.currentUserId, pizzaId).subscribe({
        next: () => {
          this.favoritosPizzaIds.delete(pizzaId);
          this.mostrarMensaje('Eliminado de favoritos', false);
          this.accessibility.announceRemovedFromFavorites(pizzaName);
          console.log('💔 Pizza eliminada de favoritos:', pizzaId);
          
          // Reordenar después de eliminar favorito
          this.pizzasFiltradas = this.ordenarPorFavoritos([...this.pizzasFiltradas]);
        },
        error: (error) => {
          console.error('Error al eliminar favorito:', error);
          this.mostrarMensaje('Error al eliminar de favoritos', true);
          this.accessibility.announceError('No se pudo eliminar de favoritos');
        }
      });
    } else {
      // Agregar a favoritos
      this.favoriteService.agregarFavorito(this.currentUserId, pizzaId).subscribe({
        next: () => {
          this.favoritosPizzaIds.add(pizzaId);
          this.mostrarMensaje('Agregado a favoritos', false);
          this.accessibility.announceAddedToFavorites(pizzaName);
          console.log('💖 Pizza agregada a favoritos:', pizzaId);
          
          // Reordenar después de agregar favorito
          this.pizzasFiltradas = this.ordenarPorFavoritos([...this.pizzasFiltradas]);
        },
        error: (error) => {
          console.error('Error al agregar favorito:', error);
          this.mostrarMensaje('Error al agregar a favoritos', true);
          this.accessibility.announceError('No se pudo agregar a favoritos');
        }
      });
    }
  }

  cargarDatosIniciales(): void {
    this.loading = true;
    
    // Cargar pizzas, tamaños e ingredientes en paralelo
    forkJoin({
      pizzas: this.pizzaService.listarPizzas(),
      sizes: this.sizeService.obtenerTodos(),
      ingredientes: this.ingredientService.obtenerDisponibles()
    }).subscribe({
      next: (response) => {
        // Pizzas
        const pizzasDisponibles = response.pizzas.filter(pizza => pizza.isAvailable);
        this.pizzas = pizzasDisponibles.length > 0 ? pizzasDisponibles : response.pizzas;
        this.pizzasFiltradas = [...this.pizzas]; // Inicializar pizzas filtradas
        
        // Tamaños
        this.tamanosDisponibles = response.sizes.sort((a, b) => 
          (a.displayOrder || 0) - (b.displayOrder || 0)
        );
        
        // Ingredientes
        this.ingredientesDisponibles = response.ingredientes.filter(ing => ing.isAvailable);
        
        this.loading = false;
        
        // Pre-cargar imágenes en segundo plano
        this.precargarImagenes();
        
        // Aplicar ordenamiento por favoritos si ya se cargaron
        this.aplicarOrdenamientoFavoritos();
      },
      error: (error) => {
        console.error('Error al cargar datos:', error);
        this.error = 'Error al cargar el menú';
        this.loading = false;
      }
    });
  }
  
  /**
   * Pre-carga todas las imágenes del menú en segundo plano (versiones optimizadas)
   */
  private precargarImagenes(): void {
    const imageUrls = this.pizzas
      .map(p => p.imageUrl)
      .filter(url => url && url.trim() !== '') as string[];
    
    // Optimizar las URLs antes de pre-cargar
    const optimizedUrls = imageUrls.map(url => 
      this.imageOptimizer.optimizeImageUrl(url, 'medium')
    );
    
    // Pre-cargar en segundo plano sin bloquear la UI
    this.imageCacheService.preloadImages(optimizedUrls).then(() => {
      console.log(`✅ ${optimizedUrls.length} imágenes optimizadas pre-cargadas en caché`);
    }).catch(error => {
      console.warn('Algunas imágenes no se pudieron pre-cargar:', error);
    });
  }
  
  /**
   * Verifica si una imagen ya está en caché (versión optimizada)
   */
  isImageCached(imageUrl: string | undefined): boolean {
    if (!imageUrl) return false;
    const optimizedUrl = this.imageOptimizer.optimizeImageUrl(imageUrl, 'medium');
    return this.imageCacheService.isImageCached(optimizedUrl);
  }

  /**
   * Filtra las pizzas según el término de búsqueda y ordena favoritos primero
   */
  filtrarPizzas(): void {
    const termino = this.searchTerm.toLowerCase().trim();
    
    let pizzasFiltradas: PizzaDTO[];
    
    if (!termino) {
      pizzasFiltradas = [...this.pizzas];
    } else {
      pizzasFiltradas = this.pizzas.filter(pizza => 
        pizza.name.toLowerCase().includes(termino) ||
        (pizza.description && pizza.description.toLowerCase().includes(termino))
      );
    }

    // Ordenar: favoritos primero
    this.pizzasFiltradas = this.ordenarPorFavoritos(pizzasFiltradas);
    
    // Announce search results for accessibility
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
  }

  /**
   * Ordena las pizzas poniendo los favoritos primero
   */
  private ordenarPorFavoritos(pizzas: PizzaDTO[]): PizzaDTO[] {
    return pizzas.sort((a, b) => {
      const aEsFavorito = this.esFavorito(a.id || 0);
      const bEsFavorito = this.esFavorito(b.id || 0);
      
      // Si ambos son favoritos o ambos no lo son, mantener orden original
      if (aEsFavorito === bEsFavorito) {
        return 0;
      }
      
      // Los favoritos van primero
      return aEsFavorito ? -1 : 1;
    });
  }

  /**
   * Aplica el ordenamiento por favoritos solo si las pizzas ya están cargadas
   */
  private aplicarOrdenamientoFavoritos(): void {
    if (this.pizzas.length > 0) {
      this.pizzasFiltradas = this.ordenarPorFavoritos([...this.pizzasFiltradas]);
    }
  }

  /**
   * Maneja el parámetro de highlight para iluminar una pizza específica
   */
  private manejarHighlight(): void {
    this.route.queryParams.subscribe(params => {
      const highlightId = params['highlight'];
      if (highlightId) {
        this.pizzaHighlightId = +highlightId;
        // Scroll suave después de que las pizzas se hayan cargado
        setTimeout(() => {
          this.scrollToPizza(this.pizzaHighlightId!);
        }, 500);
        // Remover highlight después de unos segundos
        setTimeout(() => {
          this.pizzaHighlightId = null;
        }, 3000);
      }
    });
  }

  /**
   * Hace scroll suave hacia una pizza específica
   */
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

  /**
   * Limpia el buscador y reaplica ordenamiento
   */
  limpiarBuscador(): void {
    this.searchTerm = '';
    this.pizzasFiltradas = this.ordenarPorFavoritos([...this.pizzas]);
    this.accessibility.announce('Búsqueda borrada. Mostrando todas las pizzas disponibles');
    
    // Set focus back to search input
    setTimeout(() => {
      this.searchInput?.nativeElement?.focus();
    }, 100);
  }

  /**
   * Obtiene la URL optimizada de una imagen
   */
  getOptimizedImageUrl(imageUrl: string | undefined): string {
    return this.imageOptimizer.optimizeImageUrl(imageUrl, 'medium');
  }

  cargarPizzasDisponibles(): void {
    this.loading = true;
    this.pizzaService.listarPizzas().subscribe({
      next: (pizzas) => {
        // Filtrar solo las pizzas disponibles (temporal: mostrar todas si no hay disponibles)
        const pizzasDisponibles = pizzas.filter(pizza => pizza.isAvailable);
        this.pizzas = pizzasDisponibles.length > 0 ? pizzasDisponibles : pizzas;
        this.loading = false;
      },
      error: (error) => {
        console.error('Error al cargar pizzas:', error);
        this.error = 'Error al cargar las pizzas';
        this.loading = false;
      }
    });
  }

  cargarTamanos(): void {
    this.sizeService.obtenerTodos().subscribe({
      next: (sizes) => {
        this.tamanosDisponibles = sizes.sort((a, b) => 
          (a.displayOrder || 0) - (b.displayOrder || 0)
        );
      },
      error: (error) => {
        console.error('Error al cargar tamaños:', error);
      }
    });
  }

  cargarIngredientes(): void {
    this.ingredientService.obtenerDisponibles().subscribe({
      next: (ingredientes) => {
        this.ingredientesDisponibles = ingredientes.filter(ing => ing.isAvailable);
      },
      error: (error) => {
        console.error('Error al cargar ingredientes:', error);
      }
    });
  }

  abrirModalAgregarRapido(pizza: PizzaDTO): void {
    this.pizzaSeleccionada = pizza;
    this.tamanoSeleccionado = undefined;
    this.mostrarModalRapido = true;
  }

  cerrarModalRapido(): void {
    this.mostrarModalRapido = false;
    this.pizzaSeleccionada = undefined;
    this.tamanoSeleccionado = undefined;
  }

  agregarAlCarritoRapido(): void {
    if (!this.pizzaSeleccionada || !this.tamanoSeleccionado) {
      this.mostrarMensaje('Por favor selecciona un tamaño', true);
      return;
    }

    try {
      // Calcular precio simple: base + tamaño extra
      const precioTotal = this.pizzaSeleccionada.price + (this.tamanoSeleccionado.extraCost || 0);
      
      this.cartService.addItem({
        pizza: {
          ...this.pizzaSeleccionada,
          price: precioTotal
        },
        size: this.tamanoSeleccionado.name,
        sizeId: this.tamanoSeleccionado.id, // Agregar el ID del tamaño
        quantity: 1
      });

      this.mostrarMensaje(`${this.pizzaSeleccionada.name} agregado al carrito`);
      this.accessibility.announceAddToCart(this.pizzaSeleccionada.name);
      this.cerrarModalRapido();
    } catch (error) {
      console.error('Error al agregar al carrito:', error);
      this.mostrarMensaje('No se pudo agregar al carrito', true);
      this.accessibility.announceError('No se pudo agregar al carrito');
    }
  }

  abrirModalPersonalizacion(pizza: PizzaDTO): void {
    this.pizzaSeleccionada = pizza;
    this.tamanoSeleccionado = undefined;
    this.extrasSeleccionados = [];
    this.precioCalculado = pizza.price || 0;
    this.descripcionPersonalizacion = pizza.name;
    this.mostrarModal = true;
  }

  cerrarModal(): void {
    this.mostrarModal = false;
    this.pizzaSeleccionada = undefined;
    this.tamanoSeleccionado = undefined;
    this.extrasSeleccionados = [];
  }

  seleccionarTamano(tamano: SizeDTO): void {
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

    // Precio base de la pizza (sin modificaciones)
    const precioBasePizza = this.pizzaSeleccionada.price || 0;
    const costoTamano = this.tamanoSeleccionado?.extraCost || 0;
    
    let descripcion = this.pizzaSeleccionada.name;
    if (this.tamanoSeleccionado) {
      descripcion += ` - ${this.tamanoSeleccionado.name}`;
    }

    // Si hay extras, calcular con el backend usando Decorator Pattern
    if (this.extrasSeleccionados.length > 0 && this.pizzaSeleccionada.id) {
      this.pizzaPatronesService.calcularPrecioConExtras(
        this.pizzaSeleccionada.id,
        this.extrasSeleccionados
      ).subscribe({
        next: (response) => {
          // El backend retorna: precioFinal, descripcion, precioBase, extras
          // Agregamos el costo del tamaño
          this.precioCalculado = (response.precioFinal || response.finalPrice || 0) + costoTamano;
          this.descripcionPersonalizacion = response.descripcion || response.description || descripcion;
          if (this.tamanoSeleccionado) {
            this.descripcionPersonalizacion += ` - ${this.tamanoSeleccionado.name}`;
          }
        },
        error: (error) => {
          console.error('Error al calcular precio:', error);
          // Calcular localmente en caso de error
          const costosExtras = this.extrasSeleccionados.reduce((total, extraId) => {
            const ingrediente = this.ingredientesDisponibles.find(i => i.id?.toString() === extraId);
            return total + (ingrediente?.extraCost || 0);
          }, 0);
          this.precioCalculado = precioBasePizza + costoTamano + costosExtras;
          this.descripcionPersonalizacion = descripcion;
        }
      });
    } else {
      // Sin extras: solo precio base + tamaño
      this.precioCalculado = precioBasePizza + costoTamano;
      this.descripcionPersonalizacion = descripcion;
    }
  }

  agregarAlCarritoPersonalizado(): void {
    if (!this.pizzaSeleccionada || !this.tamanoSeleccionado) {
      this.mostrarMensaje('Por favor selecciona un tamaño', true);
      return;
    }

    try {
      // Agregar al carrito con la información personalizada
      this.cartService.addItem({
        pizza: {
          ...this.pizzaSeleccionada,
          price: this.precioCalculado
        },
        size: this.tamanoSeleccionado.name,
        sizeId: this.tamanoSeleccionado.id, // Agregar el ID del tamaño
        quantity: 1,
        extras: this.extrasSeleccionados
      });

      this.mostrarMensaje(`${this.pizzaSeleccionada.name} agregado al carrito`);
      this.accessibility.announceAddToCart(this.pizzaSeleccionada.name);
      this.cerrarModal();
    } catch (error) {
      console.error('Error al agregar al carrito:', error);
      this.mostrarMensaje('No se pudo agregar al carrito', true);
      this.accessibility.announceError('No se pudo agregar al carrito');
    }
  }

  formatearPrecio(precio: number): string {
    return `S/. ${precio.toFixed(2)}`;
  }

  onImageLoad(pizzaId: number): void {
    this.imagenesCargadas.add(pizzaId);
    
    // Obtener URL optimizada de la imagen y marcarla en caché
    const pizza = this.pizzas.find(p => p.id === pizzaId);
    if (pizza?.imageUrl) {
      const optimizedUrl = this.imageOptimizer.optimizeImageUrl(pizza.imageUrl, 'medium');
      this.imageCacheService.markAsLoaded(optimizedUrl);
    }
  }

  onImageError(event: any): void {
    // Cambiar a imagen por defecto cuando falla la carga
    event.target.src = '/combo1.webp';
    // Marcar como cargada para ocultar skeleton
    const pizzaId = this.pizzas.find(p => p.imageUrl === event.target.src)?.id;
    if (pizzaId) {
      this.imagenesCargadas.add(pizzaId);
    }
  }

  private mostrarMensaje(texto: string, esError = false): void {
    this.mensaje = texto;
    this.esError = esError;
    setTimeout(() => {
      this.mensaje = '';
    }, 2500);
  }
}
