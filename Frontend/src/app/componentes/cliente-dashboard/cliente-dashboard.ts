import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, ActivatedRoute } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { UsuarioService } from '../../services/usuario.service';
import { ClientePerfilComponent } from '../cliente-perfil/cliente-perfil';
import { ClientePedidosComponent } from '../cliente-pedidos/cliente-pedidos';
import { Reviews } from '../reviews/reviews';
import { FavoritosComponent } from '../favoritos/favoritos';

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
export class ClienteDashboardComponent implements OnInit {
  usuario: any = null;
  usuarioCompleto: any = null;
  vistaActual: string = 'dashboard';
  sidebarCollapsed: boolean = false;
  loading: boolean = true;

  constructor(
    private authService: AuthService,
    private usuarioService: UsuarioService,
    private router: Router,
    private route: ActivatedRoute
  ) {}

  ngOnInit() {
    const currentUser = this.authService.getCurrentUser();
    if (!currentUser) {
      this.router.navigate(['/']);
      return;
    }

    // Leer el parámetro de vista desde la ruta
    const vistaFromRoute = this.route.snapshot.data['vista'];
    
    // Siempre usar la vista de la ruta si existe, y guardarla
    if (vistaFromRoute) {
      this.vistaActual = vistaFromRoute;
      sessionStorage.setItem('clienteVistaActual', vistaFromRoute);
    } else {
      // Si no hay parámetro de ruta, usar la vista por defecto
      this.vistaActual = 'dashboard';
      sessionStorage.setItem('clienteVistaActual', 'dashboard');
    }

    // Obtener información completa del usuario desde el backend
    this.usuarioService.obtenerUsuarioActual().subscribe({
      next: (usuario) => {
        this.usuario = usuario;
        this.loading = false;
      },
      error: (error) => {
        console.error('Error al cargar información del usuario:', error);
        // Fallback: usar datos del token si falla la petición
        this.usuario = {
          ...currentUser,
          active: true,
          createdAt: null
        };
        this.loading = false;
      }
    });
  }

  cambiarVista(vista: string): void {
    // Navegar a la URL correspondiente en lugar de solo cambiar la vista local
    const rutaMap: { [key: string]: string } = {
      'dashboard': '/cliente/dashboard',
      'perfil': '/cliente/perfil',
      'pedidos': '/cliente/pedidos',
      'resenas': '/cliente/resenas',
      'favoritos': '/cliente/favoritos'
    };
    
    const ruta = rutaMap[vista];
    if (ruta) {
      this.router.navigate([ruta]);
    }
  }

  toggleSidebar(): void {
    this.sidebarCollapsed = !this.sidebarCollapsed;
  }

  navegarA(ruta: string) {
    this.router.navigate([ruta]);
  }

  cerrarSesion(): void {
    if (confirm('¿Estás seguro de que deseas cerrar sesión?')) {
      // Limpiar la vista guardada
      sessionStorage.removeItem('clienteVistaActual');
      this.authService.logout();
      this.router.navigate(['/']);
    }
  }

  getCurrentUser() {
    return this.authService.getCurrentUser();
  }

  getIniciales(): string {
    if (!this.usuario?.name) return 'U';
    
    // Obtener solo el primer nombre
    const primerNombre = this.usuario.name.trim().split(' ')[0];
    
    // Retornar las primeras 2 letras del primer nombre en mayúsculas
    return primerNombre.substring(0, 2).toUpperCase();
  }
}
