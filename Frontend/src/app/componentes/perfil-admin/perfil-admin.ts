import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../services/auth.service';
import { UsuarioService } from '../../services/usuario.service';
import { UsuarioDTO, UsuarioCreateDTO } from '../../models/usuario.interface';
import { ToastService } from '../../services/toast.service';

@Component({
  selector: 'app-perfil-admin',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './perfil-admin.html',
  styleUrls: ['./perfil-admin.css']
})
export class PerfilAdminComponent implements OnInit {
  usuario: UsuarioDTO | null = null;
  usuarioEditado: UsuarioDTO = {
    id: 0,
    name: '',
    email: '',
    phone: '',
    role: 'ADMIN'
  };

  nuevaPassword: string = '';
  confirmarPassword: string = '';

  showPassword = false;
  showConfirmPassword = false;

  mostrarCambioPassword: boolean = false;
  loadError: string = '';
  mensaje: string = '';
  tipoMensaje: 'success' | 'error' = 'success';
  cargando: boolean = false;
  guardandoPerfil: boolean = false;
  guardandoPassword: boolean = false;

  constructor(
    private readonly authService: AuthService,
    private readonly usuarioService: UsuarioService,
    private readonly toastService: ToastService
  ) {}

  ngOnInit() {
    this.cargarDatosUsuario();
  }

  recargar(): void {
    this.cargarDatosUsuario();
  }

  reintentar(): void {
    this.cargarDatosUsuario();
  }

  cargarDatosUsuario() {
    this.cargando = true;
    this.loadError = '';

    const currentUser = this.authService.getCurrentUser();
    const token = this.authService.getToken();

    if (!token && !currentUser) {
      this.loadError = 'No se encontró una sesión activa de administrador.';
      this.cargando = false;
      return;
    }

    // Usar datos locales primero como fallback instantáneo
    if (currentUser) {
      this.usuario = currentUser;
      this.usuarioEditado = { ...currentUser };
    }

    const tokenData = token ? this.decodeToken(token) : null;
    const userId = currentUser?.id || tokenData?.id;

    if (!userId) {
      this.cargando = false;
      return;
    }

    // Obtener datos frescos desde el backend
    this.usuarioService.obtenerUsuarioPorId(userId).subscribe({
      next: (usuario) => {
        this.usuario = usuario;
        this.usuarioEditado = { ...usuario };
        this.cargando = false;
      },
      error: (error) => {
        console.error('Error al sincronizar perfil con backend:', error);
        if (!this.usuario) {
          this.loadError = error?.status === 0
            ? 'No se pudo conectar con el servidor (Backend fuera de línea). Verifica el puerto 8089.'
            : (error?.error?.message || 'Error al cargar los datos del perfil.');
        }
        this.cargando = false;
      }
    });
  }

  private decodeToken(token: string): any {
    try {
      const payload = token.split('.')[1];
      return JSON.parse(atob(payload));
    } catch {
      return null;
    }
  }

  actualizarDatos() {
    if (!this.validarFormulario() || this.guardandoPerfil) {
      return;
    }

    if (!this.usuarioEditado.id) {
      this.mostrarNotificacion('No se puede actualizar: ID de usuario no válido', 'error');
      return;
    }

    this.guardandoPerfil = true;

    const datosActualizar: UsuarioCreateDTO = {
      email: this.usuarioEditado.email,
      name: this.usuarioEditado.name,
      phone: this.usuarioEditado.phone,
      password: 'dummy_password_not_used', // Ignorado en actualización
      role: this.usuarioEditado.role
    };

    this.usuarioService.actualizarPerfil(this.usuarioEditado.id, datosActualizar).subscribe({
      next: (response) => {
        this.guardandoPerfil = false;
        this.usuario = response;
        this.usuarioEditado = { ...response };
        this.authService.updateCurrentUser(response);
        this.mostrarNotificacion('Datos de perfil actualizados correctamente', 'success');
      },
      error: (error) => {
        this.guardandoPerfil = false;
        console.error('Error al actualizar perfil:', error);
        this.mostrarNotificacion(error?.error?.message || 'Error al actualizar datos de perfil', 'error');
      }
    });
  }

  cambiarPassword() {
    if (this.guardandoPassword) return;

    if (this.nuevaPassword !== this.confirmarPassword) {
      this.mostrarNotificacion('Las contraseñas no coinciden', 'error');
      return;
    }

    if (this.nuevaPassword.length < 6) {
      this.mostrarNotificacion('La contraseña debe tener al menos 6 caracteres', 'error');
      return;
    }

    if (!this.usuarioEditado.id) {
      this.mostrarNotificacion('ID de usuario no válido', 'error');
      return;
    }

    this.guardandoPassword = true;

    this.usuarioService.cambiarPassword(this.usuarioEditado.id, { password: this.nuevaPassword }).subscribe({
      next: () => {
        this.guardandoPassword = false;
        this.nuevaPassword = '';
        this.confirmarPassword = '';
        this.mostrarCambioPassword = false;
        this.toastService.showSuccess('Contraseña cambiada exitosamente. Redirigiendo a inicio de sesión...');
        setTimeout(() => {
          this.authService.logout();
        }, 1500);
      },
      error: (error) => {
        this.guardandoPassword = false;
        console.error('Error al cambiar contraseña:', error);
        this.mostrarNotificacion(error?.error?.message || 'Error al cambiar contraseña', 'error');
      }
    });
  }

  validarFormulario(): boolean {
    if (!this.usuarioEditado.name?.trim() || !this.usuarioEditado.email?.trim()) {
      this.mostrarNotificacion('El nombre y el email son obligatorios', 'error');
      return false;
    }

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(this.usuarioEditado.email)) {
      this.mostrarNotificacion('El correo electrónico ingresado no tiene un formato válido', 'error');
      return false;
    }

    return true;
  }

  cancelarCambioPassword() {
    this.mostrarCambioPassword = false;
    this.nuevaPassword = '';
    this.confirmarPassword = '';
  }

  cancelarEdicion() {
    this.cargarDatosUsuario();
  }

  private mostrarNotificacion(texto: string, tipo: 'success' | 'error'): void {
    this.mensaje = texto;
    this.tipoMensaje = tipo;
    if (tipo === 'success') {
      this.toastService.showSuccess(texto);
      setTimeout(() => (this.mensaje = ''), 4000);
    } else {
      this.toastService.showError(texto);
    }
  }
}
