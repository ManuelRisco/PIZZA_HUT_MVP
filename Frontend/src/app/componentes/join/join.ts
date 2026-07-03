import { Component } from '@angular/core';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { LoginDTO } from '../../models/usuario.interface';

import { CommonModule } from '@angular/common';

interface ValidationError {
  field: string;
  message: string;
  code: string;
}

@Component({
  selector: 'app-join',
  standalone: true,
  imports: [
    CommonModule,
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
  showPassword = false;

  constructor(private readonly authService: AuthService, private readonly router: Router) {}

  // Validar formato de email
  isValidEmail(email: string): boolean {
    // Validar formato básico
    const emailRegex = /^[\w.-]+@[\w.-]+\.\w+$/i;
    if (!emailRegex.test(email)) return false;

    // Extraer la parte local (antes del @)
    const localPart = email.split('@')[0];

    // Validar longitud: mÃ­nimo 2 caracteres (local), mÃ¡ximo 30 total
    if (localPart.length < 2 || email.length > 31) return false;

    return true;
  }

  // Validar contraseÃ±a (mÃ­nimo 6, mÃ¡ximo 20 caracteres)
  isValidPassword(password: string): boolean {
    return password.length >= 6 && password.length <= 20;
  }

  // Validaciones del formulario
  validateForm(): ValidationError[] {
    const errors: ValidationError[] = [];
    this.fieldErrors = {};

    // Validar email
    if (!this.usuario.email || this.usuario.email.trim() === '') {
      errors.push({ field: 'email', message: 'Complete el campo correo electrÃ³nico.', code: 'CP02' });
      this.fieldErrors['email'] = 'Correo electrÃ³nico requerido';
    } else if (!this.isValidEmail(this.usuario.email)) {
      errors.push({ field: 'email', message: 'Ingrese un correo electrÃ³nico vÃ¡lido (2-20 caracteres).', code: 'CP03' });
      this.fieldErrors['email'] = 'Formato invÃ¡lido: mÃ­nimo 2 caracteres, mÃ¡ximo 20 total (ej: usuario@dominio.com)';
    }
    // Validar contraseÃ±a
    if (!this.usuario.password || this.usuario.password.trim() === '') {
      errors.push({ field: 'password', message: 'Complete el campo contraseÃ±a.', code: 'CP06' });
      this.fieldErrors['password'] = 'ContraseÃ±a requerida';
    } else if (!this.isValidPassword(this.usuario.password)) {
      errors.push({ field: 'password', message: 'La contraseÃ±a debe tener entre 6 y 20 caracteres.', code: 'CP07' });
      this.fieldErrors['password'] = 'ContraseÃ±a debe tener entre 6 y 20 caracteres';
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

        // Redirigir segÃºn el rol del usuario en la respuesta (CP08, CP09)
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
          // CP11: Error de servidor o conexiÃ³n
          this.serverError = true;
          this.loginError = 'Error de conexiÃ³n con el servidor. Intente mÃ¡s tarde.';
        } else if (error.status === 401) {
          // CP05: ContraseÃ±a incorrecta
          this.loginError = 'ContraseÃ±a incorrecta';
          this.fieldErrors['password'] = 'ContraseÃ±a incorrecta';
        } else if (error.status === 404) {
          // CP04: Correo no registrado
          this.loginError = 'El correo no se encuentra registrado en el sistema.';
          this.fieldErrors['email'] = 'Correo no registrado';
        } else if (error.status === 403) {
          // Usuario bloqueado o sin acceso
          this.loginError = error.error?.message || 'Acceso denegado. Su cuenta puede estar bloqueada.';
        } else {
          this.loginError = error.error?.message || 'Correo o contraseÃ±a incorrectos';
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

