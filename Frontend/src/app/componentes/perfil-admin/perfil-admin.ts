import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../services/auth.service';
import { UsuarioService } from '../../services/usuario.service';
import { UsuarioDTO, UsuarioCreateDTO } from '../../models/usuario.interface';

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
  
  mostrarCambioPassword: boolean = false;
  mensaje: string = '';
  tipoMensaje: 'success' | 'error' = 'success';
  cargando: boolean = false;

  constructor(
    private authService: AuthService,
    private usuarioService: UsuarioService
  ) {}

  ngOnInit() {
    this.cargarDatosUsuario();
  }

  cargarDatosUsuario() {
    const token = this.authService.getToken();
    if (!token) {
      this.mensaje = 'No se pudo cargar la información del usuario';
      this.tipoMensaje = 'error';
      return;
    }

    // Decodificar el token para obtener el ID
    const tokenData = this.decodeToken(token);
    if (!tokenData || !tokenData.id) {
      this.mensaje = 'Token inválido';
      this.tipoMensaje = 'error';
      return;
    }

    // Obtener el usuario completo desde el backend
    this.usuarioService.obtenerUsuarioPorId(tokenData.id).subscribe({
      next: (usuario) => {
        this.usuario = usuario;
        this.usuarioEditado = { ...usuario };
      },
      error: (error) => {
        this.mensaje = 'Error al cargar los datos del usuario';
        this.tipoMensaje = 'error';
        console.error('Error:', error);
      }
    });
  }

  private decodeToken(token: string): any {
    try {
      const payload = token.split('.')[1];
      return JSON.parse(atob(payload));
    } catch (error) {
      return null;
    }
  }

  actualizarDatos() {
    if (!this.validarFormulario()) {
      return;
    }

    if (!this.usuarioEditado.id) {
      this.tipoMensaje = 'error';
      this.mensaje = 'No se puede actualizar: ID de usuario no válido';
      return;
    }

    this.cargando = true;
    this.mensaje = '';

    const datosActualizar: UsuarioCreateDTO = {
      email: this.usuarioEditado.email,
      name: this.usuarioEditado.name,
      phone: this.usuarioEditado.phone,
      password: 'dummy_password_not_used', // El backend debería ignorar esto en PUT
      role: this.usuarioEditado.role
    };

    this.usuarioService.actualizarPerfil(this.usuarioEditado.id, datosActualizar).subscribe({
      next: (response) => {
        this.tipoMensaje = 'success';
        this.mensaje = 'Datos actualizados correctamente';
        
        // Actualizar los datos locales
        this.usuario = response;
        this.usuarioEditado = { ...response };
        
        this.cargando = false;
        setTimeout(() => this.mensaje = '', 5000);
      },
      error: (error) => {
        this.tipoMensaje = 'error';
        this.mensaje = 'Error al actualizar los datos: ' + (error.error?.message || error.message);
        this.cargando = false;
        console.error('Error completo:', error);
      }
    });
  }

  cambiarPassword() {
    if (this.nuevaPassword !== this.confirmarPassword) {
      this.tipoMensaje = 'error';
      this.mensaje = 'Las contraseñas no coinciden';
      return;
    }

    if (this.nuevaPassword.length < 6) {
      this.tipoMensaje = 'error';
      this.mensaje = 'La contraseña debe tener al menos 6 caracteres';
      return;
    }

    if (!this.usuarioEditado.id) {
      this.tipoMensaje = 'error';
      this.mensaje = 'No se puede cambiar la contraseña: ID de usuario no válido';
      return;
    }

    this.cargando = true;
    this.mensaje = '';

    this.usuarioService.cambiarPassword(this.usuarioEditado.id, { password: this.nuevaPassword }).subscribe({
      next: (response) => {
        this.tipoMensaje = 'success';
        this.mensaje = 'Contraseña cambiada correctamente';
        this.nuevaPassword = '';
        this.confirmarPassword = '';
        this.mostrarCambioPassword = false;
        this.cargando = false;
        setTimeout(() => this.mensaje = '', 5000);
      },
      error: (error) => {
        this.tipoMensaje = 'error';
        this.mensaje = 'Error al cambiar la contraseña: ' + (error.error?.message || error.message);
        this.cargando = false;
        console.error('Error completo:', error);
      }
    });
  }

  validarFormulario(): boolean {
    if (!this.usuarioEditado.name || !this.usuarioEditado.email) {
      this.tipoMensaje = 'error';
      this.mensaje = 'El nombre y el email son obligatorios';
      return false;
    }

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(this.usuarioEditado.email)) {
      this.tipoMensaje = 'error';
      this.mensaje = 'El email no es válido';
      return false;
    }

    return true;
  }

  cancelarCambioPassword() {
    this.mostrarCambioPassword = false;
    this.nuevaPassword = '';
    this.confirmarPassword = '';
    this.mensaje = '';
  }

  cancelarEdicion() {
    this.cargarDatosUsuario();
    this.mensaje = '';
  }
}
