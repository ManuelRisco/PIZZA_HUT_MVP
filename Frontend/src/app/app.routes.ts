import { Routes } from '@angular/router';
import { Home } from './componentes/home/home';
import { AdminGuard } from './guards/admin.guard';
import { ClienteGuard } from './guards/cliente.guard';

export const routes: Routes = [
  { path: '', component: Home, title: 'Inicio' },
  { path: 'home', component: Home, title: 'Inicio' },
  { path: 'join', loadComponent: () => import('./componentes/join/join').then(m => m.Join), title: 'Iniciar Sesión' },
  { path: 'menu', loadComponent: () => import('./componentes/menu/menu').then(m => m.MenuComponent), title: 'Menú' },
  { path: 'extras', loadComponent: () => import('./componentes/extras-cliente/extras-cliente').then(m => m.ExtrasClienteComponent), title: 'Extras' },
  { path: 'promociones', loadComponent: () => import('./componentes/promociones-cliente/promociones-cliente').then(m => m.PromocionesClienteComponent), title: 'Promociones' },
  { path: 'cart', loadComponent: () => import('./componentes/carrito/carrito').then(m => m.CarritoComponent), title: 'Carrito' },
  { path: 'sobre-nosotros', loadComponent: () => import('./componentes/sobre-nosotros/sobre-nosotros').then(m => m.SobreNosotros), title: 'Sobre Nosotros' },
  { path: 'ubicacion', loadComponent: () => import('./componentes/ubicacion/ubicacion').then(m => m.Ubicacion), title: 'Ubicación' },
  { path: 'register', loadComponent: () => import('./componentes/register/register').then(m => m.Register), title: 'Registro' },

  { path: 'panel-cliente', loadComponent: () => import('./componentes/panel-cliente/panel-cliente').then(m => m.PanelClienteComponent), canActivate: [ClienteGuard], title: 'Mi Dashboard' },
  { path: 'cliente/dashboard', loadComponent: () => import('./componentes/panel-cliente/panel-cliente').then(m => m.PanelClienteComponent), canActivate: [ClienteGuard], data: { vista: 'dashboard' }, title: 'Mi Dashboard' },
  { path: 'cliente/perfil', loadComponent: () => import('./componentes/panel-cliente/panel-cliente').then(m => m.PanelClienteComponent), canActivate: [ClienteGuard], data: { vista: 'perfil' }, title: 'Mi Perfil' },
  { path: 'cliente/pedidos', loadComponent: () => import('./componentes/panel-cliente/panel-cliente').then(m => m.PanelClienteComponent), canActivate: [ClienteGuard], data: { vista: 'pedidos' }, title: 'Mis Pedidos' },
  { path: 'cliente/resenas', loadComponent: () => import('./componentes/panel-cliente/panel-cliente').then(m => m.PanelClienteComponent), canActivate: [ClienteGuard], data: { vista: 'resenas' }, title: 'Mis Reseñas' },
  { path: 'cliente/favoritos', loadComponent: () => import('./componentes/panel-cliente/panel-cliente').then(m => m.PanelClienteComponent), canActivate: [ClienteGuard], data: { vista: 'favoritos' }, title: 'Mis Favoritos' },
  { path: 'cliente/carrito', loadComponent: () => import('./componentes/carrito/carrito').then(m => m.CarritoComponent), canActivate: [ClienteGuard], title: 'Carrito' },

  { path: 'admin/dashboard', loadComponent: () => import('./componentes/panel-admin/panel-admin').then(m => m.PanelAdmin), canActivate: [AdminGuard], data: { vista: 'dashboard' }, title: 'Dashboard Ejecutivo' },
  { path: 'panel-admin', loadComponent: () => import('./componentes/panel-admin/panel-admin').then(m => m.PanelAdmin), canActivate: [AdminGuard], title: 'Dashboard Ejecutivo' },
  { path: 'admin/perfil', loadComponent: () => import('./componentes/panel-admin/panel-admin').then(m => m.PanelAdmin), canActivate: [AdminGuard], data: { vista: 'mi-perfil' }, title: 'Mi Perfil' },
  { path: 'admin/finanzas', loadComponent: () => import('./componentes/panel-admin/panel-admin').then(m => m.PanelAdmin), canActivate: [AdminGuard], data: { vista: 'finanzas' }, title: 'Pérdidas y Ganancias' },
  { path: 'admin/inventario', loadComponent: () => import('./componentes/panel-admin/panel-admin').then(m => m.PanelAdmin), canActivate: [AdminGuard], data: { vista: 'inventario' }, title: 'Stock e Inventario' },
  { path: 'admin/perdidas', loadComponent: () => import('./componentes/panel-admin/panel-admin').then(m => m.PanelAdmin), canActivate: [AdminGuard], data: { vista: 'perdidas' }, title: 'Pérdidas de Productos' },
  { path: 'admin/costos-produccion', loadComponent: () => import('./componentes/panel-admin/panel-admin').then(m => m.PanelAdmin), canActivate: [AdminGuard], data: { vista: 'costos-produccion' }, title: 'Costos de Producción' },
  { path: 'admin/extras', loadComponent: () => import('./componentes/panel-admin/panel-admin').then(m => m.PanelAdmin), canActivate: [AdminGuard], data: { vista: 'extras' }, title: 'Extras' },
  { path: 'admin/promociones', loadComponent: () => import('./componentes/panel-admin/panel-admin').then(m => m.PanelAdmin), canActivate: [AdminGuard], data: { vista: 'promociones' }, title: 'Promociones' },
  { path: 'usuarios', loadComponent: () => import('./componentes/panel-admin/panel-admin').then(m => m.PanelAdmin), canActivate: [AdminGuard], data: { vista: 'usuarios' }, title: 'Usuarios' },
  { path: 'pizzas', loadComponent: () => import('./componentes/panel-admin/panel-admin').then(m => m.PanelAdmin), canActivate: [AdminGuard], data: { vista: 'pizzas' }, title: 'Pizzas y Recetas' },
  { path: 'categorias', loadComponent: () => import('./componentes/panel-admin/panel-admin').then(m => m.PanelAdmin), canActivate: [AdminGuard], data: { vista: 'categorias' }, title: 'Categorías' },
  { path: 'ingredientes', loadComponent: () => import('./componentes/panel-admin/panel-admin').then(m => m.PanelAdmin), canActivate: [AdminGuard], data: { vista: 'ingredientes' }, title: 'Ingredientes' },
  { path: 'sizes', loadComponent: () => import('./componentes/panel-admin/panel-admin').then(m => m.PanelAdmin), canActivate: [AdminGuard], data: { vista: 'sizes' }, title: 'Tamaños' },
  { path: 'orders', loadComponent: () => import('./componentes/panel-admin/panel-admin').then(m => m.PanelAdmin), canActivate: [AdminGuard], data: { vista: 'orders' }, title: 'Pedidos' },
  { path: 'admin/notas-credito', loadComponent: () => import('./componentes/panel-admin/panel-admin').then(m => m.PanelAdmin), canActivate: [AdminGuard], data: { vista: 'notas-credito' }, title: 'Notas de Crédito' },
  { path: 'admin/delivery', loadComponent: () => import('./componentes/panel-admin/panel-admin').then(m => m.PanelAdmin), canActivate: [AdminGuard], data: { vista: 'delivery' }, title: 'Tarifa de Delivery' },
  { path: 'payments', loadComponent: () => import('./componentes/panel-admin/panel-admin').then(m => m.PanelAdmin), canActivate: [AdminGuard], data: { vista: 'payments' }, title: 'Pagos' },
  { path: 'metodos-pago', loadComponent: () => import('./componentes/panel-admin/panel-admin').then(m => m.PanelAdmin), canActivate: [AdminGuard], data: { vista: 'metodos-pago' }, title: 'Métodos de Pago' },
  { path: 'reviews', loadComponent: () => import('./componentes/panel-admin/panel-admin').then(m => m.PanelAdmin), canActivate: [AdminGuard], data: { vista: 'reviews' }, title: 'Reseñas' },

  { path: '404', loadComponent: () => import('./componentes/not-found/not-found').then(m => m.NotFoundComponent), title: 'Página no encontrada' },
  { path: '**', redirectTo: '/404', pathMatch: 'full' }
];
