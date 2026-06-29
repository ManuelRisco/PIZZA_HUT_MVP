import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../services/auth.service';
import { Router, ActivatedRoute } from '@angular/router';
import { Subscription, timer } from 'rxjs';
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

// Services for dashboard data
import { UsuarioService } from '../../services/usuario.service';
import { Order } from '../../services/order.service';
import { Payment } from '../../services/payment.service';
import { Review } from '../../services/review.service';
import { PizzaService } from '../../services/pizza.service';
import { OrderCompleteDTO } from '../../models/admin.interface';
import * as ExcelJS from 'exceljs';
import { saveAs } from 'file-saver';

@Component({
  selector: 'app-panel-admin',
  standalone: true,
  imports: [
    CommonModule,
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
    Promociones
  ],
  templateUrl: './panel-admin.html',
  styleUrls: ['./panel-admin.css']
})
export class PanelAdmin implements OnInit, OnDestroy {
  vistaActual: string = 'dashboard';
  sidebarCollapsed: boolean = false;
  private pollingSubscription?: Subscription;

  // Nuevas propiedades para dashboard
  pedidosRecientes: OrderCompleteDTO[] = [];
  todosLosPedidos: OrderCompleteDTO[] = [];
  masVendidos: any[] = [];
  ventasSemanales: any[] = [];
  
  // Propiedad para el modal de pedidos del día
  diaSeleccionado: any = null;

  constructor(
    private authService: AuthService, 
    private router: Router,
    private route: ActivatedRoute,
    private usuarioService: UsuarioService,
    private orderService: Order,
    private paymentService: Payment,
    private reviewService: Review,
    private pizzaService: PizzaService
  ) {}

  ngOnInit() {
    // Leer el parámetro de vista desde la ruta
    const vistaFromRoute = this.route.snapshot.data['vista'];
    if (vistaFromRoute) {
      this.vistaActual = vistaFromRoute;
      // Guardar la vista en sessionStorage
      sessionStorage.setItem('adminVistaActual', vistaFromRoute);
    } else {
      // Si no hay parámetro de vista, intentar restaurar desde sessionStorage
      const vistaGuardada = sessionStorage.getItem('adminVistaActual');
      if (vistaGuardada) {
        this.vistaActual = vistaGuardada;
      }
    }

    this.iniciarAutoRefresh();
  }

  ngOnDestroy() {
    if (this.pollingSubscription) {
      this.pollingSubscription.unsubscribe();
    }
  }

  iniciarAutoRefresh(): void {
    // Actualizar cada 15 segundos sin recargar la página
    this.pollingSubscription = timer(0, 15000).subscribe(() => {
      if (this.vistaActual === 'dashboard') {
        this.cargarEstadisticas();
      }
    });
  }

  cargarEstadisticas(): void {
    const hoyStr = new Date().toDateString();

    // Cargar pedidos recientes, ventas semanales y más vendidos
    this.orderService.obtenerTodosCompletos().subscribe({
      next: (pedidos) => {
        this.todosLosPedidos = pedidos;
        // Pedidos Recientes
        // Ordenamos por fecha de forma descendente y tomamos los 5 últimos
        const pedidosOrdenados = [...pedidos].sort((a, b) => {
          const dateA = a.createdAt ? new Date(a.createdAt).getTime() : 0;
          const dateB = b.createdAt ? new Date(b.createdAt).getTime() : 0;
          return dateB - dateA;
        });
        this.pedidosRecientes = pedidosOrdenados.slice(0, 5);

        // Ventas Semanales
        this.calcularVentasSemanales(pedidos);

        // Más Vendidos
        this.calcularMasVendidos(pedidos);
      },
      error: (err) => console.error('Error al cargar pedidos completos:', err)
    });
  }

