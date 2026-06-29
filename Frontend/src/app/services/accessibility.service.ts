import { Injectable } from '@angular/core';
import { Subject } from 'rxjs';

export interface AccessibilityAnnouncement {
  message: string;
  type: 'polite' | 'assertive';
  duration?: number;
}

@Injectable({
  providedIn: 'root'
})
export class AccessibilityService {
  private announceSubject = new Subject<AccessibilityAnnouncement>();
  public announce$ = this.announceSubject.asObservable();

  private liveRegionElement: HTMLDivElement | null = null;

  constructor() {
    this.initializeLiveRegion();
  }

  /**
   * Inicializa la región en vivo global para anuncios
   */
  private initializeLiveRegion(): void {
    // Se crea un elemento invisible pero accesible para screen readers
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

    // Esperar a que el DOM esté listo
    if (document.body) {
      document.body.appendChild(this.liveRegionElement);
    }
  }

  /**
   * Anuncia un mensaje de forma accesible
   * @param message - Mensaje a anunciar
   * @param type - 'polite' (esperado) o 'assertive' (urgente)
   */
  public announce(message: string, type: 'polite' | 'assertive' = 'polite'): void {
    if (this.liveRegionElement) {
      this.liveRegionElement.setAttribute('aria-live', type);
      this.liveRegionElement.textContent = message;

      // Limpiar después de que se haya anunciado (si es polite, más tiempo)
      const clearDelay = type === 'assertive' ? 2000 : 3000;
      setTimeout(() => {
        if (this.liveRegionElement) {
          this.liveRegionElement.textContent = '';
        }
      }, clearDelay);
    }

    // Emitir para otros componentes que escuchen
    this.announceSubject.next({ message, type });
  }

  /**
   * Anuncia que se agregó un producto al carrito
   */
  public announceAddToCart(productName: string, quantity: number = 1): void {
    const message = quantity > 1
      ? `${quantity} ${productName}s agregadas al carrito`
      : `${productName} agregada al carrito`;
    this.announce(message, 'polite');
  }

  /**
   * Anuncia que se removió un producto del carrito
   */
  public announceRemoveFromCart(productName: string): void {
    this.announce(`${productName} removida del carrito`, 'polite');
  }

  /**
   * Anuncia actualización de cantidad
   */
  public announceQuantityChange(quantity: number, productName: string): void {
    this.announce(`Cantidad de ${productName}: ${quantity}`, 'polite');
  }

  /**
   * Anuncia el total del carrito
   */
  public announceCartTotal(total: number): void {
    this.announce(`Total del carrito: ${total.toFixed(2)} pesos`, 'polite');
  }

  /**
   * Anuncia error de validación
   */
  public announceValidationError(fieldName: string, error: string): void {
    this.announce(`Error en ${fieldName}: ${error}`, 'assertive');
  }

  /**
   * Anuncia error general (urgente)
   */
  public announceError(message: string): void {
    this.announce(`Error: ${message}`, 'assertive');
  }

  /**
   * Anuncia confirmación de acción
   */
  public announceSuccess(message: string): void {
    this.announce(`Éxito: ${message}`, 'polite');
  }

  /**
   * Anuncia que se añadió a favoritos
   */
  public announceAddedToFavorites(productName: string): void {
    this.announce(`${productName} agregada a favoritos`, 'polite');
  }

  /**
   * Anuncia que se removió de favoritos
   */
  public announceRemovedFromFavorites(productName: string): void {
    this.announce(`${productName} removida de favoritos`, 'polite');
  }

  /**
   * Anuncia navegación
   */
  public announceNavigation(pageName: string): void {
    this.announce(`Navegando a ${pageName}`, 'polite');
  }

  /**
   * Anuncia formulario enviado
   */
  public announceFormSubmitted(): void {
    this.announce(`Formulario enviado correctamente`, 'polite');
  }

