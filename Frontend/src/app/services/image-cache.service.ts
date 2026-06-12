import { Injectable } from '@angular/core';

/**
 * Servicio para cachear imágenes y evitar recargas innecesarias
 */
@Injectable({
  providedIn: 'root'
})
export class ImageCacheService {
  private imageCache = new Map<string, boolean>();
  private preloadedImages = new Map<string, HTMLImageElement>();

  constructor() {
    // Recuperar estado del caché de sessionStorage al iniciar
    this.loadCacheFromStorage();
  }

  /**
   * Marca una imagen como cargada en el caché
   */
  markAsLoaded(imageUrl: string): void {
    this.imageCache.set(imageUrl, true);
    this.saveCacheToStorage();
  }

  /**
   * Verifica si una imagen ya está en caché
   */
  isImageCached(imageUrl: string): boolean {
    return this.imageCache.has(imageUrl) && this.imageCache.get(imageUrl) === true;
  }

  /**
   * Pre-carga una imagen en segundo plano
   */
  preloadImage(imageUrl: string): Promise<void> {
    // Si ya está cargada, resolver inmediatamente
    if (this.isImageCached(imageUrl)) {
      return Promise.resolve();
    }

    // Si ya está en proceso de pre-carga
    if (this.preloadedImages.has(imageUrl)) {
      return Promise.resolve();
    }

    return new Promise((resolve, reject) => {
      const img = new Image();
      
      img.onload = () => {
        this.preloadedImages.set(imageUrl, img);
        this.markAsLoaded(imageUrl);
        resolve();
      };

      img.onerror = () => {
        reject(new Error(`Failed to load image: ${imageUrl}`));
      };

      img.src = imageUrl;
    });
  }

  /**
   * Pre-carga múltiples imágenes en paralelo
   */
  preloadImages(imageUrls: string[]): Promise<void[]> {
    const promises = imageUrls
      .filter(url => url && url.trim() !== '')
      .map(url => this.preloadImage(url).catch(() => {})); // Ignorar errores individuales
    
    return Promise.all(promises);
  }

  /**
   * Guarda el estado del caché en sessionStorage
   * Se mantiene durante la sesión del navegador
   */
  private saveCacheToStorage(): void {
    try {
      const cacheArray = Array.from(this.imageCache.entries());
      sessionStorage.setItem('image-cache', JSON.stringify(cacheArray));
    } catch (error) {
    }
  }

  /**
   * Carga el estado del caché desde sessionStorage
   */
  private loadCacheFromStorage(): void {
    try {
      const stored = sessionStorage.getItem('image-cache');
      if (stored) {
        const cacheArray = JSON.parse(stored);
        this.imageCache = new Map(cacheArray);
      }
    } catch (error) {
    }
  }

  /**
   * Limpia el caché de imágenes
   */
  clearCache(): void {
    this.imageCache.clear();
    this.preloadedImages.clear();
    sessionStorage.removeItem('image-cache');
  }

  /**
   * Obtiene el tamaño del caché
   */
  getCacheSize(): number {
    return this.imageCache.size;
  }
}

