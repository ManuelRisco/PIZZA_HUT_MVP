import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../services/auth.service';
import { Router, ActivatedRoute } from '@angular/router';
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
export class PanelAdmin implements OnInit {
  vistaActual: string = 'dashboard';
  sidebarCollapsed: boolean = false;

  constructor(
    private authService: AuthService, 
    private router: Router,
    private route: ActivatedRoute
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
  }

  cambiarVista(vista: string): void {
    this.vistaActual = vista;
    // Guardar la vista actual en sessionStorage al cambiar
    sessionStorage.setItem('adminVistaActual', vista);
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
