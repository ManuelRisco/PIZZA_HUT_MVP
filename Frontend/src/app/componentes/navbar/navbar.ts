import { Component, OnInit, OnDestroy, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { CartService } from '../../services/cart.service';
import { AccessibilityService } from '../../services/accessibility.service';
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
  contrasteAlto: boolean = false;

  // Propiedades de accesibilidad
  mostrarMenuAccesibilidad: boolean = false;
  modoColorblind: 'none' | 'protanopia' | 'deuteranopia' | 'tritanopia' = 'none';
  tamanoFuente: 'pequena' | 'normal' | 'grande' | 'extra-grande' = 'normal';

  constructor(
    private authService: AuthService,
    private cartService: CartService,
    private router: Router,
    private accessibilityService: AccessibilityService
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

    // Verificar estado inicial del contraste
    this.verificarContraste();

    // Cargar estados de accesibilidad
    this.cargarEstadosAccesibilidad();

    // Anunciar cuando el componente está listo
    this.accessibilityService.announce('Barra de navegación cargada. Use Alt+M para Menú, Alt+C para Carrito, Alt+P para Perfil, Alt+A para Alto Contraste, Alt+S para Accesibilidad', 'polite');
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
      case 'A':
        event.preventDefault();
        this.toggleAltoContraste();
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

  toggleAltoContraste() {
    if (typeof window !== 'undefined' && (window as any).toggleAltoContraste) {
      (window as any).toggleAltoContraste();
      this.verificarContraste();
      const estado = this.contrasteAlto ? 'activado' : 'desactivado';
      this.accessibilityService.announce(`Alto contraste ${estado}`, 'polite');
    }
  }

  verificarContraste() {
    if (typeof window !== 'undefined' && (window as any).getEstadoContraste) {
      this.contrasteAlto = (window as any).getEstadoContraste();
    }
  }

  cargarEstadosAccesibilidad() {
    if (typeof window !== 'undefined') {
      // Cargar modo de daltonismo
      this.modoColorblind = ((window as any).getColorblindMode?.() || 'none') as any;
      // Cargar tamaño de fuente
      this.tamanoFuente = ((window as any).getFontSize?.() || 'normal') as any;
    }
  }

  toggleMenuAccesibilidad() {
    this.mostrarMenuUsuario = false; // Cerrar menú de usuario si está abierto
    this.mostrarMenuAccesibilidad = !this.mostrarMenuAccesibilidad;
    const estado = this.mostrarMenuAccesibilidad ? 'abierto' : 'cerrado';
    this.accessibilityService.announce(`Menú de accesibilidad ${estado}`, 'polite');
  }

  setColorblindMode(mode: 'none' | 'protanopia' | 'deuteranopia' | 'tritanopia') {
    if (typeof window !== 'undefined' && (window as any).setColorblindMode) {
      (window as any).setColorblindMode(mode);
      this.modoColorblind = mode;
      this.accessibilityService.setColorblindMode(mode);
    }
  }

  setFontSize(size: 'pequena' | 'normal' | 'grande' | 'extra-grande') {
    if (typeof window !== 'undefined' && (window as any).setFontSize) {
      (window as any).setFontSize(size);
      this.tamanoFuente = size;
      this.accessibilityService.setFontSize(size);
    }
  }

  /**
   * Obtiene label legible para los modos de daltonismo
   */
  getColorblindLabel(mode: string): string {
    const labels: { [key: string]: string } = {
      'none': 'Sin filtro',
      'protanopia': 'Protanopia (Rojo-Verde)',
      'deuteranopia': 'Deuteranopia (Rojo-Verde)',
      'tritanopia': 'Tritanopia (Azul-Amarillo)'
    };
    return labels[mode] || mode;
  }

  /**
   * Obtiene label legible para los tamaños de fuente
   */
  getFontSizeLabel(size: string): string {
    const labels: { [key: string]: string } = {
      'pequena': 'Pequeña (90%)',
      'normal': 'Normal',
      'grande': 'Grande (125%)',
      'extra-grande': 'Extra Grande (150%)'
    };
    return labels[size] || size;
  }
}
