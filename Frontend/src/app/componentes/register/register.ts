import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ValidatorFn, AbstractControl, ReactiveFormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { UsuarioService } from '../../services/usuario.service';
import { UsuarioPatronesService } from '../../services/usuario-patrones.service';
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
  
  registerForm: FormGroup;
  loading = false;
  error = '';
  success = '';
  usarPatrones = true;

  constructor(
    private formBuilder: FormBuilder,
    private usuarioService: UsuarioService,
    private usuarioPatronesService: UsuarioPatronesService,
    private cdr: ChangeDetectorRef
  ) {
    this.registerForm = this.formBuilder.group({
      name: ['', [Validators.required, Validators.minLength(2)]],
      phone: [''],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]],
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
      let errorMessage = 'Por favor, revisa los campos';
      this.mostrarMensajeError(errorMessage);
      this.registerForm.markAllAsTouched();
    }
  }

  registrarConPatrones(formValue: any): void {
    const clienteData = {
      email: formValue.email.trim(),
      password: formValue.password,
      name: formValue.name.trim(),
      phone: this.formatearTelefono(formValue.phone)
    };
    this.usuarioPatronesService.crearCliente(clienteData).subscribe({
      next: (response) => {
        this.mostrarMensajeExito('Cuenta creada exitosamente!');
        this.registerForm.reset();
        this.loading = false;
      },
      error: (error) => {
        this.manejarErrorRegistro(error);
      }
    });
  }

  registrarTradicional(formValue: any): void {
    const usuario: UsuarioCreateDTO = {
      name: formValue.name.trim(),
      phone: this.formatearTelefono(formValue.phone),
      email: formValue.email.trim(),
      password: formValue.password,
      role: 'CUSTOMER'
    };
    this.usuarioService.crearUsuario(usuario).subscribe({
      next: (response) => {
        this.mostrarMensajeExito('Cuenta creada exitosamente!');
        this.registerForm.reset();
        this.loading = false;
      },
      error: (error) => {
        this.manejarErrorRegistro(error);
      }
    });
  }

  /**
   * Formatea el número de teléfono eliminando espacios y código de país +51
   */
  formatearTelefono(telefono: string | null | undefined): string {
    if (!telefono) {
      return '';
    }

    // Convertir a string y limpiar
    let telefonoLimpio = telefono.toString().trim();

    // Eliminar todos los espacios
    telefonoLimpio = telefonoLimpio.replace(/\s+/g, '');

    // Eliminar el código de país +51 si está presente
    if (telefonoLimpio.startsWith('+51')) {
      telefonoLimpio = telefonoLimpio.substring(3);
    } else if (telefonoLimpio.startsWith('51') && telefonoLimpio.length > 9) {
      // Si empieza con 51 y tiene más de 9 dígitos, probablemente sea 51XXXXXXXXX
      telefonoLimpio = telefonoLimpio.substring(2);
    }

    // Eliminar guiones y otros caracteres no numéricos
    telefonoLimpio = telefonoLimpio.replace(/[^0-9]/g, '');

    return telefonoLimpio;
  }

  manejarErrorRegistro(error: any): void {
    let errorMessage = 'Error al crear la cuenta';
    if (error.error && error.error.message) {
      errorMessage = error.error.message;
    }
    this.mostrarMensajeError(errorMessage);
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

  verificarNombreDuplicado(): boolean {
    return false;
  }

  verificarEmailDuplicado(): boolean {
    return false;
  }
}
