import { Routes } from '@angular/router';
import { Join} from './componentes/join/join';
import { Home } from './componentes/home/home';
import { SobreNosotros } from './componentes/sobre-nosotros/sobre-nosotros';
import { Ubicacion } from './componentes/ubicacion/ubicacion';
import { MenuComponent } from './componentes/menu/menu';
import { Register } from './componentes/register/register';
import { PanelAdmin } from './componentes/panel-admin/panel-admin';
import { Usuarios } from './componentes/usuarios/usuarios';
import { Pizzas } from './componentes/pizzas/pizzas';
import { Categorias } from './componentes/categorias/categorias';
import { Ingredientes } from './componentes/ingredientes/ingredientes';
import { Promociones } from './componentes/promociones/promociones';
import { Sizes } from './componentes/sizes/sizes';
import { Orders } from './componentes/orders/orders';
import { Payments } from './componentes/payments/payments';
import { Reviews } from './componentes/reviews/reviews';
import { MetodosPagoComponent } from './componentes/metodos-pago/metodos-pago';
import { NotFoundComponent } from './componentes/not-found/not-found';
import { AdminGuard } from './guards/admin.guard';
import { ClienteGuard } from './guards/cliente.guard';
import { ClienteDashboardComponent } from './componentes/cliente-dashboard/cliente-dashboard';
import { ClientePerfilComponent } from './componentes/cliente-perfil/cliente-perfil';
import { CarritoComponent } from './componentes/carrito/carrito';
import { FavoritosComponent } from './componentes/favoritos/favoritos';
import { ExtrasClienteComponent } from './componentes/extras-cliente/extras-cliente';
import { PromocionesClienteComponent } from './componentes/promociones-cliente/promociones-cliente';

export const routes: Routes = [
  { path: '', component: Home }, // Página principal con navbar, carousel y footer
  { path: 'home', component: Home }, // Ruta alternativa para home
  { path: 'join', component: Join },
  { path: 'menu', component: MenuComponent },
  { path: 'extras', component: ExtrasClienteComponent },
  { path: 'promociones', component: PromocionesClienteComponent },
  { path: 'cart', component: CarritoComponent },
  { path: 'sobre-nosotros', component: SobreNosotros },
  { path: 'ubicacion', component: Ubicacion },
  { path: 'register', component: Register },
  
  // Rutas de Cliente
  { path: 'cliente/dashboard', component: ClienteDashboardComponent, canActivate: [ClienteGuard], data: { vista: 'dashboard' } },
  { path: 'cliente/perfil', component: ClienteDashboardComponent, canActivate: [ClienteGuard], data: { vista: 'perfil' } },
  { path: 'cliente/pedidos', component: ClienteDashboardComponent, canActivate: [ClienteGuard], data: { vista: 'pedidos' } },
  { path: 'cliente/resenas', component: ClienteDashboardComponent, canActivate: [ClienteGuard], data: { vista: 'resenas' } },
  { path: 'cliente/favoritos', component: ClienteDashboardComponent, canActivate: [ClienteGuard], data: { vista: 'favoritos' } },
  { path: 'cliente/carrito', component: CarritoComponent, canActivate: [ClienteGuard] },
  
  // Rutas de Administrador
  { path: 'admin/dashboard', component: PanelAdmin, canActivate: [AdminGuard] },
  { path: 'panel-admin', component: PanelAdmin, canActivate: [AdminGuard] },
  { path: 'admin/perfil', component: PanelAdmin, canActivate: [AdminGuard], data: { vista: 'mi-perfil' } },
  { path: 'usuarios', component: PanelAdmin, canActivate: [AdminGuard], data: { vista: 'usuarios' } },
  { path: 'pizzas', component: PanelAdmin, canActivate: [AdminGuard], data: { vista: 'pizzas' } },
  { path: 'categorias', component: PanelAdmin, canActivate: [AdminGuard], data: { vista: 'categorias' } },
  { path: 'ingredientes', component: PanelAdmin, canActivate: [AdminGuard], data: { vista: 'ingredientes' } },
  { path: 'sizes', component: PanelAdmin, canActivate: [AdminGuard], data: { vista: 'sizes' } },
  { path: 'orders', component: PanelAdmin, canActivate: [AdminGuard], data: { vista: 'orders' } },
  { path: 'payments', component: PanelAdmin, canActivate: [AdminGuard], data: { vista: 'payments' } },
  { path: 'metodos-pago', component: PanelAdmin, canActivate: [AdminGuard], data: { vista: 'metodos-pago' } },
  { path: 'reviews', component: PanelAdmin, canActivate: [AdminGuard], data: { vista: 'reviews' } },
  
  { path: '404', component: NotFoundComponent }, // Página 404 personalizada
  { path: '**', redirectTo: '/404', pathMatch: 'full' } // Wildcard route - debe ir al final
]