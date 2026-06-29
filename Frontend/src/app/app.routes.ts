import { Routes } from '@angular/router';
import { Home } from './componentes/home/home';
import { AdminGuard } from './guards/admin.guard';
import { ClienteGuard } from './guards/cliente.guard';

export const routes: Routes = [
  { path: '', component: Home }, // Eager load para evitar CLS (Cumulative Layout Shift) del Footer
  { path: 'home', component: Home }, // Ruta alternativa para home
  { path: 'join', loadComponent: () => import('./componentes/join/join').then(m => m.Join) },
  { path: 'menu', loadComponent: () => import('./componentes/menu/menu').then(m => m.MenuComponent) },
  { path: 'extras', loadComponent: () => import('./componentes/extras-cliente/extras-cliente').then(m => m.ExtrasClienteComponent) },
  { path: 'promociones', loadComponent: () => import('./componentes/promociones-cliente/promociones-cliente').then(m => m.PromocionesClienteComponent) },
  { path: 'cart', loadComponent: () => import('./componentes/carrito/carrito').then(m => m.CarritoComponent) },
  { path: 'sobre-nosotros', loadComponent: () => import('./componentes/sobre-nosotros/sobre-nosotros').then(m => m.SobreNosotros) },
  { path: 'ubicacion', loadComponent: () => import('./componentes/ubicacion/ubicacion').then(m => m.Ubicacion) },
  { path: 'register', loadComponent: () => import('./componentes/register/register').then(m => m.Register) },
  
  // Rutas de Cliente
  { path: 'cliente/dashboard', loadComponent: () => import('./componentes/cliente-dashboard/cliente-dashboard').then(m => m.ClienteDashboardComponent), canActivate: [ClienteGuard], data: { vista: 'dashboard' } },
  { path: 'cliente/perfil', loadComponent: () => import('./componentes/cliente-dashboard/cliente-dashboard').then(m => m.ClienteDashboardComponent), canActivate: [ClienteGuard], data: { vista: 'perfil' } },
  { path: 'cliente/pedidos', loadComponent: () => import('./componentes/cliente-dashboard/cliente-dashboard').then(m => m.ClienteDashboardComponent), canActivate: [ClienteGuard], data: { vista: 'pedidos' } },
  { path: 'cliente/resenas', loadComponent: () => import('./componentes/cliente-dashboard/cliente-dashboard').then(m => m.ClienteDashboardComponent), canActivate: [ClienteGuard], data: { vista: 'resenas' } },
  { path: 'cliente/favoritos', loadComponent: () => import('./componentes/cliente-dashboard/cliente-dashboard').then(m => m.ClienteDashboardComponent), canActivate: [ClienteGuard], data: { vista: 'favoritos' } },
  { path: 'cliente/carrito', loadComponent: () => import('./componentes/carrito/carrito').then(m => m.CarritoComponent), canActivate: [ClienteGuard] },
  
  // Rutas de Administrador
  { path: 'admin/dashboard', loadComponent: () => import('./componentes/panel-admin/panel-admin').then(m => m.PanelAdmin), canActivate: [AdminGuard] },
  { path: 'panel-admin', loadComponent: () => import('./componentes/panel-admin/panel-admin').then(m => m.PanelAdmin), canActivate: [AdminGuard] },
  { path: 'admin/perfil', loadComponent: () => import('./componentes/panel-admin/panel-admin').then(m => m.PanelAdmin), canActivate: [AdminGuard], data: { vista: 'mi-perfil' } },
  { path: 'usuarios', loadComponent: () => import('./componentes/panel-admin/panel-admin').then(m => m.PanelAdmin), canActivate: [AdminGuard], data: { vista: 'usuarios' } },
  { path: 'pizzas', loadComponent: () => import('./componentes/panel-admin/panel-admin').then(m => m.PanelAdmin), canActivate: [AdminGuard], data: { vista: 'pizzas' } },
  { path: 'categorias', loadComponent: () => import('./componentes/panel-admin/panel-admin').then(m => m.PanelAdmin), canActivate: [AdminGuard], data: { vista: 'categorias' } },
  { path: 'ingredientes', loadComponent: () => import('./componentes/panel-admin/panel-admin').then(m => m.PanelAdmin), canActivate: [AdminGuard], data: { vista: 'ingredientes' } },
  { path: 'sizes', loadComponent: () => import('./componentes/panel-admin/panel-admin').then(m => m.PanelAdmin), canActivate: [AdminGuard], data: { vista: 'sizes' } },
  { path: 'orders', loadComponent: () => import('./componentes/panel-admin/panel-admin').then(m => m.PanelAdmin), canActivate: [AdminGuard], data: { vista: 'orders' } },
  { path: 'payments', loadComponent: () => import('./componentes/panel-admin/panel-admin').then(m => m.PanelAdmin), canActivate: [AdminGuard], data: { vista: 'payments' } },
  { path: 'metodos-pago', loadComponent: () => import('./componentes/panel-admin/panel-admin').then(m => m.PanelAdmin), canActivate: [AdminGuard], data: { vista: 'metodos-pago' } },
  { path: 'reviews', loadComponent: () => import('./componentes/panel-admin/panel-admin').then(m => m.PanelAdmin), canActivate: [AdminGuard], data: { vista: 'reviews' } },
  
  { path: '404', loadComponent: () => import('./componentes/not-found/not-found').then(m => m.NotFoundComponent) }, // Página 404 personalizada
  { path: '**', redirectTo: '/404', pathMatch: 'full' } // Wildcard route - debe ir al final
];