  /**
   * Getter para el elemento live region (útil para testing)
   */
  public getLiveRegionElement(): HTMLDivElement | null {
    return this.liveRegionElement;
  }

  /**
   * Establece el modo de visión de color (filtro para daltonismo)
   * @param mode - 'none', 'protanopia', 'deuteranopia', 'tritanopia'
   */
  public setColorblindMode(mode: 'none' | 'protanopia' | 'deuteranopia' | 'tritanopia'): void {
    if (typeof document !== 'undefined') {
      const html = document.documentElement;

      // Remover atributo anterior
      html.removeAttribute('data-colorblind');

      // Establecer nuevo modo si no es 'none'
      if (mode !== 'none') {
        html.setAttribute('data-colorblind', mode);
      }

      // Guardar preferencia
      localStorage.setItem('colorblind-mode', mode);

      // Anunciar cambio
      const mensajes: { [key: string]: string } = {
        'none': 'Filtro de daltonismo desactivado',
        'protanopia': 'Filtro para protanopia (rojo-verde) activado',
        'deuteranopia': 'Filtro para deuteranopia (rojo-verde) activado',
        'tritanopia': 'Filtro para tritanopia (azul-amarillo) activado'
      };

      this.announce(mensajes[mode] || 'Modo de visión actualizado', 'polite');
    }
  }

  /**
   * Obtiene el modo actual de daltonismo
   */
  public getColorblindMode(): string {
    if (typeof document !== 'undefined') {
      const mode = document.documentElement.getAttribute('data-colorblind') || 'none';
      return mode;
    }
    return 'none';
  }

  /**
   * Establece el tamaño de fuente
   * @param size - 'pequena', 'normal', 'grande', 'extra-grande'
   */
  public setFontSize(size: 'pequena' | 'normal' | 'grande' | 'extra-grande'): void {
    if (typeof document !== 'undefined') {
      const html = document.documentElement;

      // Remover tamaño anterior
      html.removeAttribute('data-font-size');

      // Establecer nuevo tamaño si no es 'normal'
      if (size !== 'normal') {
        html.setAttribute('data-font-size', size);
      }

      // Guardar preferencia
      localStorage.setItem('font-size', size);

      // Anunciar cambio
      const mensajes: { [key: string]: string } = {
        'pequena': 'Tamaño de fuente pequeño',
        'normal': 'Tamaño de fuente normal',
        'grande': 'Tamaño de fuente grande',
        'extra-grande': 'Tamaño de fuente extra grande'
      };

      this.announce(mensajes[size] || 'Tamaño de fuente actualizado', 'polite');
    }
  }

  /**
   * Obtiene el tamaño de fuente actual
   */
  public getFontSize(): string {
    if (typeof document !== 'undefined') {
      const size = document.documentElement.getAttribute('data-font-size') || 'normal';
      return size;
    }
    return 'normal';
  }

  /**
   * Establece el modo dislexia
   */
  public setDyslexiaMode(active: boolean): void {
    if (typeof document !== 'undefined') {
      const html = document.documentElement;
      if (active) {
        html.setAttribute('data-dyslexia', 'true');
      } else {
        html.removeAttribute('data-dyslexia');
      }
      localStorage.setItem('dyslexia-mode', active ? 'true' : 'false');
      this.announce(active ? 'Modo lectura fácil activado' : 'Modo lectura fácil desactivado', 'polite');
    }
  }

  /**
   * Establece la reducción de movimiento
   */
  public setReduceMotion(active: boolean): void {
    if (typeof document !== 'undefined') {
      const html = document.documentElement;
      if (active) {
        html.setAttribute('data-reduce-motion', 'true');
      } else {
        html.removeAttribute('data-reduce-motion');
      }
      localStorage.setItem('reduce-motion', active ? 'true' : 'false');
      this.announce(active ? 'Reducción de movimiento activada' : 'Reducción de movimiento desactivada', 'polite');
    }
  }
}
