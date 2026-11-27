import { Component, OnInit, HostListener, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { AccessibilityService, ColorblindMode, FontSizeMode } from '../../services/accessibility.service';

@Component({
  selector: 'app-accessibility-fab',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './accessibility-fab.html',
  styleUrls: ['./accessibility-fab.css']
})
export class AccessibilityFabComponent implements OnInit {
  isPanelOpen = false;
  isMobile = false;

  readonly colorblindModes: ColorblindMode[] = ['none', 'protanopia', 'deuteranopia', 'tritanopia'];
  readonly fontSizes: FontSizeMode[] = ['pequena', 'normal', 'grande', 'extra-grande'];
  readonly mobileFontSizes: FontSizeMode[] = ['pequena', 'normal', 'grande'];

  contrasteAlto = false;
  modoOscuro = false;
  modoColorblind: ColorblindMode = 'none';
  tamanoFuente: FontSizeMode = 'normal';
  modoDislexia = false;
  reducirMovimiento = false;

  constructor(
    public readonly accessibilityService: AccessibilityService,
    @Inject(PLATFORM_ID) private readonly platformId: Object
  ) {}

  ngOnInit(): void {
    if (isPlatformBrowser(this.platformId)) {
      this.detectViewport();
      this.cargarEstadosAccesibilidad();
    }
  }

  @HostListener('window:resize')
  onWindowResize(): void {
    if (isPlatformBrowser(this.platformId)) {
      this.detectViewport();
    }
  }

  private detectViewport(): void {
    this.isMobile = window.innerWidth <= 768;
    if (this.isMobile && this.accessibilityService.shortcutsGuideState) {
      this.accessibilityService.toggleShortcutsGuide();
    }
  }

  togglePanel(): void {
    this.isPanelOpen = !this.isPanelOpen;
    const estado = this.isPanelOpen ? 'abierto' : 'cerrado';
    this.accessibilityService.announce(`Panel de accesibilidad ${estado}`, 'polite');
  }

  closePanel(): void {
    this.isPanelOpen = false;
  }

  @HostListener('document:keydown.escape')
  onEscapeKey(): void {
    if (this.isPanelOpen) {
      this.closePanel();
    }
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    const target = event.target as HTMLElement;
    if (this.isPanelOpen && !target.closest('.accessibility-fab-container')) {
      this.closePanel();
    }
  }

  cargarEstadosAccesibilidad(): void {
    this.contrasteAlto = this.accessibilityService.getHighContrast();
    this.modoOscuro = this.accessibilityService.getDarkMode();
    this.modoColorblind = this.accessibilityService.getColorblindMode();
    this.tamanoFuente = this.accessibilityService.getFontSize();
    this.modoDislexia = this.accessibilityService.getDyslexiaMode();
    this.reducirMovimiento = this.accessibilityService.getReduceMotion();
  }

  toggleDarkMode(): void {
    this.modoOscuro = this.accessibilityService.toggleDarkMode();
  }

  toggleAltoContraste(): void {
    this.contrasteAlto = this.accessibilityService.toggleHighContrast();
  }

  setColorblindMode(mode: ColorblindMode): void {
    this.modoColorblind = mode;
    this.accessibilityService.setColorblindMode(mode);
  }

  setFontSize(size: FontSizeMode): void {
    this.tamanoFuente = size;
    this.accessibilityService.setFontSize(size);
  }

  toggleDyslexiaMode(): void {
    this.modoDislexia = !this.modoDislexia;
    this.accessibilityService.setDyslexiaMode(this.modoDislexia);
  }

  toggleReduceMotion(): void {
    this.reducirMovimiento = !this.reducirMovimiento;
    this.accessibilityService.setReduceMotion(this.reducirMovimiento);
  }

  toggleGuia(): void {
    this.accessibilityService.toggleShortcutsGuide();
    const currentState = this.accessibilityService.shortcutsGuideState;
    this.accessibilityService.announce(
      currentState ? 'Guía global de atajos activada' : 'Guía global de atajos desactivada'
    );
  }

  resetAccesibilidad(): void {
    if (this.contrasteAlto) {
      this.contrasteAlto = this.accessibilityService.toggleHighContrast();
    }
    if (this.modoOscuro) {
      this.modoOscuro = false;
      this.accessibilityService.setDarkMode(false);
    }
    this.setColorblindMode('none');
    this.setFontSize('normal');
    if (this.modoDislexia) {
      this.modoDislexia = false;
      this.accessibilityService.setDyslexiaMode(false);
    }
    if (this.reducirMovimiento) {
      this.reducirMovimiento = false;
      this.accessibilityService.setReduceMotion(false);
    }
    this.accessibilityService.announce('Preferencias de accesibilidad restablecidas', 'polite');
  }

  get tieneCambiosActivos(): boolean {
    return (
      this.contrasteAlto ||
      this.modoOscuro ||
      this.modoColorblind !== 'none' ||
      this.tamanoFuente !== 'normal' ||
      this.modoDislexia ||
      this.reducirMovimiento
    );
  }

  getColorblindLabel(mode: ColorblindMode): string {
    const labels: Record<ColorblindMode, string> = {
      'none': 'Sin filtro',
      'protanopia': 'Protanopia',
      'deuteranopia': 'Deuteranopia',
      'tritanopia': 'Tritanopia'
    };
    return labels[mode];
  }

  getFontSizeLabel(size: FontSizeMode): string {
    const labels: Record<FontSizeMode, string> = {
      'pequena': 'Pequeña',
      'normal': 'Normal',
      'grande': 'Grande',
      'extra-grande': 'Muy grande'
    };
    return labels[size];
  }

  getCompactFontLabel(size: FontSizeMode): string {
    const labels: Record<FontSizeMode, string> = {
      'pequena': 'A-',
      'normal': 'A',
      'grande': 'A+',
      'extra-grande': 'A++'
    };
    return labels[size];
  }
}
