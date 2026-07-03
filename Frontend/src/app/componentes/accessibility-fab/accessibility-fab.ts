import { Component, OnInit, HostListener, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { AccessibilityService } from '../../services/accessibility.service';

@Component({
  selector: 'app-accessibility-fab',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './accessibility-fab.html',
  styleUrls: ['./accessibility-fab.css']
})
export class AccessibilityFabComponent implements OnInit {
  isPanelOpen: boolean = false;

  // Estados de accesibilidad
  contrasteAlto: boolean = false;
  modoColorblind: 'none' | 'protanopia' | 'deuteranopia' | 'tritanopia' = 'none';
  tamanoFuente: 'pequena' | 'normal' | 'grande' | 'extra-grande' = 'normal';
  modoDislexia: boolean = false;
  reducirMovimiento: boolean = false;

  constructor(
    private readonly accessibilityService: AccessibilityService,
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
    if (typeof window !== 'undefined') {
      this.contrasteAlto = (window as any).getEstadoContraste?.() || false;
      this.modoColorblind = ((window as any).getColorblindMode?.() || 'none') as any;
      this.tamanoFuente = ((window as any).getFontSize?.() || 'normal') as any;
      this.modoDislexia = (window as any).getDyslexiaMode?.() || false;
      this.reducirMovimiento = (window as any).getReduceMotion?.() || false;
    }
  }

  toggleAltoContraste(): void {
    if (typeof window !== 'undefined' && (window as any).toggleAltoContraste) {
      (window as any).toggleAltoContraste();
      this.contrasteAlto = (window as any).getEstadoContraste();
      const estado = this.contrasteAlto ? 'activado' : 'desactivado';
      this.accessibilityService.announce(`Alto contraste ${estado}`, 'polite');
    }
  }

  setColorblindMode(mode: 'none' | 'protanopia' | 'deuteranopia' | 'tritanopia'): void {
    if (typeof window !== 'undefined' && (window as any).setColorblindMode) {
      (window as any).setColorblindMode(mode);
      this.modoColorblind = mode;
      this.accessibilityService.setColorblindMode(mode);
    }
  }

  setFontSize(size: 'pequena' | 'normal' | 'grande' | 'extra-grande'): void {
    if (typeof window !== 'undefined' && (window as any).setFontSize) {
      (window as any).setFontSize(size);
      this.tamanoFuente = size;
      this.accessibilityService.setFontSize(size);
    }
  }

  toggleDyslexiaMode(): void {
    if (typeof window !== 'undefined' && (window as any).toggleDyslexiaMode) {
      (window as any).toggleDyslexiaMode();
      this.modoDislexia = (window as any).getDyslexiaMode();
      this.accessibilityService.setDyslexiaMode(this.modoDislexia);
    }
  }

  toggleReduceMotion(): void {
    if (typeof window !== 'undefined' && (window as any).toggleReduceMotion) {
      (window as any).toggleReduceMotion();
      this.reducirMovimiento = (window as any).getReduceMotion();
      this.accessibilityService.setReduceMotion(this.reducirMovimiento);
    }
  }

  // ===== LABELS =====

  getColorblindLabel(mode: string): string {
    const labels: { [key: string]: string } = {
      'none': 'Sin filtro',
      'protanopia': 'Protanopia (Rojo-Verde)',
      'deuteranopia': 'Deuteranopia (Rojo-Verde)',
      'tritanopia': 'Tritanopia (Azul-Amarillo)'
    };
    return labels[mode] || mode;
  }

  getFontSizeLabel(size: string): string {
    const labels: { [key: string]: string } = {
      'pequena': 'PequeÃ±a (90%)',
      'normal': 'Normal',
      'grande': 'Grande (125%)',
      'extra-grande': 'Extra Grande (150%)'
    };
    return labels[size] || size;
  }
}