  calcularVentasSemanales(pedidos: OrderCompleteDTO[]): void {
    const hoy = new Date();
    hoy.setHours(23, 59, 59, 999); // Final del día actual
    
    interface VentaDiaria {
      dia: string;
      fecha: string;
      total: number;
      porcentaje: number;
      pedidos: OrderCompleteDTO[];
    }
    const ventas: VentaDiaria[] = [];
    const dias = ['DOM', 'LUN', 'MAR', 'MIE', 'JUE', 'VIE', 'SAB'];
    
    // Inicializar los últimos 7 días
    for (let i = 6; i >= 0; i--) {
      const fecha = new Date(hoy);
      fecha.setDate(hoy.getDate() - i);
      ventas.push({
        dia: dias[fecha.getDay()],
        fecha: fecha.toDateString(),
        total: 0,
        porcentaje: 0,
        pedidos: []
      });
    }

    // Sumar totales por día (sólo pedidos entregados)
    pedidos.forEach(pedido => {
      if (pedido.createdAt && pedido.status === 'DELIVERED') {
        const fechaPedido = new Date(pedido.createdAt);
        // Verificar si pertenece a los últimos 7 días
        const diffTime = Math.abs(hoy.getTime() - fechaPedido.getTime());
        const diffDays = Math.floor(diffTime / (1000 * 60 * 60 * 24));
        
        if (diffDays < 7) {
          // Encontrar el día correcto en nuestro arreglo
          const fechaStr = fechaPedido.toDateString();
          const diaEncontrado = ventas.find(v => v.fecha === fechaStr);
          if (diaEncontrado) {
            diaEncontrado.total += (pedido.total || 0);
            diaEncontrado.pedidos.push(pedido);
          }
        }
      }
    });

    // Calcular porcentajes para la altura de las barras CSS
    const maxTotal = Math.max(...ventas.map(v => v.total));
    ventas.forEach(v => {
      // Usar 5% mínimo para que la barra se vea si es 0
      v.porcentaje = maxTotal > 0 ? Math.max((v.total / maxTotal) * 100, 5) : 5;
    });

    this.ventasSemanales = ventas;
  }

  calcularMasVendidos(pedidos: OrderCompleteDTO[]): void {
    const conteoProductos: { [key: string]: { nombre: string, cantidad: number, precio: number } } = {};

    pedidos.forEach(pedido => {
      if (pedido.status !== 'CANCELLED' && pedido.items) {
        pedido.items.forEach(item => {
          const nombreProducto = item.pizzaName || item.extraName || 'Producto Desconocido';
          if (!conteoProductos[nombreProducto]) {
            conteoProductos[nombreProducto] = {
              nombre: nombreProducto,
              cantidad: 0,
              precio: item.unitPrice || 0
            };
          }
          conteoProductos[nombreProducto].cantidad += (item.quantity || 1);
        });
      }
    });

    // Convertir objeto a array y ordenar de mayor a menor
    const masVendidosArr = Object.values(conteoProductos)
      .sort((a, b) => b.cantidad - a.cantidad)
      .slice(0, 3); // Top 3

    this.masVendidos = masVendidosArr;
  }

