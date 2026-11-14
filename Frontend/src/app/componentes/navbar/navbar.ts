import { Component, OnInit, OnDestroy, HostListener } from '@angular/core';
import { CommonModule, Location } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { CartService } from '../../services/cart.service';
import { AccessibilityService } from '../../services/accessibility.service';
import { TranslateService } from '../../services/translate.service';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './navbar.html',
  styleUrls: ['./navbar.css']
})
export class NavbarComponent implements OnInit, OnDestroy {
  mostrarMenuUsuario: boolean = false;
  mostrarModalLogout: boolean = false;
  usuarioActual: any = null;
  esAdmin: boolean = false;
  private userSubscription?: Subscription;
  private cartSubscription?: Subscription;
  cartCount = 0;

  // Idioma
  idiomaActual: string = 'es';
  mostrarSelectorIdioma: boolean = false;
  idiomas: { code: string; name: string; flag: string }[] = [];

  // Estado para Menu Movil
  isMobileMenuOpen: boolean = false;

  constructor(
    private readonly authService: AuthService,
    private readonly cartService: CartService,
    private readonly router: Router,
    private readonly location: Location,
    private readonly accessibilityService: AccessibilityService,
    private readonly translateService: TranslateService
  ) {}

  ngOnInit() {
    this.userSubscription = this.authService.currentUser$.subscribe(user => {
      this.usuarioActual = user;
      this.esAdmin = user?.role === 'ADMIN';
    });

    this.cartSubscription = this.cartService.items$.subscribe(() => {
      this.cartCount = this.cartService.getItemCount();
    });

    this.accessibilityService.announce(
      'Barra de navegaci\u00f3n cargada. Use Alt+M para Men\u00fa, Alt+C para Carrito, Alt+P para Perfil',
      'polite'
    );

    this.idiomas = this.translateService.getAvailableLanguages();
    this.idiomaActual = this.translateService.getCurrentLang();

    if (this.idiomaActual !== 'es') {
      this.translateService.loadGoogleTranslate();
    }
  }

  ngOnDestroy() {
    if (this.userSubscription) {
      this.userSubscription.unsubscribe();
    }
    if (this.cartSubscription) {
      this.cartSubscription.unsubscribe();
    }
  }

  toggleMobileMenu() {
    this.isMobileMenuOpen = !this.isMobileMenuOpen;
    if (this.isMobileMenuOpen) {
      this.mostrarMenuUsuario = false;
      this.mostrarSelectorIdioma = false;
    }
  }

  toggleMenuUsuario() {
    this.mostrarMenuUsuario = !this.mostrarMenuUsuario;
    if (this.mostrarMenuUsuario) {
      this.mostrarSelectorIdioma = false;
      setTimeout(() => {
        const menu = document.querySelector('.user-dropdown');
        if (menu) {
          const firstItem = menu.querySelector('button, a, input, [tabindex]:not([tabindex="-1"])') as HTMLElement;
          if (firstItem) {
            firstItem.focus();
          }
        }
      }, 0);
    }
    const estado = this.mostrarMenuUsuario ? 'abierto' : 'cerrado';
    this.accessibilityService.announce(`Men\u00fa de usuario ${estado}`, 'polite');
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent) {
    const target = event.target as HTMLElement;
    if (!target.closest('.user-menu-container')) {
      this.mostrarMenuUsuario = false;
    }
    if (!target.closest('.language-menu-container')) {
      this.mostrarSelectorIdioma = false;
    }
    if (!target.closest('.navbar-links') && !target.closest('.hamburger-btn') && this.isMobileMenuOpen) {
      this.isMobileMenuOpen = false;
    }
  }

