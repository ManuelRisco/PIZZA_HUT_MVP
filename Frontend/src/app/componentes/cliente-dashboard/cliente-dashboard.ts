import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule, Location } from '@angular/common';
import { Router, ActivatedRoute } from '@angular/router';
import { Subscription, forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { AuthService } from '../../services/auth.service';
import { UsuarioService } from '../../services/usuario.service';
import { Order } from '../../services/order.service';
import { FavoriteService } from '../../services/favorite.service';
import { Review } from '../../services/review.service';
import { ClientePerfilComponent } from '../cliente-perfil/cliente-perfil';
import { ClientePedidosComponent } from '../cliente-pedidos/cliente-pedidos';
import { Reviews } from '../reviews/reviews';
import { FavoritosComponent } from '../favoritos/favoritos';

const CLIENTE_VISTA_TO_ROUTE: Record<string, string> = {
  'dashboard': '/cliente/dashboard',
  'perfil': '/cliente/perfil',
  'pedidos': '/cliente/pedidos',
  'resenas': '/cliente/resenas',
  'favoritos': '/cliente/favoritos'
};

const CLIENTE_ROUTE_TO_VISTA: Record<string, string> = Object.entries(CLIENTE_VISTA_TO_ROUTE).reduce(
  (acc, [vista, path]) => {
    acc[path] = vista;
    return acc;
  },
  {} as Record<string, string>
);

const CLIENTE_VISTA_TITLES: Record<string, string> = {
  'dashboard': 'Mi Dashboard',
  'perfil': 'Mi Perfil',
  'pedidos': 'Mis Pedidos',
  'resenas': 'Mis Rese\u00f1as',
  'favoritos': 'Mis Favoritos'
};

@Component({
  selector: 'app-cliente-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    ClientePerfilComponent,
    ClientePedidosComponent,
    Reviews,
    FavoritosComponent
  ],
  templateUrl: './cliente-dashboard.html',
  styleUrls: ['./cliente-dashboard.css']
})
export class ClienteDashboardComponent implements OnInit, OnDestroy {
  usuario: any = null;
  usuarioCompleto: any = null;
  currentUser: any = null;
  primerNombre: string = 'Cliente';
  inicialUsuario: string = 'U';

  vistaActual: string = 'dashboard';
  sidebarCollapsed: boolean = typeof window !== 'undefined' ? window.innerWidth < 768 : false;
  loading: boolean = true;
  loadingMetricas: boolean = false;
  loadError: string = '';

  totalPedidos: number = 0;
  pedidosActivos: number = 0;
  gastoTotal: number = 0;
  totalFavoritos: number = 0;
  totalResenas: number = 0;
  promedioResenas: number = 0;
  pedidosRecientes: any[] = [];

  mostrarModalLogout: boolean = false;

  private userSub?: Subscription;
  private removeLocationListener?: VoidFunction;

  constructor(
    private readonly authService: AuthService,
    private readonly usuarioService: UsuarioService,
    private readonly orderService: Order,
    private readonly favoriteService: FavoriteService,
    private readonly reviewService: Review,
    private readonly router: Router,
    private readonly route: ActivatedRoute,
    private readonly location: Location
  ) {}

  ngOnInit() {
    const initialUser = this.authService.getCurrentUser();
    if (!initialUser) {
      this.router.navigate(['/']);
      return;
    }
    this.syncUser(initialUser);

    this.userSub = this.authService.currentUser$.subscribe((u) => {
      if (u) this.syncUser(u);
    });

    if (typeof window !== 'undefined') {
      if (window.innerWidth < 768) {
        this.sidebarCollapsed = true;
      } else {
        this.sidebarCollapsed = sessionStorage.getItem('clienteSidebarCollapsed') === 'true';
      }
    }

    const cleanPath = (this.location.path() || '').split('?')[0].split('#')[0];
    const vistaFromRoute = this.route.snapshot.data['vista'] || CLIENTE_ROUTE_TO_VISTA[cleanPath];
    if (vistaFromRoute) {
      this.vistaActual = vistaFromRoute;
      sessionStorage.setItem('clienteVistaActual', vistaFromRoute);
    } else {
      const saved = sessionStorage.getItem('clienteVistaActual');
      this.vistaActual = saved && CLIENTE_VISTA_TO_ROUTE[saved] ? saved : 'dashboard';
      sessionStorage.setItem('clienteVistaActual', this.vistaActual);
    }

    this.removeLocationListener = this.location.onUrlChange((url) => {
      const pathOnly = (url || '').split('?')[0].split('#')[0];
      const mapped = CLIENTE_ROUTE_TO_VISTA[pathOnly];
      if (mapped && mapped !== this.vistaActual) {
        this.vistaActual = mapped;
        sessionStorage.setItem('clienteVistaActual', mapped);
      }
    });

    this.recargarDashboard();
  }

  ngOnDestroy(): void {
    this.userSub?.unsubscribe();
    if (this.removeLocationListener) {
      this.removeLocationListener();
    }
  }

  private syncUser(u: any): void {
    this.currentUser = u;
    const rawName = (u?.name || '').trim();
    this.primerNombre = rawName ? rawName.split(' ')[0] : 'Cliente';
    this.inicialUsuario = rawName ? rawName.charAt(0).toUpperCase() : 'U';
  }

  recargarDashboard(): void {
    this.cargarUsuarioActual();
    const userId = this.currentUser?.id || this.authService.getCurrentUser()?.id;
    if (userId) {
      this.cargarEstadisticasCliente(Number(userId));
    }
  }

