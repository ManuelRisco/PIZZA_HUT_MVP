import { Component } from '@angular/core';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { LoginDTO } from '../../models/usuario.interface';

interface ValidationError {
  field: string;
  message: string;
  code: string;
}

@Component({
  selector: 'app-join',
  standalone: true,
  imports: [
    FormsModule,
    RouterModule
  ],
  templateUrl: './join.html',
  styleUrls: ['./join.css']
})
export class Join {

  usuario: LoginDTO = {
    email: '',
    password: '',
  };

  loginError: string = '';
  fieldErrors: { [key: string]: string } = {};
  loading = false;
  serverError = false;

  constructor(private authService: AuthService, private router: Router) {}

  // Validar formato de email
  isValidEmail(email: string): boolean {
    // Validar formato básico
    const emailRegex = /^[\w\.-]+@[\w\.-]+\.\w+$/i;
    if (!emailRegex.test(email)) return false;

    // Extraer la parte local (antes del @)
    const localPart = email.split('@')[0];

    // Validar longitud: mínimo 2 caracteres (local), máximo 30 total
    if (localPart.length < 2 || email.length > 31) return false;

    return true;
  }

  // Validar contraseña (mínimo 6, máximo 20 caracteres)
  isValidPassword(password: string): boolean {
    return password.length >= 6 && password.length <= 20;
  }

  // Validaciones del formulario
  validateForm(): ValidationError[] {
    const errors: ValidationError[] = [];
    this.fieldErrors = {};

    // Validar email
    if (!this.usuario.email || this.usuario.email.trim() === '') {
      errors.push({ field: 'email', message: 'Complete el campo correo electrónico.', code: 'CP02' });
      this.fieldErrors['email'] = 'Correo electrónico requerido';
    } else if (!this.isValidEmail(this.usuario.email)) {
      errors.push({ field: 'email', message: 'Ingrese un correo electrónico válido (2-20 caracteres).', code: 'CP03' });
      this.fieldErrors['email'] = 'Formato inválido: mínimo 2 caracteres, máximo 20 total (ej: usuario@dominio.com)';
    }
    // Validar contraseña
    if (!this.usuario.password || this.usuario.password.trim() === '') {
      errors.push({ field: 'password', message: 'Complete el campo contraseña.', code: 'CP06' });
      this.fieldErrors['password'] = 'Contraseña requerida';
    } else if (!this.isValidPassword(this.usuario.password)) {
      errors.push({ field: 'password', message: 'La contraseña debe tener entre 6 y 20 caracteres.', code: 'CP07' });
      this.fieldErrors['password'] = 'Contraseña debe tener entre 6 y 20 caracteres';
    }

    return errors;
  }

  onLogin(event: Event) {
    event.preventDefault();
    this.loginError = '';
    this.serverError = false;
    this.fieldErrors = {};

    // Validar formulario
    const validationErrors = this.validateForm();
    if (validationErrors.length > 0) {
      this.loginError = validationErrors[0].message;
      return;
    }

    this.loading = true;

    this.authService.login(this.usuario.email.toLowerCase().trim(), this.usuario.password).subscribe({
      next: (response) => {
        this.loading = false;

        // Validar rol asignado (CP10)
        if (!response.usuario.role || (response.usuario.role !== 'ADMIN' && response.usuario.role !== 'CUSTOMER')) {
          this.loginError = 'Usuario sin rol asignado. Contacte al administrador.';
          return;
        }

        // Redirigir según el rol del usuario en la respuesta (CP08, CP09)
        if (response.usuario.role === 'ADMIN') {
          this.router.navigate(['/panel-admin']);
        } else if (response.usuario.role === 'CUSTOMER') {
          this.router.navigate(['/menu']);
        }
      },
      error: (error) => {
        this.loading = false;
        console.error('Error en login:', error);

        // Distinguir entre errores
        if (!error.status || error.status === 0) {
          // CP11: Error de servidor o conexión
          this.serverError = true;
          this.loginError = 'Error de conexión con el servidor. Intente más tarde.';
        } else if (error.status === 401) {
          // CP05: Contraseña incorrecta
          this.loginError = 'Contraseña incorrecta';
          this.fieldErrors['password'] = 'Contraseña incorrecta';
        } else if (error.status === 404) {
          // CP04: Correo no registrado
          this.loginError = 'El correo no se encuentra registrado en el sistema.';
          this.fieldErrors['email'] = 'Correo no registrado';
        } else if (error.status === 403) {
          // Usuario bloqueado o sin acceso
          this.loginError = error.error?.message || 'Acceso denegado. Su cuenta puede estar bloqueada.';
        } else {
          this.loginError = error.error?.message || 'Correo o contraseña incorrectos';
        }
      }
    });
  }

  // Limpiar error cuando el usuario empieza a escribir
  onEmailChange() {
    if (this.fieldErrors['email']) {
      delete this.fieldErrors['email'];
    }
  }

  onPasswordChange() {
    if (this.fieldErrors['password']) {
      delete this.fieldErrors['password'];
    }
  }
}
