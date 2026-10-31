import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { finalize } from 'rxjs/operators';
import { AuthService } from '../../services/auth.service';
import { UsuarioService } from '../../services/usuario.service';

@Component({
  selector: 'app-cliente-perfil',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './cliente-perfil.html',
  styleUrls: ['./cliente-perfil.css']
})
export class ClientePerfilComponent implements OnInit {
  usuario: any = null;
  editando = false;
  mensajeExito = '';
  mensajeError = '';
  loadError = '';
  loading = true;
  guardandoPerfil = false;
  guardandoPassword = false;

  editandoPassword = false;
  passwordActual: string = '';
  nuevaPassword: string = '';
  confirmarPassword: string = '';

  showPasswordActual = false;
  showNuevaPassword = false;
  showConfirmarPassword = false;

  mensajeSeguridadExito = '';
  mensajeSeguridadError = '';

  constructor(
    private readonly authService: AuthService,
    private readonly usuarioService: UsuarioService,
    private readonly router: Router
  ) {}

  ngOnInit() {
    const currentUser = this.authService.getCurrentUser();
    if (!currentUser) {
      this.router.navigate(['/']);
      return;
    }
    this.cargarDatosUsuario();
  }

  toggleEdicion() {
    this.editando = !this.editando;
    if (!this.editando) {
      this.cargarDatosUsuario();
    }
  }

  cargarDatosUsuario() {
    const currentUser = this.authService.getCurrentUser();
    this.loading = true;
    this.loadError = '';

    this.usuarioService.obtenerUsuarioActual()
      .pipe(finalize(() => { this.loading = false; }))
      .subscribe({
        next: (usuario) => {
          this.usuario = usuario;
        },
        error: (error) => {
          console.error('Error al cargar información del usuario:', error);
          this.usuario = currentUser
            ? { ...currentUser, active: true, createdAt: null }
            : null;
          this.loadError = error?.status === 0
            ? 'No se pudo conectar con el servidor (Backend fuera de línea). Mostrando información en caché de tu sesión.'
            : (error?.error?.message || 'No se pudo sincronizar tu perfil con el servidor.');
        }
      });
  }

  guardarCambios() {
    if (this.guardandoPerfil) return;
    if (!this.usuario?.id) {
      this.mensajeError = 'Error: No se pudo identificar el usuario';
      setTimeout(() => this.mensajeError = '', 3500);
      return;
    }

    if (!this.usuario.name || this.usuario.name.trim() === '') {
      this.mensajeError = 'El nombre es obligatorio';
      setTimeout(() => this.mensajeError = '', 3500);
      return;
    }

    if (!this.usuario.email || this.usuario.email.trim() === '') {
      this.mensajeError = 'El email es obligatorio';
      setTimeout(() => this.mensajeError = '', 3500);
      return;
    }

    const datosActualizados = {
      name: this.usuario.name.trim(),
      email: this.usuario.email.trim(),
      phone: this.usuario.phone && this.usuario.phone.trim() !== '' ? this.usuario.phone.trim() : null
    };

    this.guardandoPerfil = true;
    this.usuarioService.actualizarPerfil(this.usuario.id, datosActualizados)
      .pipe(finalize(() => { this.guardandoPerfil = false; }))
      .subscribe({
        next: (usuarioActualizado) => {
          this.usuario = usuarioActualizado;
          this.mensajeExito = 'Perfil actualizado correctamente';
          this.editando = false;
          this.authService.updateCurrentUser(usuarioActualizado);
          setTimeout(() => this.mensajeExito = '', 3500);
        },
        error: (error) => {
          console.error('Error al actualizar perfil:', error);
          if (error.status === 0) {
            this.mensajeError = 'Sin conexión con el servidor. Inténtalo nuevamente cuando el servicio esté en línea.';
          } else if (error.status === 400) {
            this.mensajeError = error.error?.message || 'Datos inválidos';
          } else if (error.status === 409) {
            this.mensajeError = 'El correo electrónico ya está en uso';
          } else {
            this.mensajeError = error.error?.message || 'Error al actualizar el perfil';
          }
          setTimeout(() => this.mensajeError = '', 5000);
        }
      });
  }

  getIniciales(): string {
    if (!this.usuario?.name) return 'U';
    const primerNombre = this.usuario.name.trim().split(' ')[0];
    return primerNombre.substring(0, 2).toUpperCase();
  }

  toggleEdicionPassword() {
    this.editandoPassword = !this.editandoPassword;
    if (!this.editandoPassword) {
      this.limpiarFormularioPassword();
    }
  }

  limpiarFormularioPassword() {
    this.passwordActual = '';
    this.nuevaPassword = '';
    this.confirmarPassword = '';
    this.mensajeSeguridadError = '';
  }

  cambiarPassword() {
    if (this.guardandoPassword || !this.usuario?.id) return;

    if (!this.passwordActual || !this.nuevaPassword || !this.confirmarPassword) {
      this.mensajeSeguridadError = 'Todos los campos son obligatorios';
      setTimeout(() => this.mensajeSeguridadError = '', 3500);
      return;
    }

    if (this.nuevaPassword !== this.confirmarPassword) {
      this.mensajeSeguridadError = 'Las contraseñas nuevas no coinciden';
      setTimeout(() => this.mensajeSeguridadError = '', 3500);
      return;
    }

    if (this.nuevaPassword.length < 6) {
      this.mensajeSeguridadError = 'La nueva contraseña debe tener al menos 6 caracteres';
      setTimeout(() => this.mensajeSeguridadError = '', 3500);
      return;
    }

    this.guardandoPassword = true;
    this.usuarioService.cambiarPassword(this.usuario.id, { password: this.nuevaPassword })
      .pipe(finalize(() => { this.guardandoPassword = false; }))
      .subscribe({
        next: () => {
          this.mensajeExito = 'Contraseña actualizada correctamente. Por seguridad, vuelve a iniciar sesión.';
          this.editandoPassword = false;
          setTimeout(() => {
            this.authService.logout();
          }, 2000);
        },
        error: (error) => {
          console.error('Error al cambiar contraseña:', error);
          this.mensajeSeguridadError = error?.status === 0
            ? 'Sin conexión con el servidor. No se pudo actualizar la contraseña.'
            : (error?.error?.message || 'Error al actualizar la contraseña');
          setTimeout(() => this.mensajeSeguridadError = '', 5000);
        }
      });
  }

  volverDashboard() {
    this.router.navigate(['/cliente/dashboard']);
  }
}
