import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { finalize } from 'rxjs/operators';
import { AuthService } from '../../services/auth.service';
import { UsuarioService } from '../../services/usuario.service';
import { SunatService } from '../../services/sunat.service';
import { UiSkeletonComponent } from '../ui-skeleton/ui-skeleton';

@Component({
  selector: 'app-cliente-perfil',
  standalone: true,
  imports: [CommonModule, FormsModule, UiSkeletonComponent],
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
  vinculandoGoogle = false;
  buscandoDni = false;
  ageVerified = false;

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
    private readonly sunatService: SunatService,
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
          const isGoogle = this.authService.isGoogleLinked(usuario);
          this.usuario = {
            ...usuario,
            dni: usuario?.dni || '',
            ageVerified: Boolean(usuario?.ageVerified || usuario?.dni),
            googleLinked: isGoogle,
            authProvider: isGoogle ? 'GOOGLE' : (usuario?.authProvider || 'LOCAL')
          };
          this.ageVerified = Boolean(this.usuario.ageVerified);
          this.authService.updateCurrentUser(this.usuario);
        },
        error: (error) => {
          console.error('Error al cargar informaci\u00f3n del usuario:', error);
          const isGoogle = this.authService.isGoogleLinked(currentUser);
          this.usuario = currentUser
            ? { ...currentUser, active: true, createdAt: null, googleLinked: isGoogle, authProvider: isGoogle ? 'GOOGLE' : 'LOCAL' }
            : null;
          this.loadError = error?.status === 0
            ? 'No se pudo conectar con el servidor (Backend fuera de l\u00ednea). Mostrando informaci\u00f3n en cach\u00e9 de tu sesi\u00f3n.'
            : (error?.error?.message || 'No se pudo sincronizar tu perfil con el servidor.');
        }
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
    this.authService.vincularCuentaConGoogle()
      .pipe(finalize(() => { this.vinculandoGoogle = false; }))
      .subscribe({
        next: (actualizado) => {
          this.usuario = {
            ...this.usuario,
            ...actualizado,
            googleLinked: true,
            authProvider: 'GOOGLE'
          };
          this.mensajeExito = 'Tu cuenta ha sido vinculada con Google correctamente';
          setTimeout(() => this.mensajeExito = '', 4000);
        },
        error: (err) => {
          this.mensajeError = err?.error?.message || 'No se pudo vincular la cuenta con Google.';
          setTimeout(() => this.mensajeError = '', 4000);
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

    const cleanDni = this.usuario.dni ? String(this.usuario.dni).trim() : '';
    if (cleanDni && !/^\d{8}$/.test(cleanDni)) {
      this.mensajeError = 'El DNI debe contener exactamente 8 d\u00edgitos num\u00e9ricos';
      setTimeout(() => this.mensajeError = '', 3500);
      return;
    }
    if (cleanDni && !this.ageVerified) {
      this.mensajeError = 'Debes confirmar que eres mayor de 18 a\u00f1os y titular del DNI ingresado';
      setTimeout(() => this.mensajeError = '', 3500);
      return;
    }

    const datosActualizados: any = {
      name: this.usuario.name.trim(),
      email: this.usuario.email.trim(),
      phone: this.usuario.phone && this.usuario.phone.trim() !== '' ? this.usuario.phone.trim() : null,
      dni: cleanDni,
      ageVerified: cleanDni ? this.ageVerified : false
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
            this.mensajeError = 'Sin conexi\u00f3n con el servidor. Int\u00e9ntalo nuevamente cuando el servicio est\u00e9 en l\u00ednea.';
          } else if (error.status === 400) {
            this.mensajeError = error.error?.message || 'Datos inv\u00e1lidos';
          } else if (error.status === 409) {
            this.mensajeError = 'El correo electr\u00f3nico ya est\u00e1 en uso';
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

  consultarDniPerfil(): void {
    const clean = (this.usuario?.dni || '').trim();
    if (!/^\d{8}$/.test(clean)) {
      this.mensajeError = 'Ingresa un DNI v\u00e1lido de 8 d\u00edgitos para consultar en RENIEC';
      setTimeout(() => this.mensajeError = '', 3500);
      return;
    }
    this.buscandoDni = true;
    this.sunatService.consultarDni(clean).subscribe({
      next: (res) => {
        this.buscandoDni = false;
        if (res?.nombreCompleto) {
          this.usuario.name = res.nombreCompleto;
          this.mensajeExito = `Nombre autocompletado con RENIEC: ${res.nombreCompleto}`;
          setTimeout(() => this.mensajeExito = '', 4000);
        }
      },
      error: (err) => {
        this.buscandoDni = false;
        this.mensajeError = err?.error?.message || 'No se pudo consultar el DNI en RENIEC';
        setTimeout(() => this.mensajeError = '', 4000);
      }
    });
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

    const requiereActual = this.tienePasswordDefinida;
    if ((requiereActual && !this.passwordActual) || !this.nuevaPassword || !this.confirmarPassword) {
      this.mensajeSeguridadError = 'Todos los campos son obligatorios';
      setTimeout(() => this.mensajeSeguridadError = '', 3500);
      return;
    }

    if (this.nuevaPassword !== this.confirmarPassword) {
      this.mensajeSeguridadError = 'Las contrase\u00f1as nuevas no coinciden';
      setTimeout(() => this.mensajeSeguridadError = '', 3500);
      return;
    }

    if (this.nuevaPassword.length < 6) {
      this.mensajeSeguridadError = 'La nueva contrase\u00f1a debe tener al menos 6 caracteres';
      setTimeout(() => this.mensajeSeguridadError = '', 3500);
      return;
    }

    this.guardandoPassword = true;

    if (!requiereActual) {
      this.authService.definirPasswordCuentaGoogle(this.nuevaPassword)
        .pipe(finalize(() => { this.guardandoPassword = false; }))
        .subscribe({
          next: (actualizado) => {
            this.usuario = {
              ...this.usuario,
              ...actualizado,
              passwordSet: true
            };
            this.mensajeExito = 'Contrase\u00f1a definida correctamente. Ahora tambi\u00e9n puedes iniciar sesi\u00f3n con tu correo y contrase\u00f1a.';
            this.editandoPassword = false;
            this.limpiarFormularioPassword();
            setTimeout(() => this.mensajeExito = '', 4500);
          },
          error: (error) => {
            this.mensajeSeguridadError = error?.error?.message || 'Error al definir la contrase\u00f1a';
            setTimeout(() => this.mensajeSeguridadError = '', 5000);
          }
        });
      return;
    }

    this.usuarioService.cambiarPassword(this.usuario.id, {
      password: this.nuevaPassword,
      currentPassword: this.passwordActual
    })
      .pipe(finalize(() => { this.guardandoPassword = false; }))
      .subscribe({
        next: () => {
          this.mensajeExito = 'Contrase\u00f1a actualizada correctamente. Por seguridad, vuelve a iniciar sesi\u00f3n.';
          this.editandoPassword = false;
          setTimeout(() => {
            this.authService.logout();
          }, 2000);
        },
        error: (error) => {
          console.error('Error al cambiar contrase\u00f1a:', error);
          this.mensajeSeguridadError = error?.status === 0
            ? 'Sin conexi\u00f3n con el servidor. No se pudo actualizar la contrase\u00f1a.'
            : (error?.error?.message || 'Error al actualizar la contrase\u00f1a');
          setTimeout(() => this.mensajeSeguridadError = '', 5000);
        }
      });
  }

  volverDashboard() {
    this.router.navigate(['/cliente/dashboard']);
  }
}