  @HostListener('document:keydown', ['$event'])
  handleKeyboardShortcuts(event: KeyboardEvent) {
    if (!event.altKey) return;

    switch (event.key.toUpperCase()) {
      case 'M':
        event.preventDefault();
        this.navegarA('/menu');
        this.accessibilityService.announceNavigation('Men\u00fa');
        break;
      case 'C':
        event.preventDefault();
        this.navegarA('/cart');
        this.accessibilityService.announceNavigation('Carrito');
        break;
      case 'P':
        event.preventDefault();
        this.toggleMenuUsuario();
        break;
      case 'E':
        event.preventDefault();
        this.navegarA('/extras');
        this.accessibilityService.announceNavigation('Extras');
        break;
      case 'O':
        event.preventDefault();
        this.navegarA('/promociones');
        this.accessibilityService.announceNavigation('Promociones');
        break;
      case 'I':
        event.preventDefault();
        this.navegarA('/');
        this.accessibilityService.announceNavigation('Inicio');
        break;
      case 'U':
        event.preventDefault();
        this.navegarA('/ubicacion');
        this.accessibilityService.announceNavigation('Ubicaci\u00f3n');
        break;
      case 'L':
        event.preventDefault();
        this.toggleSelectorIdioma();
        break;
      case 'Y': {
        event.preventDefault();
        this.accessibilityService.toggleShortcutsGuide();
        const estadoGuia = this.accessibilityService.shortcutsGuideState ? 'abierta' : 'cerrada';
        this.accessibilityService.announce(`Gu\u00eda de atajos ${estadoGuia}`, 'polite');
        break;
      }
      case 'S':
        if (!this.usuarioActual) {
          event.preventDefault();
          this.navegarA('/join');
          this.accessibilityService.announceNavigation('Iniciar Sesi\u00f3n');
        }
        break;
      case 'R':
        if (!this.usuarioActual) {
          event.preventDefault();
          this.navegarA('/register');
          this.accessibilityService.announceNavigation('Registro de cuenta');
        }
        break;
      case 'D':
        if (this.usuarioActual) {
          event.preventDefault();
          const ruta = this.esAdmin ? '/admin/dashboard' : '/cliente/dashboard';
          this.navegarA(ruta);
          this.accessibilityService.announceNavigation('Panel de control');
        }
        break;
    }
  }

  cerrarSesion() {
    this.mostrarMenuUsuario = false;
    this.isMobileMenuOpen = false;
    this.mostrarModalLogout = true;
  }

  cancelarLogout() {
    this.mostrarModalLogout = false;
  }

  confirmarLogout() {
    this.mostrarModalLogout = false;
    sessionStorage.removeItem('adminVistaActual');
    sessionStorage.removeItem('clienteVistaActual');
    this.authService.logout();
    this.usuarioActual = null;
    this.esAdmin = false;
    this.mostrarMenuUsuario = false;
    this.router.navigate(['/']);
    this.accessibilityService.announceSuccess('Sesi\u00f3n cerrada correctamente');
  }

  navegarA(ruta: string) {
    this.mostrarMenuUsuario = false;
    this.isMobileMenuOpen = false;
    this.router.navigate([ruta]).then(navigated => {
      if (navigated === false || this.location.path() !== ruta) {
        this.location.go(ruta);
      }
    });
  }

  getIniciales(): string {
    if (!this.usuarioActual?.name) return 'U';
    const primerNombre = this.usuarioActual.name.trim().split(' ')[0];
    return primerNombre.substring(0, 2).toUpperCase();
  }

  toggleSelectorIdioma(): void {
    this.mostrarMenuUsuario = false;
    this.mostrarSelectorIdioma = !this.mostrarSelectorIdioma;
    if (this.mostrarSelectorIdioma) {
      setTimeout(() => {
        const menu = document.querySelector('.language-dropdown');
        if (menu) {
          const firstItem = menu.querySelector('button, a, input, [tabindex]:not([tabindex="-1"])') as HTMLElement;
          if (firstItem) {
            firstItem.focus();
          }
        }
      }, 0);
    }
  }

  cambiarIdioma(langCode: string): void {
    this.idiomaActual = langCode;
    this.mostrarSelectorIdioma = false;
    this.translateService.changeLanguage(langCode);

    const idioma = this.idiomas.find(i => i.code === langCode);
    this.accessibilityService.announce(`Idioma cambiado a ${idioma?.name || langCode}`, 'polite');
  }

  getIdiomaActualFlag(): string {
    const idioma = this.idiomas.find(i => i.code === this.idiomaActual);
    return idioma ? idioma.flag : '\uD83C\uDDEA\uD83C\uDDF8';
  }
}
