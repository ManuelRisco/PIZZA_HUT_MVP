import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule, Location } from '@angular/common';
import { Router, ActivatedRoute } from '@angular/router';
import { Subscription } from 'rxjs';
import { AuthService } from '../../services/auth.service';
import { UsuarioService } from '../../services/usuario.service';
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
  'resenas': 'Mis Reseñas',
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
  loadError: string = '';

  private userSub?: Subscription;
  private removeLocationListener?: VoidFunction;

  constructor(
    private readonly authService: AuthService,
    private readonly usuarioService: UsuarioService,
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

    this.cargarUsuarioActual();
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

  cargarUsuarioActual(): void {
    this.loading = true;
    this.loadError = '';
    this.usuarioService.obtenerUsuarioActual().subscribe({
      next: (usuario) => {
        this.usuario = usuario;
        this.syncUser(usuario);
        this.loading = false;
      },
      error: (err) => {
        console.error('Error al cargar información del usuario:', err);
        this.usuario = {
          ...this.currentUser,
          active: true,
          createdAt: null
        };
        this.loadError = err?.status === 0
          ? 'El servidor no responde en este momento. Mostrando datos locales de tu sesión.'
          : '';
        this.loading = false;
      }
    });
  }

  getVistaTituloActual(): string {
    return CLIENTE_VISTA_TITLES[this.vistaActual] || 'Mi Panel';
  }

  cambiarVista(vista: string): void {
    this.vistaActual = vista;
    sessionStorage.setItem('clienteVistaActual', vista);

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

  navegarA(ruta: string) {
    this.router.navigate([ruta]);
  }

  cerrarSesion(): void {
    if (confirm('¿Estás seguro de que deseas cerrar sesión?')) {
      sessionStorage.removeItem('clienteVistaActual');
      this.authService.logout();
      this.router.navigate(['/']);
    }
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
