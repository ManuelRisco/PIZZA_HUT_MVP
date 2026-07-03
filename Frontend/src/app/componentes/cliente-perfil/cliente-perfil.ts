import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
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
  loading = true;

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

    // Obtener informaciÃ³n completa del usuario desde el backend
    this.usuarioService.obtenerUsuarioActual().subscribe({
      next: (usuario) => {
        this.usuario = usuario;
        this.loading = false;
      },
      error: (error) => {
        console.error('Error al cargar informaciÃ³n del usuario:', error);
        // Fallback: usar datos del token si falla la peticiÃ³n
        this.usuario = {
          ...currentUser,
          active: true,
          createdAt: null
        };
        this.loading = false;
      }
    });
  }

  toggleEdicion() {
    this.editando = !this.editando;
    // Si cancela la ediciÃ³n, recargar los datos originales
    if (!this.editando) {
      this.cargarDatosUsuario();
    }
  }

  cargarDatosUsuario() {
    this.loading = true;
    this.usuarioService.obtenerUsuarioActual().subscribe({
      next: (usuario) => {
        this.usuario = usuario;
        this.loading = false;
      },
      error: (error) => {
        console.error('Error al cargar informaciÃ³n del usuario:', error);
        const currentUser = this.authService.getCurrentUser();
        this.usuario = {
          ...currentUser,
          active: true,
          createdAt: null
        };
        this.loading = false;
      }
    });
  }

  guardarCambios() {
    if (!this.usuario?.id) {
      this.mensajeError = 'Error: No se pudo identificar el usuario';
      setTimeout(() => this.mensajeError = '', 3000);
      return;
    }

    // Validaciones
    if (!this.usuario.name || this.usuario.name.trim() === '') {
      this.mensajeError = 'El nombre es obligatorio';
      setTimeout(() => this.mensajeError = '', 3000);
      return;
    }

    if (!this.usuario.email || this.usuario.email.trim() === '') {
      this.mensajeError = 'El email es obligatorio';
      setTimeout(() => this.mensajeError = '', 3000);
      return;
    }

    // Preparar datos para actualizar
    const datosActualizados = {
      name: this.usuario.name.trim(),
      email: this.usuario.email.trim(),
      phone: this.usuario.phone && this.usuario.phone.trim() !== '' ? this.usuario.phone.trim() : null
    };

    this.loading = true;
    this.usuarioService.actualizarPerfil(this.usuario.id, datosActualizados).subscribe({
      next: (usuarioActualizado) => {
        this.usuario = usuarioActualizado;
        this.mensajeExito = 'Perfil actualizado correctamente';
        this.editando = false;
        this.loading = false;
        
        // Actualizar el token con los nuevos datos
        this.authService.updateCurrentUser(usuarioActualizado);
        
        setTimeout(() => this.mensajeExito = '', 3000);
      },
      error: (error) => {
        console.error('Error al actualizar perfil:', error);
        this.loading = false;
        
        if (error.status === 400) {
          this.mensajeError = error.error?.message || 'Datos invÃ¡lidos';
        } else if (error.status === 409) {
          this.mensajeError = 'El email ya estÃ¡ en uso';
        } else {
          this.mensajeError = 'Error al actualizar el perfil';
        }
        
        setTimeout(() => this.mensajeError = '', 5000);
      }
    });
  }

  getIniciales(): string {
    if (!this.usuario?.name) return 'U';
    
    // Obtener solo el primer nombre
    const primerNombre = this.usuario.name.trim().split(' ')[0];
    
    // Retornar las primeras 2 letras del primer nombre en mayÃºsculas
    return primerNombre.substring(0, 2).toUpperCase();
  }

  // --- SecciÃ³n de ConfiguraciÃ³n de Seguridad ---
  editandoPassword = false;
  passwordActual: string = '';
  nuevaPassword: string = '';
  confirmarPassword: string = '';

  showPasswordActual = false;
  showNuevaPassword = false;
  showConfirmarPassword = false;

  mensajeSeguridadExito = '';
  mensajeSeguridadError = '';

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
    if (!this.usuario?.id) return;

    if (!this.passwordActual || !this.nuevaPassword || !this.confirmarPassword) {
      this.mensajeSeguridadError = 'Todos los campos son obligatorios';
      setTimeout(() => this.mensajeSeguridadError = '', 3000);
      return;
    }

    if (this.nuevaPassword !== this.confirmarPassword) {
      this.mensajeSeguridadError = 'Las contraseÃ±as nuevas no coinciden';
      setTimeout(() => this.mensajeSeguridadError = '', 3000);
      return;
    }

    if (this.nuevaPassword.length < 6) {
      this.mensajeSeguridadError = 'La nueva contraseÃ±a debe tener al menos 6 caracteres';
      setTimeout(() => this.mensajeSeguridadError = '', 3000);
      return;
    }

    this.loading = true;
    this.usuarioService.cambiarPassword(this.usuario.id, { password: this.nuevaPassword }).subscribe({
      next: () => {
        this.mensajeExito = 'ContraseÃ±a actualizada correctamente. Por seguridad, vuelve a iniciar sesiÃ³n.';
        this.loading = false;
        this.editandoPassword = false;
        
        // El backend ha invalidado el token actual, asÃ­ que debemos cerrar sesiÃ³n
        setTimeout(() => {
          this.authService.logout();
        }, 2000);
      },
      error: (error) => {
        console.error('Error al cambiar contraseÃ±a:', error);
        this.loading = false;
        this.mensajeSeguridadError = 'Error al actualizar la contraseÃ±a';
        setTimeout(() => this.mensajeSeguridadError = '', 5000);
      }
    });
  }

  volverDashboard() {
    this.router.navigate(['/cliente/dashboard']);
  }
}

