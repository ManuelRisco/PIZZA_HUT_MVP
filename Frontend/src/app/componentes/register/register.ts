import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ValidatorFn, AbstractControl, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { UsuarioService } from '../../services/usuario.service';
import { UsuarioPatronesService } from '../../services/usuario-patrones.service';
import { AuthService } from '../../services/auth.service';
import { AccessibilityService } from '../../services/accessibility.service';
import { UsuarioDTO, UsuarioCreateDTO } from '../../models/usuario.interface';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterModule
  ],
  templateUrl: './register.html',
  styleUrls: ['./register.css']
})
export class Register implements OnInit {

  registerForm!: FormGroup;
  loading = false;
  error = '';
  success = '';
  usarPatrones = true;
  showPassword = false;
  showConfirmPassword = false;

  constructor(
    private readonly formBuilder: FormBuilder,
    private readonly usuarioService: UsuarioService,
    private readonly usuarioPatronesService: UsuarioPatronesService,
    private readonly authService: AuthService,
    private readonly cdr: ChangeDetectorRef,
    private readonly accessibilityService: AccessibilityService,
    private readonly router: Router
  ) {
    this.registerForm = this.formBuilder.group({
      name: ['', [
        Validators.required,
        Validators.minLength(4),
        Validators.maxLength(50),
        Validators.pattern(/^[a-zA-ZáéíóúÁÉÍÓÚñÑ\s]+$/)
      ]],
      phone: ['', [
        Validators.required,
        Validators.pattern(/^[0-9]{9}$/)
      ]],
      email: ['', [
        Validators.required,
        Validators.email
      ]],
      password: ['', [
        Validators.required,
        Validators.minLength(6),
        Validators.maxLength(20),
        Validators.pattern(/^(?=.*[A-Z])(?=.*\d).+$/)
      ]],
      confirmPassword: ['', Validators.required]
    }, { validators: this.passwordsMatchValidator });
  }

  ngOnInit(): void {
    this.setupFormValidationListeners();
  }

  passwordsMatchValidator: ValidatorFn = (form: AbstractControl) => {
    const password = form.get('password')?.value;
    const confirm = form.get('confirmPassword')?.value;
    return password === confirm ? null : { passwordMismatch: true };
  };

  setupFormValidationListeners(): void {
    this.registerForm.get('password')?.valueChanges.subscribe(() => {
      this.registerForm.get('confirmPassword')?.updateValueAndValidity();
    });
    this.registerForm.get('confirmPassword')?.valueChanges.subscribe(() => {
      this.registerForm.updateValueAndValidity();
    });
  }

  onSubmit(): void {
    if (this.registerForm.valid) {
      this.loading = true;
      this.limpiarMensajes();
      const formValue = this.registerForm.value;
      if (this.usarPatrones) {
        this.registrarConPatrones(formValue);
      } else {
        this.registrarTradicional(formValue);
      }
    } else {
      let errorMessage = 'Por favor, revisa los campos en rojo y corrige los errores.';
      this.mostrarMensajeError(errorMessage);
      this.registerForm.markAllAsTouched();
      this.accessibilityService.announceError(errorMessage);
      
      // Anunciar el primer error específico
      const controls = this.registerForm.controls;
      for (const name in controls) {
        if (controls[name].invalid) {
          this.accessibilityService.announceValidationError(name, 'Campo inválido o incompleto');
          break;
        }
      }
    }
  }

  registrarConPatrones(formValue: any): void {
    const clienteData = {
      email: formValue.email.trim().toLowerCase(),
      password: formValue.password,
      name: formValue.name.trim(),
      phone: formValue.phone.trim()
    };
    this.usuarioPatronesService.crearCliente(clienteData).subscribe({
      next: () => {
        this.confirmarRegistro();
      },
      error: (error) => {
        this.manejarErrorRegistro(error);
      }
    });
  }

  registrarTradicional(formValue: any): void {
    const usuario: UsuarioCreateDTO = {
      name: formValue.name.trim(),
      phone: formValue.phone.trim(),
      email: formValue.email.trim().toLowerCase(),
      password: formValue.password,
      role: 'CUSTOMER'
    };
    this.usuarioService.crearUsuario(usuario).subscribe({
      next: () => {
        this.confirmarRegistro();
      },
      error: (error) => {
        this.manejarErrorRegistro(error);
      }
    });
  }

  confirmarRegistro(): void {
    const email = String(this.registerForm.value.email || '').trim().toLowerCase();
    const password = String(this.registerForm.value.password || '');
    this.mostrarMensajeExito('Usuario registrado correctamente');
    this.loading = false;
    this.authService.login(email, password).subscribe({
      next: (response) => {
        const destino = response?.usuario?.role === 'ADMIN' ? '/panel-admin' : '/menu';
        this.router.navigate([destino]);
      },
      error: () => {
        this.router.navigate(['/join'], { queryParams: { email } });
      }
    });
  }

  manejarErrorRegistro(error: any): void {
    let errorMessage = 'Error al crear la cuenta';

    if (error.error?.message) {
      const serverMessage = error.error.message.toLowerCase();

      // Manejo dinámico de duplicados devueltos por el backend
      if (serverMessage.includes('correo') || serverMessage.includes('email')) {
        this.registerForm.get('email')?.setErrors({ duplicado: true });
        errorMessage = 'El correo electrónico ya se encuentra registrado';
      } else if (serverMessage.includes('nombre') || serverMessage.includes('name')) {
        this.registerForm.get('name')?.setErrors({ duplicado: true });
        errorMessage = 'El nombre de usuario ya se encuentra registrado';
      } else {
        errorMessage = error.error.message;
      }
    }

    this.mostrarMensajeError(errorMessage);
    this.accessibilityService.announceError(errorMessage);
    this.loading = false;
  }

  limpiarMensajes(): void {
    this.error = '';
    this.success = '';
  }

  mostrarMensajeExito(mensaje: string): void {
    this.success = mensaje;
    this.error = '';
    setTimeout(() => {
      this.success = '';
      this.cdr.detectChanges();
    }, 5000);
  }

  mostrarMensajeError(mensaje: string): void {
    this.error = mensaje;
    this.success = '';
  }
}

