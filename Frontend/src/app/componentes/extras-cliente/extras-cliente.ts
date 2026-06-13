import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ExtraService, Extra } from '../../services/extra.service';
import { CartService } from '../../services/cart.service';
import { AuthService } from '../../services/auth.service';
import { AccessibilityService } from '../../services/accessibility.service';
import { ToastService } from '../../services/toast.service';

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

  private extraService = inject(ExtraService);
  private cartService = inject(CartService);
  private authService = inject(AuthService);
  private accessibility = inject(AccessibilityService);
  private toastService = inject(ToastService);

  ngOnInit(): void {
    this.cargarExtras();
  }

  cargarExtras(): void {
    this.loading = true;
    this.error = '';

    this.extraService.listarDisponibles().subscribe({
      next: (data) => {
        this.extras = data.sort((a, b) => (a.displayOrder || 0) - (b.displayOrder || 0));
        this.aplicarFiltros();
        this.loading = false;
      },
      error: (err) => {
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

    // Filtrar por categoría
    if (this.categoriaSeleccionada !== 'TODOS') {
      resultado = resultado.filter(e => e.category === this.categoriaSeleccionada);
    }

    // Filtrar por búsqueda
    if (this.searchTerm.trim()) {
      const term = this.searchTerm.toLowerCase();
      resultado = resultado.filter(e => 
        e.name.toLowerCase().includes(term) ||
        (e.description && e.description.toLowerCase().includes(term))
      );
    }

    this.extrasFiltrados = resultado;
  }

  limpiarBuscador(): void {
    this.searchTerm = '';
    this.aplicarFiltros();
  }

  agregarAlCarrito(extra: Extra): void {
    if (!extra.id || !extra.price) return;

    this.cartService.addExtra(
      extra.id, 
      extra.name, 
      extra.price, 
      extra.category || 'EXTRA',
      1
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
