import { Injectable } from '@angular/core';
import { Subject, BehaviorSubject } from 'rxjs';

export type ColorblindMode = 'none' | 'protanopia' | 'deuteranopia' | 'tritanopia';
export type FontSizeMode = 'pequena' | 'normal' | 'grande' | 'extra-grande';

export interface AccessibilityAnnouncement {
  message: string;
  type: 'polite' | 'assertive';
  duration?: number;
}

@Injectable({
  providedIn: 'root'
})
export class AccessibilityService {
  private readonly announceSubject= new Subject<AccessibilityAnnouncement>();
  public announce$ = this.announceSubject.asObservable();

  private readonly shortcutsGuideSubject = new BehaviorSubject<boolean>(
    this.getInitialShortcutsState()
  );
  public shortcutsGuide$ = this.shortcutsGuideSubject.asObservable();

  private getInitialShortcutsState(): boolean {
    if (typeof window !== 'undefined' && window.localStorage) {
      return localStorage.getItem('accessibility_shortcuts_guide') === 'true';
    }
    return false;
  }

  public toggleShortcutsGuide(): void {
    const newState = !this.shortcutsGuideSubject.value;
    this.shortcutsGuideSubject.next(newState);
    if (typeof window !== 'undefined' && window.localStorage) {
      localStorage.setItem('accessibility_shortcuts_guide', newState.toString());
    }
  }

  public get shortcutsGuideState(): boolean {
    return this.shortcutsGuideSubject.value;
  }

  private liveRegionElement: HTMLDivElement | null = null;

  constructor() {
    this.initializeLiveRegion();
  }

  private initializeLiveRegion(): void {
    this.liveRegionElement = document.createElement('div');
    this.liveRegionElement.id = 'accessibility-announcements';
    this.liveRegionElement.setAttribute('aria-live', 'polite');
    this.liveRegionElement.setAttribute('aria-atomic', 'true');
    this.liveRegionElement.setAttribute('aria-relevant', 'text additions');
    this.liveRegionElement.className = 'visually-hidden';
    this.liveRegionElement.style.position = 'absolute';
    this.liveRegionElement.style.left = '-10000px';
    this.liveRegionElement.style.width = '1px';
    this.liveRegionElement.style.height = '1px';
    this.liveRegionElement.style.overflow = 'hidden';

    if (document.body) {
      document.body.appendChild(this.liveRegionElement);
    }
  }

  public announce(message: string, type: 'polite' | 'assertive' = 'polite'): void {
    if (this.liveRegionElement) {
      this.liveRegionElement.setAttribute('aria-live', type);
      this.liveRegionElement.textContent = message;

      const clearDelay = type === 'assertive' ? 2000 : 3000;
      setTimeout(() => {
        if (this.liveRegionElement) {
          this.liveRegionElement.textContent = '';
        }
      }, clearDelay);
    }

    this.announceSubject.next({ message, type });
  }

  public announceAddToCart(productName: string, quantity: number = 1): void {
    const message = quantity > 1
      ? `${quantity} ${productName}s agregadas al carrito`
      : `${productName} agregada al carrito`;
    this.announce(message, 'polite');
  }

  public announceRemoveFromCart(productName: string): void {
    this.announce(`${productName} removida del carrito`, 'polite');
  }

  public announceQuantityChange(quantity: number, productName: string): void {
    this.announce(`Cantidad de ${productName}: ${quantity}`, 'polite');
  }

  public announceCartTotal(total: number): void {
    this.announce(`Total del carrito: ${total.toFixed(2)} pesos`, 'polite');
  }

  public announceValidationError(fieldName: string, error: string): void {
    this.announce(`Error en ${fieldName}: ${error}`, 'assertive');
  }

  public announceError(message: string): void {
    this.announce(`Error: ${message}`, 'assertive');
  }

  public announceSuccess(message: string): void {
    this.announce(`Éxito: ${message}`, 'polite');
  }

  public announceAddedToFavorites(productName: string): void {
    this.announce(`${productName} agregada a favoritos`, 'polite');
  }

  public announceRemovedFromFavorites(productName: string): void {
    this.announce(`${productName} removida de favoritos`, 'polite');
  }

  public announceNavigation(pageName: string): void {
    this.announce(`Navegando a ${pageName}`, 'polite');
  }

  public announceFormSubmitted(): void {
    this.announce(`Formulario enviado correctamente`, 'polite');
  }

