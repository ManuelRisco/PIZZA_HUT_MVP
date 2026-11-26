import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule, Location } from '@angular/common';
import { Router, ActivatedRoute } from '@angular/router';
import { Subscription, timer } from 'rxjs';
import { AuthService } from '../../services/auth.service';
import { UsuarioService } from '../../services/usuario.service';
import { InventoryService } from '../../services/inventory.service';
import { StoreSettingsService } from '../../services/store-settings.service';
import { CartService } from '../../services/cart.service';
import { OrderCompleteDTO } from '../../models/admin.interface';

import { AdminSidebarComponent } from '../admin-sidebar/admin-sidebar';
import { AdminDashboardComponent } from '../admin-dashboard/admin-dashboard';
import { DeliveryConfigComponent } from '../delivery-config/delivery-config';
import { Usuarios } from '../usuarios/usuarios';
import { Pizzas } from '../pizzas/pizzas';
import { Categorias } from '../categorias/categorias';
import { Ingredientes } from '../ingredientes/ingredientes';
import { Sizes } from '../sizes/sizes';
import { Orders } from '../orders/orders';
import { Payments } from '../payments/payments';
import { MetodosPagoComponent } from '../metodos-pago/metodos-pago';
import { Reviews } from '../reviews/reviews';
import { PerfilAdminComponent } from '../perfil-admin/perfil-admin';
import { ExtrasComponent } from '../extras/extras';
import { Promociones } from '../promociones/promociones';
import { InventarioComponent } from '../inventario/inventario';
import { PerdidasComponent } from '../perdidas/perdidas';
import { CostosProduccionComponent } from '../costos-produccion/costos-produccion';
import { FinanzasDashboardComponent } from '../finanzas-dashboard/finanzas-dashboard';

const VISTA_TO_ROUTE: Record<string, string> = {
  dashboard: '/admin/dashboard',
  finanzas: '/admin/finanzas',
  inventario: '/admin/inventario',
  perdidas: '/admin/perdidas',
  'costos-produccion': '/admin/costos-produccion',
  usuarios: '/usuarios',
  pizzas: '/pizzas',
  categorias: '/categorias',
  ingredientes: '/ingredientes',
  sizes: '/sizes',
  extras: '/admin/extras',
  promociones: '/admin/promociones',
  orders: '/orders',
  delivery: '/admin/delivery',
  payments: '/payments',
  'metodos-pago': '/metodos-pago',
  reviews: '/reviews',
  'mi-perfil': '/admin/perfil',
};

const ROUTE_TO_VISTA: Record<string, string> = Object.entries(VISTA_TO_ROUTE).reduce(
  (acc, [vista, path]) => {
    acc[path] = vista;
    return acc;
  },
  { '/panel-admin': 'dashboard' } as Record<string, string>,
);

const VISTA_TITLES: Record<string, string> = {
  dashboard: 'Dashboard Ejecutivo',
  finanzas: 'Pérdidas y Ganancias',
  inventario: 'Stock e Inventario',
  perdidas: 'Pérdidas de Productos',
  'costos-produccion': 'Costos de Producción',
  usuarios: 'Usuarios',
  pizzas: 'Pizzas y Recetas',
  categorias: 'Categorías',
  ingredientes: 'Ingredientes',
  sizes: 'Tamaños',
  extras: 'Extras',
  promociones: 'Promociones',
  orders: 'Pedidos',
  delivery: 'Tarifa de Delivery',
  payments: 'Pagos',
  'metodos-pago': 'Métodos de Pago',
  reviews: 'Reseñas',
  'mi-perfil': 'Mi Perfil',
};

@Component({
  selector: 'app-panel-admin',
  standalone: true,
  imports: [
    CommonModule,
    AdminSidebarComponent,
    AdminDashboardComponent,
    DeliveryConfigComponent,
    Usuarios,
    Pizzas,
    Categorias,
    Ingredientes,
    Sizes,
    Orders,
    Payments,
    MetodosPagoComponent,
    Reviews,
    PerfilAdminComponent,
    ExtrasComponent,
    Promociones,
    InventarioComponent,
    PerdidasComponent,
    CostosProduccionComponent,
    FinanzasDashboardComponent,
  ],
  templateUrl: './panel-admin.html',
  styleUrls: ['./panel-admin.css'],
})
export class PanelAdmin implements OnInit, OnDestroy {
  vistaActual: string = 'dashboard';
  sidebarCollapsed: boolean = typeof window !== 'undefined' ? window.innerWidth < 768 : false;

  private pollingSubscription?: Subscription;
  private userSubscription?: Subscription;
  private deliveryFeeSubscription?: Subscription;
  private removeLocationListener?: VoidFunction;

  currentUser: any = null;
  currentUserInitial: string = 'A';

  alertasStockTotal: number = 0;
  deliveryFeeActual: number = 5.0;
  pedidoSeleccionadoId: number | null = null;
  mostrarModalLogout: boolean = false;

