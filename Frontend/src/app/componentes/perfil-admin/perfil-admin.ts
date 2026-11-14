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
  styleUrls: ['./perfil-admin.css'],
})
export class PerfilAdminComponent implements OnInit {
  usuario: UsuarioDTO | null = null;
  usuarioEditado: UsuarioDTO = {
    id: 0,
    name: '',
    email: '',
    phone: '',
    role: 'ADMIN',
  };

  passwordActual: string = '';
  nuevaPassword: string = '';
  confirmarPassword: string = '';

  showCurrentPassword = false;
  showPassword = false;
  showConfirmPassword = false;

  editando: boolean = false;
  mostrarCambioPassword: boolean = false;
  loadError: string = '';
  mensaje: string = '';
  tipoMensaje: 'success' | 'error' = 'success';
  cargando: boolean = false;
  guardandoPerfil: boolean = false;
  guardandoPassword: boolean = false;
  vinculandoGoogle: boolean = false;

  constructor(
    private readonly authService: AuthService,
    private readonly usuarioService: UsuarioService,
    private readonly toastService: ToastService,
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
      this.loadError = 'No se encontr\u00f3 una sesi\u00f3n activa de administrador.';
      this.cargando = false;
      return;
    }

    if (currentUser) {
      this.usuario = currentUser;
      this.usuarioEditado = { ...currentUser };
    }

    const userId = currentUser?.id;

    if (!userId) {
      this.cargando = false;
      return;
    }

