import { Component, EventEmitter, OnDestroy, OnInit, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { Subscription, forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { AuthService } from '../../services/auth.service';
import { UsuarioService } from '../../services/usuario.service';
import { Order } from '../../services/order.service';
import { FavoriteService } from '../../services/favorite.service';
import { Review } from '../../services/review.service';
import { UiSkeletonComponent } from '../ui-skeleton/ui-skeleton';

@Component({
  selector: 'app-cliente-dashboard',
  standalone: true,
  imports: [CommonModule, UiSkeletonComponent],
  templateUrl: './cliente-dashboard.html',
  styleUrls: ['./cliente-dashboard.css'],
})
export class ClienteDashboardComponent implements OnInit, OnDestroy {
  @Output() vistaChange = new EventEmitter<string>();
  @Output() metricasActualizadas = new EventEmitter<{ pedidosActivos: number; totalFavoritos: number }>();

  usuario: any = null;
  currentUser: any = null;
  primerNombre: string = 'Cliente';

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

  private userSub?: Subscription;

  constructor(
    private readonly authService: AuthService,
    private readonly usuarioService: UsuarioService,
    private readonly orderService: Order,
    private readonly favoriteService: FavoriteService,
    private readonly reviewService: Review,
    private readonly router: Router,
  ) {}

  ngOnInit(): void {
    const initialUser = this.authService.getCurrentUser();
    if (initialUser) {
      this.syncUser(initialUser);
    }

    this.userSub = this.authService.currentUser$.subscribe((u) => {
      if (u) this.syncUser(u);
    });

    this.recargarDashboard();
  }

  ngOnDestroy(): void {
    this.userSub?.unsubscribe();
  }

  private syncUser(u: any): void {
    this.currentUser = u;
    const rawName = (u?.name || '').trim();
    this.primerNombre = rawName ? rawName.split(' ')[0] : 'Cliente';
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
          authProvider: isGoogle ? 'GOOGLE' : (usuario?.authProvider || 'LOCAL'),
        };
        this.authService.updateCurrentUser(this.usuario);
        this.syncUser(this.usuario);
        this.loading = false;
        if (usuario?.id && this.totalPedidos === 0 && !this.loadingMetricas) {
          this.cargarEstadisticasCliente(Number(usuario.id));
        }
      },
      error: (err) => {
        this.usuario = {
          ...this.currentUser,
          active: true,
          createdAt: null,
        };
        this.loadError =
          err?.status === 0
            ? 'El servidor no responde en este momento. Mostrando datos locales de tu sesión.'
            : '';
        this.loading = false;
      },
    });
  }

  cargarEstadisticasCliente(userId: number): void {
    if (!userId) return;
    this.loadingMetricas = true;

    forkJoin({
      orders: this.orderService.obtenerPorUsuario(userId).pipe(catchError(() => of([]))),
      favoritos: this.favoriteService.obtenerFavoritosPorUserId(userId).pipe(catchError(() => of([]))),
      resenas: this.reviewService.obtenerPorUsuario(userId).pipe(catchError(() => of([]))),
    }).subscribe({
      next: ({ orders, favoritos, resenas }) => {
        const listaPedidos = Array.isArray(orders) ? orders : [];
        const ordenados = [...listaPedidos].sort((a, b) => {
          const tA = a?.createdAt ? new Date(a.createdAt).getTime() : 0;
          const tB = b?.createdAt ? new Date(b.createdAt).getTime() : 0;
          return tB - tA || Number(b?.id || 0) - Number(a?.id || 0);
        });

        this.totalPedidos = ordenados.length;
        this.pedidosActivos = ordenados.filter(
          (o) =>
            o?.status === 'PENDING' ||
            o?.status === 'CONFIRMED' ||
            o?.status === 'PREPARING' ||
            o?.status === 'OUT_FOR_DELIVERY' ||
            o?.status === 'IN_DELIVERY',
        ).length;

        this.gastoTotal = ordenados
          .filter((o) => o?.status !== 'CANCELLED')
          .reduce((acc, o) => acc + Number(o?.total || 0), 0);

        this.pedidosRecientes = ordenados.slice(0, 5);

        const listaFavs = Array.isArray(favoritos) ? favoritos : [];
        this.totalFavoritos = listaFavs.length;

        const listaReviews = Array.isArray(resenas) ? resenas : [];
        this.totalResenas = listaReviews.length;
        this.promedioResenas =
          listaReviews.length > 0
            ? listaReviews.reduce((acc, r) => acc + Number(r?.rating || 0), 0) / listaReviews.length
            : 0;

        this.metricasActualizadas.emit({
          pedidosActivos: this.pedidosActivos,
          totalFavoritos: this.totalFavoritos,
        });
        this.loadingMetricas = false;
      },
      error: () => {
        this.loadingMetricas = false;
      },
    });
  }

  getEstadoTexto(status?: string): string {
    switch (status) {
      case 'PENDING':
        return 'Pendiente';
      case 'CONFIRMED':
        return 'Confirmado';
      case 'PREPARING':
        return 'Preparando';
      case 'OUT_FOR_DELIVERY':
      case 'IN_DELIVERY':
        return 'En camino';
      case 'DELIVERED':
        return 'Entregado';
      case 'CANCELLED':
        return 'Cancelado';
      default:
        return status || 'Pendiente';
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

  cambiarVista(vista: string): void {
    this.vistaChange.emit(vista);
  }

  navegarA(ruta: string): void {
    this.router.navigate([ruta]);
  }
}
