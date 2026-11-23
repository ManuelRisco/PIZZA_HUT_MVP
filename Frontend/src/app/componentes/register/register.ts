import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ValidatorFn, AbstractControl, ReactiveFormsModule, FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { UsuarioService } from '../../services/usuario.service';
import { UsuarioPatronesService } from '../../services/usuario-patrones.service';
import { AuthService } from '../../services/auth.service';
import { AccessibilityService } from '../../services/accessibility.service';
import { SunatService } from '../../services/sunat.service';
import { UsuarioDTO, UsuarioCreateDTO } from '../../models/usuario.interface';

declare const google: any;

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
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

  googleLoading = false;
  googleClientId = '';
  googleCloudConfigured = false;
  showGoogleModal = false;
  googleEmail = '';
  googleName = '';
  googleDni = '';
  googleAgeVerified = false;
  googleModalError = '';

  buscandoDni = false;
  dniMensaje = '';
  dniError = '';

  constructor(
    private readonly formBuilder: FormBuilder,
    private readonly usuarioService: UsuarioService,
    private readonly usuarioPatronesService: UsuarioPatronesService,
    private readonly authService: AuthService,
    private readonly sunatService: SunatService,
    private readonly cdr: ChangeDetectorRef,
    private readonly accessibilityService: AccessibilityService,
    private readonly router: Router
  ) {
    this.registerForm = this.formBuilder.group({
      dni: ['', [
        Validators.pattern(/^[0-9]{8}$/)
      ]],
      ageVerified: [false],
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
    this.authService.obtenerConfiguracionGoogle().subscribe({
      next: (cfg) => {
        if (cfg) {
          this.googleClientId = cfg.clientId || '';
          this.googleCloudConfigured = !!cfg.googleCloudConfigured;
          if (this.googleCloudConfigured && this.googleClientId && !document.getElementById('google-gsi-client-script')) {
            const script = document.createElement('script');
            script.id = 'google-gsi-client-script';
            script.src = 'https://accounts.google.com/gsi/client';
            script.async = true;
            script.defer = true;
            document.head.appendChild(script);
          }
        }
      },
      error: () => {}
    });
  }

  onGoogleRegisterClick(): void {
    this.limpiarMensajes();
    if (this.googleCloudConfigured && this.googleClientId && typeof google !== 'undefined' && google?.accounts?.oauth2) {
      this.googleLoading = true;
      const tokenClient = google.accounts.oauth2.initTokenClient({
        client_id: this.googleClientId,
        scope: 'openid email profile',
        callback: (tokenResponse: any) => {
          if (tokenResponse && tokenResponse.access_token) {
            this.executeGoogleBackendAuth({ accessToken: tokenResponse.access_token });
          } else {
            this.googleLoading = false;
            this.mostrarMensajeError('No se complet\u00f3 la autorizaci\u00f3n con Google.');
          }
        },
        error_callback: () => {
          this.googleLoading = false;
        }
      });
      tokenClient.requestAccessToken({ prompt: 'select_account' });
      return;
    }

    this.googleEmail = this.registerForm.get('email')?.value || '';
    this.googleName = this.registerForm.get('name')?.value || '';
    this.googleDni = this.registerForm.get('dni')?.value || '';
    this.googleAgeVerified = Boolean(this.registerForm.get('ageVerified')?.value);
    this.googleModalError = '';
    this.showGoogleModal = true;
  }

  consultarDniReniec(isGoogleModal: boolean = false): void {
    this.dniError = '';
    this.dniMensaje = '';
    const rawDni = isGoogleModal
      ? (this.googleDni || '').trim()
      : String(this.registerForm.get('dni')?.value || '').trim();

    if (!/^\d{8}$/.test(rawDni)) {
      const msg = 'Ingresa un DNI válido de 8 dígitos para consultar en RENIEC.';
      if (isGoogleModal) {
        this.googleModalError = msg;
      } else {
        this.dniError = msg;
      }
      return;
    }

    this.buscandoDni = true;
    this.sunatService.consultarDni(rawDni).subscribe({
      next: (res) => {
        this.buscandoDni = false;
        if (res?.nombreCompleto) {
          if (isGoogleModal) {
            this.googleName = res.nombreCompleto;
          } else {
            this.registerForm.patchValue({ name: res.nombreCompleto });
            this.registerForm.get('name')?.markAsTouched();
            this.dniMensaje = `Datos RENIEC encontrados: ${res.nombreCompleto}`;
          }
        }
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.buscandoDni = false;
        const msg = err?.error?.message || 'No se pudo consultar el DNI en RENIEC. Puedes ingresar tu nombre manualmente.';
        if (isGoogleModal) {
          this.googleModalError = msg;
        } else {
          this.dniError = msg;
        }
        this.cdr.detectChanges();
      }
    });
  }

  closeGoogleModal(): void {
    if (this.googleLoading) return;
    this.showGoogleModal = false;
    this.googleModalError = '';
  }

  onConfirmGoogleModal(): void {
    this.googleModalError = '';
    const cleanEmail = (this.googleEmail || '').trim().toLowerCase();
    const emailRegex = /^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/;
    if (!cleanEmail || !emailRegex.test(cleanEmail)) {
      this.googleModalError = 'Ingrese un correo de Google v\u00e1lido.';
      return;
    }
    const cleanDni = (this.googleDni || '').trim();
    if (cleanDni && !/^\d{8}$/.test(cleanDni)) {
      this.googleModalError = 'El DNI debe tener exactamente 8 d\u00edgitos num\u00e9ricos.';
      return;
    }
    if (cleanDni && !this.googleAgeVerified) {
      this.googleModalError = 'Debes confirmar que eres mayor de 18 a\u00f1os y titular del DNI ingresado.';
      return;
    }
    this.executeGoogleBackendAuth({
      email: cleanEmail,
      name: (this.googleName || '').trim()
    });
  }

  private executeGoogleBackendAuth(payload: { credential?: string; accessToken?: string; email?: string; name?: string }): void {
    this.googleLoading = true;
    const cleanDni = (this.googleDni || this.registerForm.get('dni')?.value || '').trim();
    const ageOk = this.googleAgeVerified || Boolean(this.registerForm.get('ageVerified')?.value);
    this.authService.loginConGoogle(payload).subscribe({
      next: (response) => {
        const destino = response?.usuario?.role === 'ADMIN' ? '/panel-admin' : '/menu';
        if (/^\d{8}$/.test(cleanDni) && ageOk) {
          this.sunatService.configurarDni(cleanDni, true, this.googleName || undefined).subscribe({
            next: (u) => {
              this.authService.updateCurrentUser(u);
              this.googleLoading = false;
              this.showGoogleModal = false;
              this.router.navigate([destino]);
            },
            error: () => {
              this.googleLoading = false;
              this.showGoogleModal = false;
              this.router.navigate([destino]);
            }
          });
        } else {
          this.googleLoading = false;
          this.showGoogleModal = false;
          this.router.navigate([destino]);
        }
      },
      error: (err) => {
        this.googleLoading = false;
        const msg = err.error?.message || 'Error al autenticar con Google.';
        if (this.showGoogleModal) {
          this.googleModalError = msg;
        } else {
          this.mostrarMensajeError(msg);
        }
      }
    });
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
    const cleanDni = String(this.registerForm.get('dni')?.value || '').trim();
    const ageVerified = Boolean(this.registerForm.get('ageVerified')?.value);
    if (cleanDni && !/^\d{8}$/.test(cleanDni)) {
      this.mostrarMensajeError('El DNI debe contener exactamente 8 dígitos numéricos.');
      return;
    }
    if (cleanDni && !ageVerified) {
      this.mostrarMensajeError('Debes confirmar que eres mayor de 18 años y que el DNI ingresado te pertenece.');
      return;
    }
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
    const cleanDni = String(formValue.dni || '').trim();
    const clienteData = {
      email: formValue.email.trim().toLowerCase(),
      password: formValue.password,
      name: formValue.name.trim(),
      phone: formValue.phone.trim(),
      dni: cleanDni || null,
      ageVerified: cleanDni ? Boolean(formValue.ageVerified) : false
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
    const cleanDni = String(formValue.dni || '').trim();
    const usuario: UsuarioCreateDTO = {
      name: formValue.name.trim(),
      phone: formValue.phone.trim(),
      email: formValue.email.trim().toLowerCase(),
      password: formValue.password,
      dni: cleanDni || null,
      ageVerified: cleanDni ? Boolean(formValue.ageVerified) : false,
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

