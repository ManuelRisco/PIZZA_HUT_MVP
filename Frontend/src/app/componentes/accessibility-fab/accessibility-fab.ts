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
  isPanelOpen: boolean = false;

  readonly colorblindModes: ColorblindMode[] = ['none', 'protanopia', 'deuteranopia', 'tritanopia'];
  readonly fontSizes: FontSizeMode[] = ['pequena', 'normal', 'grande', 'extra-grande'];

  // Estados de accesibilidad
  contrasteAlto: boolean = false;
  modoColorblind: ColorblindMode = 'none';
  tamanoFuente: FontSizeMode = 'normal';
  modoDislexia: boolean = false;
  reducirMovimiento: boolean = false;

  constructor(
    public readonly accessibilityService: AccessibilityService,
    @Inject(PLATFORM_ID) private readonly platformId: Object
  ) {}

  ngOnInit(): void {
    if (isPlatformBrowser(this.platformId)) {
      this.cargarEstadosAccesibilidad();
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

  // ===== ACCESIBILIDAD =====

  cargarEstadosAccesibilidad(): void {
    this.contrasteAlto = this.accessibilityService.getHighContrast();
    this.modoColorblind = this.accessibilityService.getColorblindMode();
    this.tamanoFuente = this.accessibilityService.getFontSize();
    this.modoDislexia = this.accessibilityService.getDyslexiaMode();
    this.reducirMovimiento = this.accessibilityService.getReduceMotion();
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

  // ===== LABELS =====

  getColorblindLabel(mode: ColorblindMode): string {
    const labels: Record<ColorblindMode, string> = {
      'none': 'Sin filtro',
      'protanopia': 'Protanopia (Rojo-Verde)',
      'deuteranopia': 'Deuteranopia (Rojo-Verde)',
      'tritanopia': 'Tritanopia (Azul-Amarillo)'
    };
    return labels[mode];
  }

  getFontSizeLabel(size: FontSizeMode): string {
    const labels: Record<FontSizeMode, string> = {
      'pequena': 'Pequeña (90%)',
      'normal': 'Normal',
      'grande': 'Grande (125%)',
      'extra-grande': 'Extra Grande (150%)'
    };
    return labels[size];
  }
}
