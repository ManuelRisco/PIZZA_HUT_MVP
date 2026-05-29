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
    private authService: AuthService,
    private usuarioService: UsuarioService,
    private router: Router
  ) {}

  ngOnInit() {
    const currentUser = this.authService.getCurrentUser();
    if (!currentUser) {
      this.router.navigate(['/']);
      return;
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

  toggleEdicion() {
    this.editando = !this.editando;
    // Si cancela la edición, recargar los datos originales
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
        console.error('Error al cargar información del usuario:', error);
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
    if (!this.usuario || !this.usuario.id) {
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
          this.mensajeError = error.error?.message || 'Datos inválidos';
        } else if (error.status === 409) {
          this.mensajeError = 'El email ya está en uso';
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
    
    // Retornar las primeras 2 letras del primer nombre en mayúsculas
    return primerNombre.substring(0, 2).toUpperCase();
  }

  volverDashboard() {
    this.router.navigate(['/cliente/dashboard']);
  }
}
