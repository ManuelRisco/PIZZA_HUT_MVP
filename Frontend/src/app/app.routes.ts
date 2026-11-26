import { Routes } from '@angular/router';
import { Home } from './componentes/home/home';
import { AdminGuard } from './guards/admin.guard';
import { ClienteGuard } from './guards/cliente.guard';

export const routes: Routes = [
  { path: '', component: Home },
  { path: 'home', component: Home },
  { path: 'join', loadComponent: () => import('./componentes/join/join').then(m => m.Join) },
  { path: 'menu', loadComponent: () => import('./componentes/menu/menu').then(m => m.MenuComponent) },
  { path: 'extras', loadComponent: () => import('./componentes/extras-cliente/extras-cliente').then(m => m.ExtrasClienteComponent) },
  { path: 'promociones', loadComponent: () => import('./componentes/promociones-cliente/promociones-cliente').then(m => m.PromocionesClienteComponent) },
  { path: 'cart', loadComponent: () => import('./componentes/carrito/carrito').then(m => m.CarritoComponent) },
  { path: 'sobre-nosotros', loadComponent: () => import('./componentes/sobre-nosotros/sobre-nosotros').then(m => m.SobreNosotros) },
  { path: 'ubicacion', loadComponent: () => import('./componentes/ubicacion/ubicacion').then(m => m.Ubicacion) },
  { path: 'register', loadComponent: () => import('./componentes/register/register').then(m => m.Register) },
  
  { path: 'panel-cliente', loadComponent: () => import('./componentes/panel-cliente/panel-cliente').then(m => m.PanelClienteComponent), canActivate: [ClienteGuard] },
  { path: 'cliente/dashboard', loadComponent: () => import('./componentes/panel-cliente/panel-cliente').then(m => m.PanelClienteComponent), canActivate: [ClienteGuard], data: { vista: 'dashboard' } },
  { path: 'cliente/perfil', loadComponent: () => import('./componentes/panel-cliente/panel-cliente').then(m => m.PanelClienteComponent), canActivate: [ClienteGuard], data: { vista: 'perfil' } },
  { path: 'cliente/pedidos', loadComponent: () => import('./componentes/panel-cliente/panel-cliente').then(m => m.PanelClienteComponent), canActivate: [ClienteGuard], data: { vista: 'pedidos' } },
  { path: 'cliente/resenas', loadComponent: () => import('./componentes/panel-cliente/panel-cliente').then(m => m.PanelClienteComponent), canActivate: [ClienteGuard], data: { vista: 'resenas' } },
  { path: 'cliente/favoritos', loadComponent: () => import('./componentes/panel-cliente/panel-cliente').then(m => m.PanelClienteComponent), canActivate: [ClienteGuard], data: { vista: 'favoritos' } },
  { path: 'cliente/carrito', loadComponent: () => import('./componentes/carrito/carrito').then(m => m.CarritoComponent), canActivate: [ClienteGuard] },
  
  { path: 'admin/dashboard', loadComponent: () => import('./componentes/panel-admin/panel-admin').then(m => m.PanelAdmin), canActivate: [AdminGuard], data: { vista: 'dashboard' } },
  { path: 'panel-admin', loadComponent: () => import('./componentes/panel-admin/panel-admin').then(m => m.PanelAdmin), canActivate: [AdminGuard] },
  { path: 'admin/perfil', loadComponent: () => import('./componentes/panel-admin/panel-admin').then(m => m.PanelAdmin), canActivate: [AdminGuard], data: { vista: 'mi-perfil' } },
  { path: 'admin/finanzas', loadComponent: () => import('./componentes/panel-admin/panel-admin').then(m => m.PanelAdmin), canActivate: [AdminGuard], data: { vista: 'finanzas' } },
  { path: 'admin/inventario', loadComponent: () => import('./componentes/panel-admin/panel-admin').then(m => m.PanelAdmin), canActivate: [AdminGuard], data: { vista: 'inventario' } },
  { path: 'admin/perdidas', loadComponent: () => import('./componentes/panel-admin/panel-admin').then(m => m.PanelAdmin), canActivate: [AdminGuard], data: { vista: 'perdidas' } },
  { path: 'admin/costos-produccion', loadComponent: () => import('./componentes/panel-admin/panel-admin').then(m => m.PanelAdmin), canActivate: [AdminGuard], data: { vista: 'costos-produccion' } },
  { path: 'admin/extras', loadComponent: () => import('./componentes/panel-admin/panel-admin').then(m => m.PanelAdmin), canActivate: [AdminGuard], data: { vista: 'extras' } },
  { path: 'admin/promociones', loadComponent: () => import('./componentes/panel-admin/panel-admin').then(m => m.PanelAdmin), canActivate: [AdminGuard], data: { vista: 'promociones' } },
  { path: 'usuarios', loadComponent: () => import('./componentes/panel-admin/panel-admin').then(m => m.PanelAdmin), canActivate: [AdminGuard], data: { vista: 'usuarios' } },
  { path: 'pizzas', loadComponent: () => import('./componentes/panel-admin/panel-admin').then(m => m.PanelAdmin), canActivate: [AdminGuard], data: { vista: 'pizzas' } },
  { path: 'categorias', loadComponent: () => import('./componentes/panel-admin/panel-admin').then(m => m.PanelAdmin), canActivate: [AdminGuard], data: { vista: 'categorias' } },
  { path: 'ingredientes', loadComponent: () => import('./componentes/panel-admin/panel-admin').then(m => m.PanelAdmin), canActivate: [AdminGuard], data: { vista: 'ingredientes' } },
  { path: 'sizes', loadComponent: () => import('./componentes/panel-admin/panel-admin').then(m => m.PanelAdmin), canActivate: [AdminGuard], data: { vista: 'sizes' } },
  { path: 'orders', loadComponent: () => import('./componentes/panel-admin/panel-admin').then(m => m.PanelAdmin), canActivate: [AdminGuard], data: { vista: 'orders' } },
  { path: 'admin/delivery', loadComponent: () => import('./componentes/panel-admin/panel-admin').then(m => m.PanelAdmin), canActivate: [AdminGuard], data: { vista: 'delivery' } },
  { path: 'payments', loadComponent: () => import('./componentes/panel-admin/panel-admin').then(m => m.PanelAdmin), canActivate: [AdminGuard], data: { vista: 'payments' } },
  { path: 'metodos-pago', loadComponent: () => import('./componentes/panel-admin/panel-admin').then(m => m.PanelAdmin), canActivate: [AdminGuard], data: { vista: 'metodos-pago' } },
  { path: 'reviews', loadComponent: () => import('./componentes/panel-admin/panel-admin').then(m => m.PanelAdmin), canActivate: [AdminGuard], data: { vista: 'reviews' } },
  
  { path: '404', loadComponent: () => import('./componentes/not-found/not-found').then(m => m.NotFoundComponent) },
  { path: '**', redirectTo: '/404', pathMatch: 'full' }
];
