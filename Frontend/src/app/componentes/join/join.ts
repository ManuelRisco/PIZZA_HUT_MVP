import { Component, OnInit } from '@angular/core';
import { RouterModule, ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../services/auth.service';
import { SunatService } from '../../services/sunat.service';
import { LoginDTO } from '../../models/usuario.interface';

declare const google: any;

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
export class Join implements OnInit {

  usuario: LoginDTO = {
    email: '',
    password: '',
  };

  loginError: string = '';
  loginSuccess: string = '';
  fieldErrors: { [key: string]: string } = {};
  loading = false;
  serverError = false;
  showPassword = false;

  googleLoading = false;
  googleClientId = '';
  googleCloudConfigured = false;
  showGoogleModal = false;
  googleEmail = '';
  googleName = '';
  googleModalError = '';

  showDniOption = false;
  loginDni = '';
  loginDniNombre = '';
  loginAgeVerified = false;
  buscandoDni = false;
  dniLookupMsg = '';
  dniLookupError = '';

  showRecoveryModal = false;
  recoveryStep: 1 | 2 = 1;
  recoveryEmail = '';
  recoveryMaskedEmail = '';
  recoveryCode = '';
  recoveryNewPassword = '';
  recoveryConfirmPassword = '';
  showRecoveryPassword = false;
  showRecoveryConfirmPassword = false;
  recoveryLoading = false;
  recoveryError = '';
  recoveryInfoMessage = '';

  constructor(
    private readonly authService: AuthService,
    private readonly sunatService: SunatService,
    private readonly router: Router,
    private readonly route: ActivatedRoute
  ) {
    const email = this.route.snapshot.queryParamMap.get('email');
    if (email) {
      this.usuario.email = email;
    }
  }

  ngOnInit(): void {
    this.authService.obtenerConfiguracionGoogle().subscribe({
      next: (cfg) => {
        if (cfg) {
          this.googleClientId = cfg.clientId || '';
          this.googleCloudConfigured = !!cfg.googleCloudConfigured;
          if (this.googleCloudConfigured && this.googleClientId) {
            this.loadGoogleGsiScript();
          }
        }
      },
      error: () => {}
    });
  }

  private loadGoogleGsiScript(): void {
    if (document.getElementById('google-gsi-client-script')) {
      return;
    }
    const script = document.createElement('script');
    script.id = 'google-gsi-client-script';
    script.src = 'https://accounts.google.com/gsi/client';
    script.async = true;
    script.defer = true;
    document.head.appendChild(script);
  }

  isValidEmail(email: string): boolean {
    const value = email.trim();
    const emailRegex = /^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/;
    return emailRegex.test(value) && value.length <= 255;
  }

  isValidPassword(password: string): boolean {
    return password.length >= 6 && password.length <= 20;
  }

  validateForm(): ValidationError[] {
    const errors: ValidationError[] = [];
    this.fieldErrors = {};

    if (!this.usuario.email || this.usuario.email.trim() === '') {
      errors.push({ field: 'email', message: 'Complete el campo correo electr\u00f3nico.', code: 'CP02' });
      this.fieldErrors['email'] = 'Correo electr\u00f3nico requerido';
    } else if (!this.isValidEmail(this.usuario.email)) {
      errors.push({ field: 'email', message: 'Ingrese un correo electr\u00f3nico v\u00e1lido.', code: 'CP03' });
      this.fieldErrors['email'] = 'Ingrese un correo electr\u00f3nico v\u00e1lido';
    }

    if (!this.usuario.password || this.usuario.password.trim() === '') {
      errors.push({ field: 'password', message: 'Complete el campo contrase\u00f1a.', code: 'CP06' });
      this.fieldErrors['password'] = 'Contrase\u00f1a requerida';
    } else if (!this.isValidPassword(this.usuario.password)) {
      errors.push({ field: 'password', message: 'La contrase\u00f1a debe tener entre 6 y 20 caracteres.', code: 'CP07' });
      this.fieldErrors['password'] = 'Contrase\u00f1a debe tener entre 6 y 20 caracteres';
    }

    return errors;
  }

  onLogin(event: Event): void {
    event.preventDefault();
    const form = event.target as HTMLFormElement;
    const emailInput = form.querySelector<HTMLInputElement>('#email');
    const passwordInput = form.querySelector<HTMLInputElement>('#password');
    if (emailInput?.value) {
      this.usuario.email = emailInput.value;
    }
    if (passwordInput?.value) {
      this.usuario.password = passwordInput.value;
    }
    this.loginError = '';
    this.loginSuccess = '';
    this.serverError = false;
    this.fieldErrors = {};

    const validationErrors = this.validateForm();
    if (validationErrors.length > 0) {
      this.loginError = validationErrors[0].message;
      return;
    }

    const cleanDni = (this.loginDni || '').trim();
    if (cleanDni && !/^\d{8}$/.test(cleanDni)) {
      this.loginError = 'El DNI opcional debe contener exactamente 8 d\u00edgitos num\u00e9ricos.';
      return;
    }
    if (cleanDni && !this.loginAgeVerified) {
      this.loginError = 'Debes confirmar que eres mayor de 18 a\u00f1os y titular del DNI ingresado.';
      return;
    }

    this.loading = true;

    this.authService.login(this.usuario.email.toLowerCase().trim(), this.usuario.password).subscribe({
      next: (response) => {
        this.loading = false;
        this.redirectAfterAuth(response);
      },
      error: (error) => {
        this.loading = false;
        if (!error.status || error.status === 0) {
          this.serverError = true;
          this.loginError = 'Error de conexi\u00f3n con el servidor. Intente m\u00e1s tarde.';
        } else if (error.status === 401) {
          this.loginError = 'Contrase\u00f1a incorrecta';
          this.fieldErrors['password'] = 'Contrase\u00f1a incorrecta';
        } else if (error.status === 404) {
          this.loginError = 'El correo no se encuentra registrado en el sistema.';
          this.fieldErrors['email'] = 'Correo no registrado';
        } else if (error.status === 403) {
          this.loginError = error.error?.message || 'Acceso denegado. Su cuenta puede estar bloqueada.';
        } else {
          this.loginError = error.error?.message || 'Correo o contrase\u00f1a incorrectos';
        }
      }
    });
  }

  onGoogleSignInClick(): void {
    this.loginError = '';
    this.loginSuccess = '';

    if (this.googleCloudConfigured && this.googleClientId && typeof google !== 'undefined' && google?.accounts?.oauth2) {
      this.googleLoading = true;
      const tokenClient = google.accounts.oauth2.initTokenClient({
        client_id: this.googleClientId,
        scope: 'openid email profile',
        callback: (tokenResponse: any) => {
          if (tokenResponse && tokenResponse.access_token) {
            this.executeGoogleBackendLogin({ accessToken: tokenResponse.access_token });
          } else {
            this.googleLoading = false;
            this.loginError = 'No se complet\u00f3 la autorizaci\u00f3n con Google.';
          }
        },
        error_callback: () => {
          this.googleLoading = false;
        }
      });
      tokenClient.requestAccessToken({ prompt: 'select_account' });
      return;
    }

    this.googleEmail = this.usuario.email && this.usuario.email.includes('@') ? this.usuario.email.trim() : '';
    this.googleName = '';
    this.googleModalError = '';
    this.showGoogleModal = true;
  }

  closeGoogleModal(): void {
    if (this.googleLoading) return;
    this.showGoogleModal = false;
    this.googleModalError = '';
  }

  onConfirmGoogleModal(): void {
    this.googleModalError = '';
    const cleanEmail = (this.googleEmail || '').trim().toLowerCase();
    if (!cleanEmail || !this.isValidEmail(cleanEmail)) {
      this.googleModalError = 'Ingrese un correo de Google v\u00e1lido (ej: usuario@gmail.com).';
      return;
    }
    const cleanName = (this.googleName || '').trim();
    this.executeGoogleBackendLogin({
      email: cleanEmail,
      name: cleanName
    });
  }

  private executeGoogleBackendLogin(payload: { credential?: string; accessToken?: string; email?: string; name?: string }): void {
    this.googleLoading = true;
    this.authService.loginConGoogle(payload).subscribe({
      next: (response) => {
        this.googleLoading = false;
        this.showGoogleModal = false;
        this.redirectAfterAuth(response);
      },
      error: (err) => {
        this.googleLoading = false;
        const msg = err.error?.message || 'No se pudo iniciar sesi\u00f3n con Google.';
        if (this.showGoogleModal) {
          this.googleModalError = msg;
        } else {
          this.loginError = msg;
        }
      }
    });
  }

  consultarDniEnLogin(fillGoogleName: boolean = false): void {
    this.dniLookupError = '';
    this.dniLookupMsg = '';
    const clean = (this.loginDni || '').trim();
    if (!/^\d{8}$/.test(clean)) {
      this.dniLookupError = 'Ingresa un DNI válido de 8 dígitos.';
      return;
    }
    this.buscandoDni = true;
    this.sunatService.consultarDni(clean).subscribe({
      next: (res) => {
        this.buscandoDni = false;
        if (res?.nombreCompleto) {
          this.loginDniNombre = res.nombreCompleto;
          this.dniLookupMsg = `Titular RENIEC: ${res.nombreCompleto}`;
          if (fillGoogleName && !this.googleName.trim()) {
            this.googleName = res.nombreCompleto;
          }
        }
      },
      error: (err) => {
        this.buscandoDni = false;
        this.dniLookupError = err?.error?.message || 'No se encontraron datos en RENIEC para ese DNI.';
      }
    });
  }

  private redirectAfterAuth(response: any): void {
    if (!response?.usuario?.role || (response.usuario.role !== 'ADMIN' && response.usuario.role !== 'CUSTOMER')) {
      this.loginError = 'Usuario sin rol asignado. Contacte al administrador.';
      return;
    }
    const destino = response.usuario.role === 'ADMIN' ? '/panel-admin' : '/menu';
    const cleanDni = (this.loginDni || '').trim();
    if (/^\d{8}$/.test(cleanDni) && this.loginAgeVerified) {
      this.sunatService.configurarDni(cleanDni, true, this.loginDniNombre || undefined).subscribe({
        next: (updatedUser) => {
          this.authService.updateCurrentUser(updatedUser);
          this.router.navigate([destino]);
        },
        error: () => {
          this.router.navigate([destino]);
        }
      });
      return;
    }
    this.router.navigate([destino]);
  }

  openRecoveryModal(): void {
    this.loginError = '';
    this.loginSuccess = '';
    this.recoveryEmail = this.usuario.email ? this.usuario.email.trim() : '';
    this.recoveryMaskedEmail = '';
    this.recoveryCode = '';
    this.recoveryNewPassword = '';
    this.recoveryConfirmPassword = '';
    this.recoveryError = '';
    this.recoveryInfoMessage = '';
    this.recoveryStep = 1;
    this.showRecoveryModal = true;
  }

  closeRecoveryModal(): void {
    if (this.recoveryLoading) return;
    this.showRecoveryModal = false;
    this.recoveryError = '';
  }

  onRequestRecoveryCode(): void {
    this.recoveryError = '';
    this.recoveryInfoMessage = '';
    const cleanEmail = (this.recoveryEmail || '').trim().toLowerCase();
    if (!cleanEmail || !this.isValidEmail(cleanEmail)) {
      this.recoveryError = 'Por favor, ingrese un correo electr\u00f3nico v\u00e1lido.';
      return;
    }

    this.recoveryLoading = true;
    this.authService.solicitarRecuperacionPassword(cleanEmail).subscribe({
      next: (res) => {
        this.recoveryLoading = false;
        this.recoveryMaskedEmail = res?.maskedEmail || cleanEmail;
        this.recoveryInfoMessage = res?.message || ('Enviamos un c\u00f3digo de 6 d\u00edgitos a ' + this.recoveryMaskedEmail);
        this.recoveryStep = 2;
      },
      error: (err) => {
        this.recoveryLoading = false;
        this.recoveryError = err.error?.message || 'No se pudo enviar el c\u00f3digo de recuperaci\u00f3n.';
      }
    });
  }

  onResendRecoveryCode(): void {
    if (this.recoveryLoading) return;
    this.recoveryCode = '';
    this.onRequestRecoveryCode();
  }

  onBackToRecoveryStep1(): void {
    if (this.recoveryLoading) return;
    this.recoveryStep = 1;
    this.recoveryError = '';
    this.recoveryInfoMessage = '';
  }

  onConfirmPasswordReset(): void {
    this.recoveryError = '';
    const cleanEmail = (this.recoveryEmail || '').trim().toLowerCase();
    const cleanCode = (this.recoveryCode || '').trim();

    if (!/^\d{6}$/.test(cleanCode)) {
      this.recoveryError = 'Ingrese el c\u00f3digo de 6 d\u00edgitos enviado a su correo.';
      return;
    }
    if (!this.recoveryNewPassword || !this.isValidPassword(this.recoveryNewPassword)) {
      this.recoveryError = 'La nueva contrase\u00f1a debe tener entre 6 y 20 caracteres.';
      return;
    }
    if (this.recoveryNewPassword !== this.recoveryConfirmPassword) {
      this.recoveryError = 'Las contrase\u00f1as no coinciden.';
      return;
    }

    this.recoveryLoading = true;
    this.authService.confirmarRecuperacionPassword(cleanEmail, cleanCode, this.recoveryNewPassword).subscribe({
      next: (res) => {
        this.recoveryLoading = false;
        this.showRecoveryModal = false;
        this.usuario.email = cleanEmail;
        this.usuario.password = '';
        this.loginSuccess = res?.message || '\u00a1Contrase\u00f1a restablecida con \u00e9xito! Ahora puedes iniciar sesi\u00f3n.';
      },
      error: (err) => {
        this.recoveryLoading = false;
        this.recoveryError = err.error?.message || 'No se pudo restablecer la contrase\u00f1a. Verifique el c\u00f3digo.';
      }
    });
  }

  onEmailChange(): void {
    if (this.fieldErrors['email']) {
      delete this.fieldErrors['email'];
    }
  }

  onPasswordChange(): void {
    if (this.fieldErrors['password']) {
      delete this.fieldErrors['password'];
    }
  }
}