  async descargarReporte(): Promise<void> {
    if (this.todosLosPedidos.length === 0) {
      alert('No hay datos para exportar.');
      return;
    }

    const workbook = new ExcelJS.Workbook();
    const worksheet = workbook.addWorksheet('Reporte de Ventas');

    // 1. Título Principal
    worksheet.mergeCells('A1', 'F1');
    const titleCell = worksheet.getCell('A1');
    titleCell.value = 'REPORTE GENERAL DE VENTAS - PIZZA HUT';
    titleCell.font = { name: 'Arial', size: 16, bold: true, color: { argb: 'FFFFFFFF' } };
    titleCell.fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: 'FFC8102E' } }; // Rojo
    titleCell.alignment = { vertical: 'middle', horizontal: 'center' };
    worksheet.getRow(1).height = 30;

    // 2. Subtítulo con fecha
    worksheet.mergeCells('A2', 'F2');
    const subtitleCell = worksheet.getCell('A2');
    subtitleCell.value = `Generado el: ${new Date().toLocaleString()}`;
    subtitleCell.font = { name: 'Arial', size: 11, italic: true, color: { argb: 'FF555555' } };
    subtitleCell.alignment = { vertical: 'middle', horizontal: 'center' }; // Ahora centrado como en la imagen
    
    // 3. Definir Columnas (sin propiedad header para que no sobreescriba la Fila 1)
    worksheet.columns = [
      { key: 'id', width: 15 },
      { key: 'cliente', width: 35 },
      { key: 'correo', width: 35 },
      { key: 'estado', width: 20 },
      { key: 'total', width: 20 },
      { key: 'fecha', width: 25 }
    ];

    // 4. Estilo de encabezados de la tabla (Fila 3)
    const headerRow = worksheet.getRow(3);
    headerRow.values = ['Codigo', 'Nombres', 'Correo', 'Estado', 'Total', 'Hora'];
    headerRow.height = 20;
    headerRow.eachCell((cell) => {
      cell.font = { name: 'Arial', size: 11, color: { argb: 'FF000000' } }; // Texto negro sin negrita extrema
      cell.alignment = { vertical: 'middle', horizontal: 'center' };
      cell.border = {
        top: { style: 'thin', color: { argb: 'FF000000' } }, left: { style: 'thin', color: { argb: 'FF000000' } },
        bottom: { style: 'thin', color: { argb: 'FF000000' } }, right: { style: 'thin', color: { argb: 'FF000000' } }
      };
    });

    // 5. Agregar filas y dar formato a los datos
    this.todosLosPedidos.forEach(p => {
      const row = worksheet.addRow({
        id: `PH-${p.id?.toString().padStart(4, '0')}`,
        cliente: p.userName || 'Usuario Anónimo',
        correo: p.userEmail || 'No registrado',
        estado: p.status,
        total: p.total,
        fecha: p.createdAt ? new Date(p.createdAt).toLocaleString() : 'N/A'
      });

      row.height = 20;
      row.eachCell((cell, colNumber) => {
        // Bordes finos negros para todas las celdas de datos
        cell.border = {
          top: { style: 'thin', color: { argb: 'FF000000' } }, left: { style: 'thin', color: { argb: 'FF000000' } },
          bottom: { style: 'thin', color: { argb: 'FF000000' } }, right: { style: 'thin', color: { argb: 'FF000000' } }
        };
        
        // Todo centrado
        cell.alignment = { vertical: 'middle', horizontal: 'center' };

        // Formato específico para la columna Total
        if (colNumber === 5) {
          cell.numFmt = '"S/" #,##0.00';
          cell.font = { bold: true, color: { argb: 'FF008000' } }; // Verde
        }
      });
    });

    // 6. Generar archivo binario y descargar
    const buffer = await workbook.xlsx.writeBuffer();
    const blob = new Blob([buffer], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' });
    saveAs(blob, `Reporte_General_PizzaHut_${new Date().getTime()}.xlsx`);
  }

  cambiarVista(vista: string): void {
    this.vistaActual = vista;
    // Guardar la vista actual en sessionStorage al cambiar
    sessionStorage.setItem('adminVistaActual', vista);
  }

  verPedidosDelDia(dia: any): void {
    if (dia.pedidos && dia.pedidos.length > 0) {
      this.diaSeleccionado = dia;
    }
  }

  cerrarModalPedidosDia(): void {
    this.diaSeleccionado = null;
  }

  toggleSidebar(): void {
    this.sidebarCollapsed = !this.sidebarCollapsed;
  }

  cerrarSesion(): void {
    if (confirm('¿Estás seguro de que deseas cerrar sesión?')) {
      // Limpiar la vista guardada
      sessionStorage.removeItem('adminVistaActual');
      this.authService.logout();
      this.router.navigate(['/join']);
    }
  }

  getCurrentUser() {
    return this.authService.getCurrentUser();
  }
}
