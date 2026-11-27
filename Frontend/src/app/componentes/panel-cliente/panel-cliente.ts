import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule, Location } from '@angular/common';
import { Title } from '@angular/platform-browser';
import { Router, ActivatedRoute } from '@angular/router';
import { Subscription, forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { AuthService } from '../../services/auth.service';
import { UsuarioService } from '../../services/usuario.service';
import { Order } from '../../services/order.service';
import { FavoriteService } from '../../services/favorite.service';

import { ClienteSidebarComponent } from '../cliente-sidebar/cliente-sidebar';
import { ClienteDashboardComponent } from '../cliente-dashboard/cliente-dashboard';
import { ClientePerfilComponent } from '../cliente-perfil/cliente-perfil';
import { ClientePedidosComponent } from '../cliente-pedidos/cliente-pedidos';
import { Reviews } from '../reviews/reviews';
import { FavoritosComponent } from '../favoritos/favoritos';

const CLIENTE_VISTA_TO_ROUTE: Record<string, string> = {
  dashboard: '/cliente/dashboard',
  perfil: '/cliente/perfil',
  pedidos: '/cliente/pedidos',
  resenas: '/cliente/resenas',
  favoritos: '/cliente/favoritos',
};

const CLIENTE_ROUTE_TO_VISTA: Record<string, string> = Object.entries(CLIENTE_VISTA_TO_ROUTE).reduce(
  (acc, [vista, path]) => {
    acc[path] = vista;
    return acc;
  },
  { '/panel-cliente': 'dashboard' } as Record<string, string>,
);

const CLIENTE_VISTA_TITLES: Record<string, string> = {
  dashboard: 'Mi Dashboard',
  perfil: 'Mi Perfil',
  pedidos: 'Mis Pedidos',
  resenas: 'Mis Reseñas',
  favoritos: 'Mis Favoritos',
};

@Component({
  selector: 'app-panel-cliente',
  standalone: true,
  imports: [
    CommonModule,
    ClienteSidebarComponent,
    ClienteDashboardComponent,
    ClientePerfilComponent,
    ClientePedidosComponent,
    Reviews,
    FavoritosComponent,
  ],
  templateUrl: './panel-cliente.html',
  styleUrls: ['./panel-cliente.css'],
})
export class PanelClienteComponent implements OnInit, OnDestroy {
  currentUser: any = null;
  primerNombre: string = 'Cliente';
  inicialUsuario: string = 'U';

  vistaActual: string = 'dashboard';
  sidebarCollapsed: boolean = typeof window !== 'undefined' ? window.innerWidth < 768 : false;

  pedidosActivos: number = 0;
  totalFavoritos: number = 0;
  mostrarModalLogout: boolean = false;

  private userSub?: Subscription;
  private removeLocationListener?: VoidFunction;

  constructor(
    private readonly authService: AuthService,
    private readonly usuarioService: UsuarioService,
    private readonly orderService: Order,
    private readonly favoriteService: FavoriteService,
    private readonly router: Router,
    private readonly route: ActivatedRoute,
    private readonly location: Location,
    private readonly titleService: Title,
  ) {}

  ngOnInit(): void {
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
    this.actualizarTituloPagina();

    this.removeLocationListener = this.location.onUrlChange((url) => {
      const pathOnly = (url || '').split('?')[0].split('#')[0];
      const mapped = CLIENTE_ROUTE_TO_VISTA[pathOnly];
      if (mapped && mapped !== this.vistaActual) {
        this.vistaActual = mapped;
        sessionStorage.setItem('clienteVistaActual', mapped);
        this.actualizarTituloPagina();
      }
    });

    this.sincronizarPerfilYBurbujas();
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

  private sincronizarPerfilYBurbujas(): void {
    this.usuarioService.obtenerUsuarioActual().subscribe({
      next: (usuario) => {
        const isGoogle = this.authService.isGoogleLinked(usuario);
        const enriched = {
          ...usuario,
          googleLinked: isGoogle,
          authProvider: isGoogle ? 'GOOGLE' : (usuario?.authProvider || 'LOCAL'),
        };
        this.authService.updateCurrentUser(enriched);
        this.syncUser(enriched);
        if (usuario?.id && this.vistaActual !== 'dashboard') {
          this.cargarContadoresSidebar(Number(usuario.id));
        }
      },
      error: () => {
        const fallbackId = this.currentUser?.id;
        if (fallbackId && this.vistaActual !== 'dashboard') {
          this.cargarContadoresSidebar(Number(fallbackId));
        }
      },
    });
  }

  private cargarContadoresSidebar(userId: number): void {
    forkJoin({
      orders: this.orderService.obtenerPorUsuario(userId).pipe(catchError(() => of([]))),
      favoritos: this.favoriteService.obtenerFavoritosPorUserId(userId).pipe(catchError(() => of([]))),
    }).subscribe(({ orders, favoritos }) => {
      const listaPedidos = Array.isArray(orders) ? orders : [];
      this.pedidosActivos = listaPedidos.filter(
        (o) =>
          o?.status === 'PENDING' ||
          o?.status === 'CONFIRMED' ||
          o?.status === 'PREPARING' ||
          o?.status === 'OUT_FOR_DELIVERY' ||
          o?.status === 'IN_DELIVERY',
      ).length;
      this.totalFavoritos = Array.isArray(favoritos) ? favoritos.length : 0;
    });
  }

  onMetricasDashboardActualizadas(info: { pedidosActivos: number; totalFavoritos: number }): void {
    this.pedidosActivos = info.pedidosActivos;
    this.totalFavoritos = info.totalFavoritos;
  }

  getVistaTituloActual(): string {
    return CLIENTE_VISTA_TITLES[this.vistaActual] || 'Mi Panel';
  }

  private actualizarTituloPagina(): void {
    this.titleService.setTitle(`Eco Pizza - ${this.getVistaTituloActual()}`);
  }

  cambiarVista(vista: string): void {
    this.vistaActual = vista;
    sessionStorage.setItem('clienteVistaActual', vista);
    this.actualizarTituloPagina();

    if (typeof window !== 'undefined' && window.innerWidth < 768) {
      this.sidebarCollapsed = true;
    }

    const ruta = CLIENTE_VISTA_TO_ROUTE[vista];
    if (ruta && this.location.path() !== ruta) {
      this.location.go(ruta);
    }
  }

  toggleSidebar(): void {
    this.sidebarCollapsed = !this.sidebarCollapsed;
    if (typeof window !== 'undefined' && window.innerWidth >= 768) {
      sessionStorage.setItem('clienteSidebarCollapsed', String(this.sidebarCollapsed));
    }
  }

  navegarA(ruta: string): void {
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
}
