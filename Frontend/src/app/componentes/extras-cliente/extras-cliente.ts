import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ExtraService, Extra } from '../../services/extra.service';
import { CartService } from '../../services/cart.service';
import { AccessibilityService } from '../../services/accessibility.service';
import { ToastService } from '../../services/toast.service';
import { ImageOptimizerService } from '../../services/image-optimizer.service';

@Component({
  selector: 'app-extras-cliente',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './extras-cliente.html',
  styleUrls: ['./extras-cliente.css']
})
export class ExtrasClienteComponent implements OnInit {
  extras: Extra[] = [];
  extrasFiltrados: Extra[] = [];
  searchTerm: string = '';
  categoriaSeleccionada: string = 'TODOS';
  loading = false;
  error = '';

  categorias = [
    { value: 'TODOS', label: 'Todos', icon: 'bi-grid' },
    { value: 'BEBIDA', label: 'Bebidas', icon: 'bi-cup-straw' },
    { value: 'POSTRE', label: 'Postres', icon: 'bi-cake2' },
    { value: 'ENTRADA', label: 'Entradas', icon: 'bi-egg-fried' },
    { value: 'COMPLEMENTO', label: 'Complementos', icon: 'bi-basket' }
  ];

  private readonly extraService = inject(ExtraService);
  private readonly cartService = inject(CartService);
  private readonly accessibility = inject(AccessibilityService);
  private readonly toastService = inject(ToastService);
  private readonly imageOptimizer = inject(ImageOptimizerService);

  ngOnInit(): void {
    this.cargarExtras();
  }

  cargarExtras(): void {
    this.loading = true;
    this.error = '';

    this.extraService.listarDisponibles().subscribe({
      next: data => {
        this.extras = data.toSorted((a, b) => (a.displayOrder || 0) - (b.displayOrder || 0));
        this.aplicarFiltros();
        this.loading = false;
      },
      error: err => {
        console.error('Error al cargar extras:', err);
        this.error = 'Error al cargar los extras';
        this.loading = false;
      }
    });
  }

  cambiarCategoria(categoria: string): void {
    this.categoriaSeleccionada = categoria;
    this.aplicarFiltros();
  }

  aplicarFiltros(): void {
    let resultado = [...this.extras];

    if (this.categoriaSeleccionada !== 'TODOS') {
      resultado = resultado.filter(e => e.category === this.categoriaSeleccionada);
    }

    if (this.searchTerm.trim()) {
      const term = this.searchTerm.toLowerCase();
      resultado = resultado.filter(
        e => e.name.toLowerCase().includes(term) || e.description?.toLowerCase().includes(term)
      );
    }

    this.extrasFiltrados = resultado;
  }

  limpiarBuscador(): void {
    this.searchTerm = '';
    this.aplicarFiltros();
  }

  getImageUrl(imageUrl?: string): string {
    return this.imageOptimizer.optimizeImageUrl(imageUrl, 'medium');
  }

  esAgotado(extra: Extra): boolean {
    return !extra.isAvailable || !!extra.outOfStock || (extra.stockQuantity !== undefined && extra.stockQuantity <= 0);
  }

  agregarAlCarrito(extra: Extra): void {
    if (!extra.id || extra.price === undefined) return;

    if (this.esAgotado(extra)) {
      this.toastService.showError(`${extra.name} se encuentra agotado`);
      return;
    }

    const currentInCart = this.cartService.getItemsSnapshot().find(i => i.id === `extra-${extra.id}`);
    if (extra.stockQuantity !== undefined && currentInCart && currentInCart.quantity >= extra.stockQuantity) {
      this.toastService.showError(`Ya tienes el stock máximo disponible (${extra.stockQuantity}) en tu carrito`);
      return;
    }

    this.cartService.addExtra(
      extra.id,
      extra.name,
      extra.price,
      extra.category || 'EXTRA',
      1,
      extra.imageUrl,
      extra.stockQuantity
    );
    this.toastService.showSuccess(`${extra.name} agregado al carrito`);
    this.accessibility.announceAddToCart(extra.name);
  }

  obtenerIconoCategoria(categoria: string): string {
    const cat = this.categorias.find(c => c.value === categoria);
    return cat?.icon || 'bi-grid';
  }

  obtenerNombreCategoria(categoria: string): string {
    const cat = this.categorias.find(c => c.value === categoria);
    return cat?.label || categoria;
  }
}