  constructor(
    private readonly authService: AuthService,
    private readonly router: Router,
    private readonly route: ActivatedRoute,
    private readonly location: Location,
    private readonly usuarioService: UsuarioService,
    private readonly inventoryService: InventoryService,
    private readonly storeSettingsService: StoreSettingsService,
    private readonly cartService: CartService,
  ) {}

  ngOnInit() {
    this.syncCurrentUser(this.authService.getCurrentUser());
    this.userSubscription = this.authService.currentUser$.subscribe((user) => {
      this.syncCurrentUser(user);
    });
    this.usuarioService.obtenerUsuarioActual().subscribe({
      next: (u) => {
        if (u) {
          this.authService.updateCurrentUser(u);
        }
      },
      error: () => {},
    });

    if (typeof window !== 'undefined') {
      if (window.innerWidth < 768) {
        this.sidebarCollapsed = true;
      } else {
        this.sidebarCollapsed = sessionStorage.getItem('adminSidebarCollapsed') === 'true';
      }
    }

    const cleanPath = (this.location.path() || '').split('?')[0].split('#')[0];
    const vistaFromRoute = this.route.snapshot.data['vista'] || ROUTE_TO_VISTA[cleanPath];
    if (vistaFromRoute) {
      this.vistaActual = vistaFromRoute;
      sessionStorage.setItem('adminVistaActual', vistaFromRoute);
    } else {
      const vistaGuardada = sessionStorage.getItem('adminVistaActual');
      if (vistaGuardada && VISTA_TO_ROUTE[vistaGuardada]) {
        this.vistaActual = vistaGuardada;
      }
    }

    this.removeLocationListener = this.location.onUrlChange((url) => {
      const pathOnly = (url || '').split('?')[0].split('#')[0];
      const mappedVista = ROUTE_TO_VISTA[pathOnly];
      if (mappedVista && mappedVista !== this.vistaActual) {
        this.vistaActual = mappedVista;
        sessionStorage.setItem('adminVistaActual', mappedVista);
      }
    });

    this.deliveryFeeSubscription = this.storeSettingsService.deliveryFee$.subscribe((fee) => {
      this.deliveryFeeActual = fee;
      this.cartService.setDeliveryFeeRate(fee);
    });
    this.storeSettingsService.loadDeliveryFee().subscribe();

    this.pollingSubscription = timer(0, 60000).subscribe(() => {
      if (typeof document !== 'undefined' && document.hidden) {
        return;
      }
      if (this.vistaActual !== 'dashboard') {
        this.cargarAlertasSidebarSilencioso();
      }
    });
  }

  ngOnDestroy() {
    this.pollingSubscription?.unsubscribe();
    this.userSubscription?.unsubscribe();
    this.deliveryFeeSubscription?.unsubscribe();
    if (this.removeLocationListener) {
      this.removeLocationListener();
    }
  }

  private syncCurrentUser(user: any): void {
    this.currentUser = user;
    this.currentUserInitial = user?.name?.trim()?.charAt(0)?.toUpperCase() || 'A';
  }

  private cargarAlertasSidebarSilencioso(): void {
    this.inventoryService.obtenerResumen().subscribe({
      next: (inv) => {
        this.alertasStockTotal = (inv.lowStockCount || 0) + (inv.outOfStockCount || 0);
      },
      error: () => {},
    });
  }

  onInventarioDashboardActualizado(info: { alertas: number; valorAlmacen: number }): void {
    this.alertasStockTotal = info.alertas;
  }

  getVistaTituloActual(): string {
    return VISTA_TITLES[this.vistaActual] || 'Panel Admin';
  }

  verDetallePedido(pedido: OrderCompleteDTO): void {
    if (!pedido?.id) return;
    this.pedidoSeleccionadoId = pedido.id;
    this.cambiarVista('orders');
  }

  cambiarVista(vista: string): void {
    this.vistaActual = vista;
    sessionStorage.setItem('adminVistaActual', vista);

    if (typeof window !== 'undefined' && window.innerWidth < 768) {
      this.sidebarCollapsed = true;
    }

    const targetPath = VISTA_TO_ROUTE[vista];
    if (targetPath) {
      const fullPath =
        vista === 'orders' && this.pedidoSeleccionadoId
          ? `${targetPath}?id=${this.pedidoSeleccionadoId}`
          : targetPath;
      if (this.location.path() !== fullPath) {
        this.location.go(fullPath);
      }
    }
  }

  toggleSidebar(): void {
    this.sidebarCollapsed = !this.sidebarCollapsed;
    if (typeof window !== 'undefined' && window.innerWidth >= 768) {
      sessionStorage.setItem('adminSidebarCollapsed', String(this.sidebarCollapsed));
    }
  }

  cerrarSesion(): void {
    this.mostrarModalLogout = true;
  }

  cancelarLogout(): void {
    this.mostrarModalLogout = false;
  }

  confirmarLogout(): void {
    this.mostrarModalLogout = false;
    sessionStorage.removeItem('adminVistaActual');
    this.authService.logout();
    this.router.navigate(['/join']);
  }

  getCurrentUser() {
    return this.currentUser;
  }
}