  cargarUsuarioActual(): void {
    this.loading = true;
    this.loadError = '';
    this.usuarioService.obtenerUsuarioActual().subscribe({
      next: (usuario) => {
        const isGoogle = this.authService.isGoogleLinked(usuario);
        this.usuario = {
          ...usuario,
          googleLinked: isGoogle,
          authProvider: isGoogle ? 'GOOGLE' : (usuario?.authProvider || 'LOCAL')
        };
        this.authService.updateCurrentUser(this.usuario);
        this.syncUser(this.usuario);
        this.loading = false;
        if (usuario?.id && this.totalPedidos === 0 && !this.loadingMetricas) {
          this.cargarEstadisticasCliente(Number(usuario.id));
        }
      },
      error: (err) => {
        console.error('Error al cargar informaci\u00f3n del usuario:', err);
        this.usuario = {
          ...this.currentUser,
          active: true,
          createdAt: null
        };
        this.loadError = err?.status === 0
          ? 'El servidor no responde en este momento. Mostrando datos locales de tu sesi\u00f3n.'
          : '';
        this.loading = false;
      }
    });
  }

  cargarEstadisticasCliente(userId: number): void {
    if (!userId) return;
    this.loadingMetricas = true;

    forkJoin({
      orders: this.orderService.obtenerPorUsuario(userId).pipe(catchError(() => of([]))),
      favoritos: this.favoriteService.obtenerFavoritosPorUserId(userId).pipe(catchError(() => of([]))),
      resenas: this.reviewService.obtenerPorUsuario(userId).pipe(catchError(() => of([])))
    }).subscribe({
      next: ({ orders, favoritos, resenas }) => {
        const listaPedidos = Array.isArray(orders) ? orders : [];
        const ordenados = [...listaPedidos].sort((a, b) => {
          const tA = a?.createdAt ? new Date(a.createdAt).getTime() : 0;
          const tB = b?.createdAt ? new Date(b.createdAt).getTime() : 0;
          return tB - tA || (Number(b?.id || 0) - Number(a?.id || 0));
        });

        this.totalPedidos = ordenados.length;
        this.pedidosActivos = ordenados.filter((o) =>
          o?.status === 'PENDING' ||
          o?.status === 'CONFIRMED' ||
          o?.status === 'PREPARING' ||
          o?.status === 'OUT_FOR_DELIVERY' ||
          o?.status === 'IN_DELIVERY'
        ).length;

        this.gastoTotal = ordenados
          .filter((o) => o?.status !== 'CANCELLED')
          .reduce((acc, o) => acc + Number(o?.total || 0), 0);

        this.pedidosRecientes = ordenados.slice(0, 5);

        const listaFavs = Array.isArray(favoritos) ? favoritos : [];
        this.totalFavoritos = listaFavs.length;

        const listaReviews = Array.isArray(resenas) ? resenas : [];
        this.totalResenas = listaReviews.length;
        this.promedioResenas = listaReviews.length > 0
          ? listaReviews.reduce((acc, r) => acc + Number(r?.rating || 0), 0) / listaReviews.length
          : 0;

        this.loadingMetricas = false;
      },
      error: () => {
        this.loadingMetricas = false;
      }
    });
  }

  getEstadoTexto(status?: string): string {
    switch (status) {
      case 'PENDING': return 'Pendiente';
      case 'CONFIRMED': return 'Confirmado';
      case 'PREPARING': return 'Preparando';
      case 'OUT_FOR_DELIVERY':
      case 'IN_DELIVERY': return 'En camino';
      case 'DELIVERED': return 'Entregado';
      case 'CANCELLED': return 'Cancelado';
      default: return status || 'Pendiente';
    }
  }

  esPickup(order: any): boolean {
    if (!order) return false;
    const dt = String(order.deliveryType || '').toUpperCase();
    return dt === 'PICKUP' || (!order.addressId && !order.addressLine1);
  }

  trackByPedido(index: number, pedido: any): number {
    return pedido?.id ?? index;
  }

  getVistaTituloActual(): string {
    return CLIENTE_VISTA_TITLES[this.vistaActual] || 'Mi Panel';
  }

  cambiarVista(vista: string): void {
    const changed = this.vistaActual !== vista;
    this.vistaActual = vista;
    sessionStorage.setItem('clienteVistaActual', vista);

    if (typeof window !== 'undefined' && window.innerWidth < 768) {
      this.sidebarCollapsed = true;
    }

    const ruta = CLIENTE_VISTA_TO_ROUTE[vista];
    if (ruta && this.location.path() !== ruta) {
      this.location.go(ruta);
    }

    if (changed && vista === 'dashboard') {
      const userId = this.currentUser?.id || this.usuario?.id;
      if (userId) {
        this.cargarEstadisticasCliente(Number(userId));
      }
    }
  }

  toggleSidebar(): void {
    this.sidebarCollapsed = !this.sidebarCollapsed;
    if (typeof window !== 'undefined' && window.innerWidth >= 768) {
      sessionStorage.setItem('clienteSidebarCollapsed', String(this.sidebarCollapsed));
    }
  }

  navegarA(ruta: string) {
    this.router.navigate([ruta]);
  }

  cerrarSesion(): void {
    this.mostrarModalLogout = true;
  }

  cancelarLogout(): void {
    this.mostrarModalLogout = false;
  }

  confirmarLogout(): void {
    this.mostrarModalLogout = false;
    sessionStorage.removeItem('clienteVistaActual');
    this.authService.logout();
    this.router.navigate(['/']);
  }

  getCurrentUser() {
    return this.currentUser;
  }

  getIniciales(): string {
    if (!this.usuario?.name) return this.inicialUsuario;
    const primer = this.usuario.name.trim().split(' ')[0];
    return primer.substring(0, 2).toUpperCase();
  }
}