  public getLiveRegionElement(): HTMLDivElement | null {
    return this.liveRegionElement;
  }

  public setHighContrast(active: boolean): void {
    if (typeof document !== 'undefined') {
      const html = document.documentElement;

      if (active) {
        html.dataset['contraste'] = 'alto';
      } else {
        delete html.dataset['contraste'];
      }

      if (typeof localStorage !== 'undefined') {
        localStorage.setItem('alta-contraste', active ? 'true' : 'false');
      }

      this.announce(active ? 'Filtro de alto contraste activado' : 'Filtro de alto contraste desactivado', 'polite');
    }
  }

  public toggleHighContrast(): boolean {
    const newState = !this.getHighContrast();
    this.setHighContrast(newState);
    return newState;
  }

  public getHighContrast(): boolean {
    if (typeof document !== 'undefined') {
      return document.documentElement.dataset['contraste'] === 'alto';
    }
    return false;
  }

  public setColorblindMode(mode: ColorblindMode): void {
    if (typeof document !== 'undefined') {
      const html = document.documentElement;

      delete html.dataset['colorblind'];

      if (mode !== 'none') {
        html.dataset['colorblind'] = mode;
      }

      if (typeof localStorage !== 'undefined') {
        localStorage.setItem('colorblind-mode', mode);
      }

      const mensajes: { [key: string]: string } = {
        'none': 'Filtro de daltonismo desactivado',
        'protanopia': 'Filtro para protanopia (rojo-verde) activado',
        'deuteranopia': 'Filtro para deuteranopia (rojo-verde) activado',
        'tritanopia': 'Filtro para tritanopia (azul-amarillo) activado'
      };

      this.announce(mensajes[mode] || 'Modo de visión actualizado', 'polite');
    }
  }

  public getColorblindMode(): ColorblindMode {
    if (typeof document !== 'undefined') {
      const mode = document.documentElement.dataset['colorblind'];
      if (mode === 'protanopia' || mode === 'deuteranopia' || mode === 'tritanopia') {
        return mode;
      }
    }
    return 'none';
  }

  public setFontSize(size: FontSizeMode): void {
    if (typeof document !== 'undefined') {
      const html = document.documentElement;

      delete html.dataset['fontSize'];

      if (size !== 'normal') {
        html.dataset['fontSize'] = size;
      }

      if (typeof localStorage !== 'undefined') {
        localStorage.setItem('font-size', size);
      }

      const mensajes: { [key: string]: string } = {
        'pequena': 'Tamaño de fuente pequeño',
        'normal': 'Tamaño de fuente normal',
        'grande': 'Tamaño de fuente grande',
        'extra-grande': 'Tamaño de fuente extra grande'
      };

      this.announce(mensajes[size] || 'Tamaño de fuente actualizado', 'polite');
    }
  }

  public getFontSize(): FontSizeMode {
    if (typeof document !== 'undefined') {
      const size = document.documentElement.dataset['fontSize'];
      if (size === 'pequena' || size === 'grande' || size === 'extra-grande') {
        return size;
      }
    }
    return 'normal';
  }

  public setDyslexiaMode(active: boolean): void {
    if (typeof document !== 'undefined') {
      const html = document.documentElement;
      if (active) {
        html.dataset['dyslexia'] = 'true';
      } else {
        delete html.dataset['dyslexia'];
      }
      if (typeof localStorage !== 'undefined') {
        localStorage.setItem('dyslexia-mode', active ? 'true' : 'false');
      }
      this.announce(active ? 'Modo lectura fácil activado' : 'Modo lectura fácil desactivado', 'polite');
    }
  }

  public getDyslexiaMode(): boolean {
    if (typeof document !== 'undefined') {
      return document.documentElement.dataset['dyslexia'] === 'true';
    }
    return false;
  }

  public setReduceMotion(active: boolean): void {
    if (typeof document !== 'undefined') {
      const html = document.documentElement;
      if (active) {
        html.dataset['reduceMotion'] = 'true';
      } else {
        delete html.dataset['reduceMotion'];
      }
      if (typeof localStorage !== 'undefined') {
        localStorage.setItem('reduce-motion', active ? 'true' : 'false');
      }
      this.announce(active ? 'Reducción de movimiento activada' : 'Reducción de movimiento desactivada', 'polite');
    }
  }

  public getReduceMotion(): boolean {
    if (typeof document !== 'undefined') {
      return document.documentElement.dataset['reduceMotion'] === 'true';
    }
    return false;
  }
}
