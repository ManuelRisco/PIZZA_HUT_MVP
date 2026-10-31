import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule, Location } from '@angular/common';
import { AuthService } from '../../services/auth.service';
import { Router, ActivatedRoute } from '@angular/router';
import { Subscription, timer, forkJoin, of } from 'rxjs';
import { catchError, finalize } from 'rxjs/operators';
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
import { PaginationComponent } from '../pagination/pagination';

// Services for dashboard data
import { UsuarioService } from '../../services/usuario.service';
import { Order } from '../../services/order.service';
import { Payment } from '../../services/payment.service';
import { Review } from '../../services/review.service';
import { PizzaService } from '../../services/pizza.service';
import { InventoryService } from '../../services/inventory.service';
import { ToastService } from '../../services/toast.service';
import { OrderCompleteDTO } from '../../models/admin.interface';
import { saveAs } from 'file-saver';

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
    PaginationComponent,
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
  private removeLocationListener?: VoidFunction;

  currentUser: any = null;
  currentUserInitial: string = 'A';

  // Estado de carga y resiliencia en Dashboard Home
  loadingDashboard: boolean = false;
  exportandoExcel: boolean = false;
  errorDashboard: string = '';

  // Propiedades para dashboard
  pedidosRecientes: OrderCompleteDTO[] = [];
  todosLosPedidos: OrderCompleteDTO[] = [];
  masVendidos: any[] = [];
  ventasSemanales: any[] = [];

  // KPI Cards
  ventasHoy: number = 0;
  pedidosHoy: number = 0;
  pedidosPendientes: number = 0;
  totalClientes: number = 0;
  ratingPromedio: number = 0;
  totalReviews: number = 0;
  ventasSemanaTotal: number = 0;
  cambioSemanal: number = 0;
  alertasStockTotal: number = 0;
  valorAlmacenTotal: number = 0;

  // Propiedad y paginación para el modal de pedidos del día
  diaSeleccionado: any = null;
  diaModalPage: number = 1;
  diaModalPerPage: number = 10;
  diaModalPedidosPaginados: OrderCompleteDTO[] = [];

  // Pedido seleccionado para abrir en vista de pedidos
  pedidoSeleccionadoId: number | null = null;

  constructor(
    private readonly authService: AuthService,
    private readonly router: Router,
    private readonly route: ActivatedRoute,
    private readonly location: Location,
    private readonly usuarioService: UsuarioService,
    private readonly orderService: Order,
    private readonly paymentService: Payment,
    private readonly reviewService: Review,
    private readonly pizzaService: PizzaService,
    private readonly inventoryService: InventoryService,
    private readonly toastService: ToastService,
  ) {}

  ngOnInit() {
    this.syncCurrentUser(this.authService.getCurrentUser());
    this.userSubscription = this.authService.currentUser$.subscribe((user) => {
      this.syncCurrentUser(user);
    });

    if (typeof window !== 'undefined') {
      if (window.innerWidth < 768) {
        this.sidebarCollapsed = true;
      } else {
        this.sidebarCollapsed = sessionStorage.getItem('adminSidebarCollapsed') === 'true';
      }
    }

    // Leer el parámetro de vista desde la ruta o URL actual
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
        if (mappedVista === 'dashboard') {
          this.cargarEstadisticas(false);
        }
      }
    });

    this.iniciarAutoRefresh();
  }

  ngOnDestroy() {
    this.pollingSubscription?.unsubscribe();
    this.userSubscription?.unsubscribe();
    if (this.removeLocationListener) {
      this.removeLocationListener();
    }
  }

  private syncCurrentUser(user: any): void {
    this.currentUser = user;
    this.currentUserInitial = user?.name?.trim()?.charAt(0)?.toUpperCase() || 'A';
  }

  getVistaTituloActual(): string {
    return VISTA_TITLES[this.vistaActual] || 'Panel Admin';
  }

  iniciarAutoRefresh(): void {
    // Actualizar cada 60 segundos sin saturar la BD; también carga alertas del sidebar al iniciar
    this.pollingSubscription = timer(0, 60000).subscribe(() => {
      if (this.vistaActual === 'dashboard') {
        this.cargarEstadisticas(false);
      } else {
        this.cargarAlertasSidebarSilencioso();
      }
    });
  }

  private cargarAlertasSidebarSilencioso(): void {
    this.inventoryService.obtenerResumen().subscribe({
      next: (inv) => {
        this.alertasStockTotal = (inv.lowStockCount || 0) + (inv.outOfStockCount || 0);
        this.valorAlmacenTotal = Number(inv.totalInventoryValue || 0);
      },
      error: () => {},
    });
  }

  cargarEstadisticas(manual: boolean = true): void {
    if (manual || this.todosLosPedidos.length === 0) {
      this.loadingDashboard = true;
    }
    if (manual) {
      this.errorDashboard = '';
    }

    let failedPrimary = false;
    let firstErrorStatus = -1;
    let firstErrorMsg = '';

    const captureErr = (err: any, isPrimary: boolean) => {
      if (isPrimary) {
        failedPrimary = true;
        if (firstErrorStatus === -1) {
          firstErrorStatus = err?.status ?? 0;
          firstErrorMsg = err?.error?.message || err?.message || '';
        }
      }
      return of(null);
    };

    forkJoin({
      pedidos: this.orderService
        .obtenerTodosCompletos()
        .pipe(catchError((err) => captureErr(err, true))),
      usuarios: this.usuarioService
        .listarUsuarios()
        .pipe(catchError((err) => captureErr(err, false))),
      reviews: this.reviewService.obtenerTodos().pipe(catchError((err) => captureErr(err, false))),
      inventario: this.inventoryService
        .obtenerResumen()
        .pipe(catchError((err) => captureErr(err, false))),
    })
      .pipe(
        finalize(() => {
          this.loadingDashboard = false;
        }),
      )
      .subscribe((res) => {
        if (failedPrimary || res.pedidos === null) {
          this.errorDashboard =
            firstErrorStatus === 0
              ? 'No se pudo conectar con el servidor (Backend fuera de línea). Verifique que el servicio en el puerto 8089 esté encendido.'
              : firstErrorMsg || 'Ocurrió un problema al sincronizar las métricas del Dashboard.';
        } else {
          this.errorDashboard = '';
        }

        if (Array.isArray(res.pedidos)) {
          const pedidos = res.pedidos;
          this.todosLosPedidos = pedidos;
          const pedidosOrdenados = [...pedidos].toSorted((a, b) => {
            const dateA = a.createdAt ? new Date(a.createdAt).getTime() : 0;
            const dateB = b.createdAt ? new Date(b.createdAt).getTime() : 0;
            return dateB - dateA;
          });
          this.pedidosRecientes = pedidosOrdenados.slice(0, 5);
          this.calcularKPIsPedidos(pedidos);
          this.calcularVentasSemanales(pedidos);
          this.calcularMasVendidos(pedidos);
        }

        if (Array.isArray(res.usuarios)) {
          this.totalClientes = res.usuarios.filter((u) => u.role === 'CUSTOMER').length;
        }

        if (Array.isArray(res.reviews)) {
          const activeReviews = res.reviews.filter((r) => r.active !== false);
          this.totalReviews = activeReviews.length;
          this.ratingPromedio =
            activeReviews.length > 0
              ? activeReviews.reduce((sum, r) => sum + r.rating, 0) / activeReviews.length
              : 0;
        }

        if (res.inventario) {
          this.alertasStockTotal =
            (res.inventario.lowStockCount || 0) + (res.inventario.outOfStockCount || 0);
          this.valorAlmacenTotal = Number(res.inventario.totalInventoryValue || 0);
        }
      });
  }

  calcularKPIsPedidos(pedidos: OrderCompleteDTO[]): void {
    const hoyStr = new Date().toDateString();
    let sumaVentasHoy = 0;
    let conteoHoy = 0;
    let conteoPendientes = 0;

    for (const p of pedidos) {
      if (p.status === 'PENDING' || p.status === 'CONFIRMED' || p.status === 'PREPARING') {
        conteoPendientes++;
      }
      if (p.createdAt) {
        const esHoy = new Date(p.createdAt).toDateString() === hoyStr;
        if (esHoy) {
          conteoHoy++;
          if (p.status === 'DELIVERED') {
            sumaVentasHoy += p.total || 0;
          }
        }
      }
    }

    this.ventasHoy = sumaVentasHoy;
    this.pedidosHoy = conteoHoy;
    this.pedidosPendientes = conteoPendientes;
  }

  calcularVentasSemanales(pedidos: OrderCompleteDTO[]): void {
    const hoy = new Date();
    hoy.setHours(23, 59, 59, 999);

    interface VentaDiaria {
      dia: string;
      fecha: string;
      total: number;
      porcentaje: number;
      pedidos: OrderCompleteDTO[];
    }
    const ventas: VentaDiaria[] = [];
    const ventaPorFecha = new Map<string, VentaDiaria>();
    const dias = ['DOM', 'LUN', 'MAR', 'MIE', 'JUE', 'VIE', 'SAB'];

    for (let i = 6; i >= 0; i--) {
      const fecha = new Date(hoy);
      fecha.setDate(hoy.getDate() - i);
      const fechaKey = fecha.toDateString();
      const entry: VentaDiaria = {
        dia: dias[fecha.getDay()],
        fecha: fechaKey,
        total: 0,
        porcentaje: 0,
        pedidos: [],
      };
      ventas.push(entry);
      ventaPorFecha.set(fechaKey, entry);
    }

    const inicioSemanaAnterior = new Date(hoy);
    inicioSemanaAnterior.setDate(hoy.getDate() - 13);
    inicioSemanaAnterior.setHours(0, 0, 0, 0);
    const finSemanaAnterior = new Date(hoy);
    finSemanaAnterior.setDate(hoy.getDate() - 7);
    finSemanaAnterior.setHours(23, 59, 59, 999);

    let ventasSemanaAnterior = 0;

    for (const pedido of pedidos) {
      if (pedido.createdAt && pedido.status === 'DELIVERED') {
        const fechaPedido = new Date(pedido.createdAt);
        const diaEncontrado = ventaPorFecha.get(fechaPedido.toDateString());
        if (diaEncontrado) {
          diaEncontrado.total += pedido.total || 0;
          diaEncontrado.pedidos.push(pedido);
        } else if (fechaPedido >= inicioSemanaAnterior && fechaPedido <= finSemanaAnterior) {
          ventasSemanaAnterior += pedido.total || 0;
        }
      }
    }

    this.ventasSemanaTotal = ventas.reduce((sum, v) => sum + v.total, 0);

    if (ventasSemanaAnterior > 0) {
      this.cambioSemanal =
        ((this.ventasSemanaTotal - ventasSemanaAnterior) / ventasSemanaAnterior) * 100;
    } else {
      this.cambioSemanal = this.ventasSemanaTotal > 0 ? 100 : 0;
    }

    const maxTotal = Math.max(...ventas.map((v) => v.total), 0);
    for (const v of ventas) {
      v.porcentaje = maxTotal > 0 ? Math.max((v.total / maxTotal) * 100, 5) : 5;
    }

    this.ventasSemanales = ventas;
  }

  calcularMasVendidos(pedidos: OrderCompleteDTO[]): void {
    const conteoProductos: Record<
      string,
      { nombre: string; cantidad: number; precio: number; ingresos: number; porcentaje?: number }
    > = {};

    for (const pedido of pedidos) {
      if (pedido.status !== 'CANCELLED' && pedido.items) {
        for (const item of pedido.items) {
          const nombreProducto = item.pizzaName || item.extraName || 'Producto Desconocido';
          if (!conteoProductos[nombreProducto]) {
            conteoProductos[nombreProducto] = {
              nombre: nombreProducto,
              cantidad: 0,
              precio: item.unitPrice || 0,
              ingresos: 0,
            };
          }
          const qty = item.quantity || 1;
          conteoProductos[nombreProducto].cantidad += qty;
          conteoProductos[nombreProducto].ingresos += (item.unitPrice || 0) * qty;
        }
      }
    }

    const masVendidosArr = Object.values(conteoProductos)
      .toSorted((a, b) => b.cantidad - a.cantidad)
      .slice(0, 5);

    const maxCantidad = masVendidosArr.length > 0 ? masVendidosArr[0].cantidad : 1;
    for (const p of masVendidosArr) {
      p.porcentaje = (p.cantidad / maxCantidad) * 100;
    }

    this.masVendidos = masVendidosArr;
  }

  async descargarReporte(): Promise<void> {
    if (this.todosLosPedidos.length === 0) {
      this.toastService.showWarning('No hay pedidos cargados para exportar.');
      return;
    }
    if (this.exportandoExcel) return;

    this.exportandoExcel = true;
    try {
      const ExcelJS = await import('exceljs');
      const workbook = new ExcelJS.Workbook();
      const worksheet = workbook.addWorksheet('Reporte de Ventas');

      worksheet.mergeCells('A1', 'F1');
      const titleCell = worksheet.getCell('A1');
      titleCell.value = 'REPORTE GENERAL DE VENTAS - PIZZA HUT';
      titleCell.font = { name: 'Arial', size: 16, bold: true, color: { argb: 'FFFFFFFF' } };
      titleCell.fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: 'FFC8102E' } };
      titleCell.alignment = { vertical: 'middle', horizontal: 'center' };
      worksheet.getRow(1).height = 30;

      worksheet.mergeCells('A2', 'F2');
      const subtitleCell = worksheet.getCell('A2');
      subtitleCell.value = `Generado el: ${new Date().toLocaleString()}`;
      subtitleCell.font = { name: 'Arial', size: 11, italic: true, color: { argb: 'FF555555' } };
      subtitleCell.alignment = { vertical: 'middle', horizontal: 'center' };

      worksheet.columns = [
        { key: 'id', width: 15 },
        { key: 'cliente', width: 35 },
        { key: 'correo', width: 35 },
        { key: 'estado', width: 20 },
        { key: 'total', width: 20 },
        { key: 'fecha', width: 25 },
      ];

      const headerRow = worksheet.getRow(3);
      headerRow.values = ['Codigo', 'Nombres', 'Correo', 'Estado', 'Total', 'Hora'];
      headerRow.height = 20;
      headerRow.eachCell((cell) => {
        cell.font = { name: 'Arial', size: 11, bold: true, color: { argb: 'FF000000' } };
        cell.alignment = { vertical: 'middle', horizontal: 'center' };
        cell.border = {
          top: { style: 'thin', color: { argb: 'FF000000' } },
          left: { style: 'thin', color: { argb: 'FF000000' } },
          bottom: { style: 'thin', color: { argb: 'FF000000' } },
          right: { style: 'thin', color: { argb: 'FF000000' } },
        };
      });

      this.todosLosPedidos.forEach((p) => {
        const row = worksheet.addRow({
          id: `PH-${p.id?.toString().padStart(4, '0')}`,
          cliente: p.userName || 'Usuario Anónimo',
          correo: p.userEmail || 'No registrado',
          estado: p.status,
          total: p.total,
          fecha: p.createdAt ? new Date(p.createdAt).toLocaleString() : 'N/A',
        });

        row.height = 20;
        row.eachCell((cell, colNumber) => {
          cell.border = {
            top: { style: 'thin', color: { argb: 'FF000000' } },
            left: { style: 'thin', color: { argb: 'FF000000' } },
            bottom: { style: 'thin', color: { argb: 'FF000000' } },
            right: { style: 'thin', color: { argb: 'FF000000' } },
          };
          cell.alignment = { vertical: 'middle', horizontal: 'center' };
          if (colNumber === 5) {
            cell.numFmt = '"S/" #,##0.00';
            cell.font = { bold: true, color: { argb: 'FF008000' } };
          }
        });
      });

      const buffer = await workbook.xlsx.writeBuffer();
      const blob = new Blob([buffer], {
        type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      });
      saveAs(blob, `Reporte_General_PizzaHut_${Date.now()}.xlsx`);
      this.toastService.showSuccess('Reporte Excel generado correctamente.');
    } catch (err) {
      console.error('Error exportando Excel:', err);
      this.toastService.showError('No se pudo generar el archivo Excel.');
    } finally {
      this.exportandoExcel = false;
    }
  }

  verDetallePedido(pedido: OrderCompleteDTO): void {
    if (!pedido?.id) return;
    this.pedidoSeleccionadoId = pedido.id;
    this.cambiarVista('orders');
  }

  cambiarVista(vista: string): void {
    const changed = this.vistaActual !== vista;
    this.vistaActual = vista;
    sessionStorage.setItem('adminVistaActual', vista);

    if (typeof window !== 'undefined' && window.innerWidth < 768) {
      this.sidebarCollapsed = true;
    }

    const targetPath = VISTA_TO_ROUTE[vista];
    if (targetPath) {
      const fullPath = vista === 'orders' && this.pedidoSeleccionadoId
        ? `${targetPath}?id=${this.pedidoSeleccionadoId}`
        : targetPath;
      if (this.location.path() !== fullPath) {
        this.location.go(fullPath);
      }
    }

    if (changed && vista === 'dashboard') {
      this.cargarEstadisticas(false);
    }
  }

  verPedidosDelDia(dia: any): void {
    if (dia?.pedidos?.length > 0) {
      this.diaSeleccionado = dia;
      this.diaModalPage = 1;
      this.actualizarPaginacionModalDia();
    }
  }

  actualizarPaginacionModalDia(): void {
    if (!this.diaSeleccionado?.pedidos) {
      this.diaModalPedidosPaginados = [];
      return;
    }
    const list: OrderCompleteDTO[] = this.diaSeleccionado.pedidos;
    const totalPages = Math.max(1, Math.ceil(list.length / this.diaModalPerPage));
    if (this.diaModalPage > totalPages) this.diaModalPage = totalPages;
    const start = (this.diaModalPage - 1) * this.diaModalPerPage;
    this.diaModalPedidosPaginados = list.slice(start, start + this.diaModalPerPage);
  }

  onDiaModalPageChange(page: number): void {
    this.diaModalPage = page;
    this.actualizarPaginacionModalDia();
  }

  onDiaModalPerPageChange(size: number): void {
    this.diaModalPerPage = size;
    this.diaModalPage = 1;
    this.actualizarPaginacionModalDia();
  }

  cerrarModalPedidosDia(): void {
    this.diaSeleccionado = null;
    this.diaModalPedidosPaginados = [];
  }

  toggleSidebar(): void {
    this.sidebarCollapsed = !this.sidebarCollapsed;
    if (typeof window !== 'undefined' && window.innerWidth >= 768) {
      sessionStorage.setItem('adminSidebarCollapsed', String(this.sidebarCollapsed));
    }
  }

  cerrarSesion(): void {
    if (confirm('¿Estás seguro de que deseas cerrar sesión?')) {
      sessionStorage.removeItem('adminVistaActual');
      this.authService.logout();
      this.router.navigate(['/join']);
    }
  }

  getCurrentUser() {
    return this.currentUser;
  }

  trackByDia(_index: number, dia: any): string {
    return dia.fecha || dia.dia;
  }

  trackByProducto(_index: number, prod: any): string {
    return prod.nombre;
  }

  trackByPedido(index: number, pedido: OrderCompleteDTO): number {
    return pedido.id ?? index;
  }
}