    this.usuarioService.obtenerUsuarioPorId(userId).subscribe({
      next: (usuario) => {
        const isGoogle = this.authService.isGoogleLinked(usuario);
        this.usuario = {
          ...usuario,
          googleLinked: isGoogle,
          authProvider: isGoogle ? 'GOOGLE' : usuario?.authProvider || 'LOCAL',
        };
        this.usuarioEditado = { ...this.usuario };
        this.authService.updateCurrentUser(this.usuario);
        this.cargando = false;
      },
      error: (error) => {
        console.error('Error al sincronizar perfil con backend:', error);
        if (!this.usuario) {
          this.loadError =
            error?.status === 0
              ? 'No se pudo conectar con el servidor (Backend fuera de l\u00ednea). Verifica el puerto 8089.'
              : error?.error?.message || 'Error al cargar los datos del perfil.';
        }
        this.cargando = false;
      },
    });
  }

  get esCuentaGoogle(): boolean {
    return this.authService.isGoogleLinked(this.usuario);
  }

  get tienePasswordDefinida(): boolean {
    return this.authService.hasPasswordDefined(this.usuario);
  }

  vincularConGoogle(): void {
    if (this.vinculandoGoogle || this.esCuentaGoogle) return;
    this.vinculandoGoogle = true;
    this.authService.vincularCuentaConGoogle().subscribe({
      next: (actualizado) => {
        this.vinculandoGoogle = false;
        this.usuario = {
          ...(this.usuario || this.usuarioEditado),
          ...actualizado,
          googleLinked: true,
          authProvider: 'GOOGLE',
        };
        this.usuarioEditado = { ...this.usuario };
        this.mostrarNotificacion(
          'Cuenta de administrador vinculada con Google correctamente',
          'success',
        );
      },
      error: (err) => {
        this.vinculandoGoogle = false;
        this.mostrarNotificacion(
          err?.error?.message || 'No se pudo vincular la cuenta con Google',
          'error',
        );
      },
    });
  }

  actualizarDatos() {
    if (!this.validarFormulario() || this.guardandoPerfil) {
      return;
    }

    if (!this.usuarioEditado.id) {
      this.mostrarNotificacion('No se puede actualizar: ID de usuario no v\u00e1lido', 'error');
      return;
    }

    this.guardandoPerfil = true;

    const datosActualizar: UsuarioCreateDTO = {
      email: this.usuarioEditado.email,
      name: this.usuarioEditado.name,
      phone: this.usuarioEditado.phone,
      password: 'dummy_password_not_used',
      role: this.usuarioEditado.role,
    };

    this.usuarioService.actualizarPerfil(this.usuarioEditado.id, datosActualizar).subscribe({
      next: (response) => {
        this.guardandoPerfil = false;
        this.editando = false;
        this.usuario = response;
        this.usuarioEditado = { ...response };
        this.authService.updateCurrentUser(response);
        this.mostrarNotificacion('Datos de perfil actualizados correctamente', 'success');
      },
      error: (error) => {
        this.guardandoPerfil = false;
        console.error('Error al actualizar perfil:', error);
        this.mostrarNotificacion(
          error?.error?.message || 'Error al actualizar datos de perfil',
          'error',
        );
      },
    });
  }

  cambiarPassword() {
    if (this.guardandoPassword) return;

    if (this.tienePasswordDefinida && !this.passwordActual.trim()) {
      this.mostrarNotificacion('Debes ingresar tu contrase\u00f1a actual', 'error');
      return;
    }

    if (this.nuevaPassword !== this.confirmarPassword) {
      this.mostrarNotificacion('Las contrase\u00f1as no coinciden', 'error');
      return;
    }

    if (this.nuevaPassword.length < 6) {
      this.mostrarNotificacion('La contrase\u00f1a debe tener al menos 6 caracteres', 'error');
      return;
    }

    if (!this.usuarioEditado.id) {
      this.mostrarNotificacion('ID de usuario no v\u00e1lido', 'error');
      return;
    }

    this.guardandoPassword = true;

    if (!this.tienePasswordDefinida) {
      this.authService.definirPasswordCuentaGoogle(this.nuevaPassword).subscribe({
        next: (actualizado) => {
          this.guardandoPassword = false;
          this.passwordActual = '';
          this.nuevaPassword = '';
          this.confirmarPassword = '';
          this.mostrarCambioPassword = false;
          this.usuario = {
            ...(this.usuario || this.usuarioEditado),
            ...actualizado,
            passwordSet: true,
          };
          this.usuarioEditado = { ...this.usuario };
          this.mostrarNotificacion(
            'Contrase\u00f1a definida correctamente para tu cuenta de Google',
            'success',
          );
        },
        error: (error) => {
          this.guardandoPassword = false;
          this.mostrarNotificacion(
            error?.error?.message || 'Error al definir contrase\u00f1a',
            'error',
          );
        },
      });
      return;
    }

    this.usuarioService
      .cambiarPassword(this.usuarioEditado.id, {
        password: this.nuevaPassword,
        currentPassword: this.passwordActual,
      })
      .subscribe({
        next: () => {
          this.guardandoPassword = false;
          this.passwordActual = '';
          this.nuevaPassword = '';
          this.confirmarPassword = '';
          this.mostrarCambioPassword = false;
          this.toastService.showSuccess(
            'Contrase\u00f1a cambiada exitosamente. Redirigiendo a inicio de sesi\u00f3n...',
          );
          setTimeout(() => {
            this.authService.logout();
          }, 1500);
        },
        error: (error) => {
          this.guardandoPassword = false;
          console.error('Error al cambiar contrase\u00f1a:', error);
          this.mostrarNotificacion(
            error?.error?.message || 'Error al cambiar contrase\u00f1a',
            'error',
          );
        },
      });
  }

  validarFormulario(): boolean {
    if (!this.usuarioEditado.name?.trim() || !this.usuarioEditado.email?.trim()) {
      this.mostrarNotificacion('El nombre y el email son obligatorios', 'error');
      return false;
    }

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(this.usuarioEditado.email)) {
      this.mostrarNotificacion(
        'El correo electr\u00f3nico ingresado no tiene un formato v\u00e1lido',
        'error',
      );
      return false;
    }

    return true;
  }

  toggleEdicion() {
    this.editando = !this.editando;
    if (!this.editando && this.usuario) {
      this.usuarioEditado = { ...this.usuario };
    }
  }

  toggleEdicionPassword() {
    this.mostrarCambioPassword = !this.mostrarCambioPassword;
    if (!this.mostrarCambioPassword) {
      this.passwordActual = '';
      this.nuevaPassword = '';
      this.confirmarPassword = '';
    }
  }

  cancelarCambioPassword() {
    this.mostrarCambioPassword = false;
    this.passwordActual = '';
    this.nuevaPassword = '';
    this.confirmarPassword = '';
  }

  cancelarEdicion() {
    this.editando = false;
    this.cargarDatosUsuario();
  }

  getIniciales(): string {
    const raw = (this.usuario?.name || this.usuarioEditado?.name || 'Admin').trim();
    const primer = raw.split(' ')[0] || 'AD';
    return primer.substring(0, 2).toUpperCase();
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
