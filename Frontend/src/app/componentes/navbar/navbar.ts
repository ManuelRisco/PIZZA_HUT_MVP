import { Component, OnInit, OnDestroy, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
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
  usuarioActual: any = null;
  esAdmin: boolean = false;
  private userSubscription?: Subscription;
  private cartSubscription?: Subscription;
  cartCount = 0;

  // Idioma
  idiomaActual: string = 'es';
  mostrarSelectorIdioma: boolean = false;
  idiomas: { code: string; name: string; flag: string }[] = [];

  // Estado para Menú Móvil
  isMobileMenuOpen: boolean = false;

  constructor(
    private authService: AuthService,
    private cartService: CartService,
    private router: Router,
    private accessibilityService: AccessibilityService,
    private translateService: TranslateService
  ) {}

  ngOnInit() {
    // Suscribirse a los cambios del usuario
    this.userSubscription = this.authService.currentUser$.subscribe(user => {
      this.usuarioActual = user;
      this.esAdmin = user?.role === 'ADMIN';
    });

    this.cartSubscription = this.cartService.items$.subscribe(() => {
      this.cartCount = this.cartService.getItemCount();
    });

    // Anunciar cuando el componente está listo
    this.accessibilityService.announce('Barra de navegación cargada. Use Alt+M para Menú, Alt+C para Carrito, Alt+P para Perfil', 'polite');

    // Cargar idiomas
    this.idiomas = this.translateService.getAvailableLanguages();
    this.idiomaActual = this.translateService.getCurrentLang();

    // Pre-cargar Google Translate si ya se seleccionó un idioma diferente
    if (this.idiomaActual !== 'es') {
      this.translateService.loadGoogleTranslate();
    }
  }

  ngOnDestroy() {
    // Limpiar la suscripción cuando se destruya el componente
    if (this.userSubscription) {
      this.userSubscription.unsubscribe();
    }
    if (this.cartSubscription) {
      this.cartSubscription.unsubscribe();
    }
  }

  toggleMobileMenu() {
    this.isMobileMenuOpen = !this.isMobileMenuOpen;
    // Cerrar otros menús si se abre el móvil
    if (this.isMobileMenuOpen) {
      this.mostrarMenuUsuario = false;
      this.mostrarSelectorIdioma = false;
    }
  }

  toggleMenuUsuario() {
    this.mostrarMenuUsuario = !this.mostrarMenuUsuario;
    const estado = this.mostrarMenuUsuario ? 'abierto' : 'cerrado';
    this.accessibilityService.announce(`Menú de usuario ${estado}`, 'polite');
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
    // Cerrar menú móvil al hacer click fuera
    if (!target.closest('.navbar-links') && !target.closest('.hamburger-btn') && this.isMobileMenuOpen) {
      this.isMobileMenuOpen = false;
    }
  }

  @HostListener('document:keydown', ['$event'])
  handleKeyboardShortcuts(event: KeyboardEvent) {
    // Solo si es Alt + tecla
    if (!event.altKey) return;

    switch (event.key.toUpperCase()) {
      case 'M':
        event.preventDefault();
        this.router.navigate(['/menu']);
        this.accessibilityService.announceNavigation('Menú');
        break;
      case 'C':
        event.preventDefault();
        this.router.navigate(['/cart']);
        this.accessibilityService.announceNavigation('Carrito');
        break;
      case 'P':
        event.preventDefault();
        this.toggleMenuUsuario();
        break;
      case 'E':
        event.preventDefault();
        this.router.navigate(['/extras']);
        this.accessibilityService.announceNavigation('Extras');
        break;
      case 'O':
        event.preventDefault();
        this.router.navigate(['/promociones']);
        this.accessibilityService.announceNavigation('Promociones');
        break;
    }
  }

  cerrarSesion() {
    if (confirm('¿Estás seguro de que deseas cerrar sesión?')) {
      this.authService.logout();
      this.usuarioActual = null;
      this.esAdmin = false;
      this.mostrarMenuUsuario = false;
      this.router.navigate(['/']);
      this.accessibilityService.announceSuccess('Sesión cerrada correctamente');
    }
  }

  navegarA(ruta: string) {
    this.mostrarMenuUsuario = false;
    this.isMobileMenuOpen = false;

    // Si es la ruta de perfil del admin, navegar al panel y cambiar vista
    if (ruta === '/admin/perfil') {
      this.router.navigate(['/admin/dashboard']).then(() => {
        // Emitir evento o llamar método del panel admin para cambiar a vista 'mi-perfil'
        // Por ahora navegamos al dashboard y el admin puede hacer clic en "Mi Perfil" en el sidebar
      });
    } else {
      this.router.navigate([ruta]);
    }
  }

  getIniciales(): string {
    if (!this.usuarioActual?.name) return 'U';

    // Obtener solo el primer nombre
    const primerNombre = this.usuarioActual.name.trim().split(' ')[0];

    // Retornar las primeras 2 letras del primer nombre en mayúsculas
    return primerNombre.substring(0, 2).toUpperCase();
  }

  // ===== IDIOMA =====

  toggleSelectorIdioma(): void {
    this.mostrarMenuUsuario = false;
    this.mostrarSelectorIdioma = !this.mostrarSelectorIdioma;
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
    return idioma ? idioma.flag : '🇪🇸';
  }
}
