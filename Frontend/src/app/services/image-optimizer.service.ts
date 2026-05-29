import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class ImageOptimizerService {
  
  /**
   * Optimiza URLs de imágenes para reducir la calidad y tamaño
   * @param imageUrl URL original de la imagen
   * @param quality Calidad deseada: 'low' | 'medium' | 'high'
   * @returns URL optimizada
   */
  optimizeImageUrl(imageUrl: string | undefined, quality: 'low' | 'medium' | 'high' = 'medium'): string {
    if (!imageUrl) {
      return '/combo1.webp';
    }

    // Configuraciones de calidad para diferentes proveedores
    const qualitySettings = {
      low: { 
        unsplashWidth: 400, 
        unsplashQuality: 60,
        googleSize: 'w400-h400', 
        googleQuality: 'c' 
      },
      medium: { 
        unsplashWidth: 600, 
        unsplashQuality: 75,
        googleSize: 'w600-h600', 
        googleQuality: 'c' 
      },
      high: { 
        unsplashWidth: 800, 
        unsplashQuality: 85,
        googleSize: 'w800-h800', 
        googleQuality: 'c' 
      }
    };

    const settings = qualitySettings[quality];

    try {
      // Optimización para Unsplash (PRIORIDAD)
      if (imageUrl.includes('unsplash.com')) {
        return this.optimizeUnsplashUrl(imageUrl, settings);
      }

      // Optimización para Google Drive
      if (imageUrl.includes('drive.google.com')) {
        return this.optimizeGoogleDriveUrl(imageUrl, settings);
      }
      
      // Optimización para Google Photos
      if (imageUrl.includes('googleusercontent.com') || imageUrl.includes('ggpht.com')) {
        return this.optimizeGooglePhotosUrl(imageUrl, settings);
      }

      // Optimización para URLs genéricas de Google
      if (imageUrl.includes('google') || imageUrl.includes('gstatic')) {
        return this.optimizeGenericGoogleUrl(imageUrl, settings);
      }

      // Si no es reconocida, devolver la URL original
      return imageUrl;
    } catch (error) {
      console.warn('Error optimizando imagen:', error);
      return imageUrl;
    }
  }

  /**
   * Optimiza URLs de Unsplash con sus parámetros avanzados
   * Unsplash soporta: w (width), h (height), q (quality), fm (format), fit (crop), auto
   */
  private optimizeUnsplashUrl(url: string, settings: any): string {
    // Limpiar parámetros existentes
    const baseUrl = url.split('?')[0];
    
    // Parámetros optimizados para Unsplash
    const params = new URLSearchParams({
      'w': settings.unsplashWidth.toString(),        // Ancho
      'h': settings.unsplashWidth.toString(),        // Alto (cuadrado)
      'q': settings.unsplashQuality.toString(),      // Calidad (0-100)
      'fm': 'webp',                                   // Formato WebP (más ligero)
      'fit': 'crop',                                  // Recortar para ajustar
      'auto': 'compress,format'                       // Compresión automática
    });
    
    return `${baseUrl}?${params.toString()}`;
  }

  /**
   * Optimiza URLs de Google Drive
   */
  private optimizeGoogleDriveUrl(url: string, settings: any): string {
    // Extraer el ID del archivo
    const fileIdMatch = url.match(/\/d\/([a-zA-Z0-9_-]+)/);
    if (fileIdMatch) {
      const fileId = fileIdMatch[1];
      // Formato: https://drive.google.com/thumbnail?id=FILE_ID&sz=w400-h400
      return `https://drive.google.com/thumbnail?id=${fileId}&sz=${settings.googleSize}`;
    }
    return url;
  }

  /**
   * Optimiza URLs de Google Photos/usercontent
   */
  private optimizeGooglePhotosUrl(url: string, settings: any): string {
    // Si ya tiene parámetros de tamaño, reemplazarlos
    let optimizedUrl = url.split('=')[0];
    
    // Agregar parámetros de optimización
    // Formato: =w400-h400-c (width, height, crop)
    optimizedUrl += `=${settings.googleSize}-${settings.googleQuality}`;
    
    return optimizedUrl;
  }

  /**
   * Optimiza URLs genéricas de Google
   */
  private optimizeGenericGoogleUrl(url: string, settings: any): string {
    // Intentar agregar parámetros de tamaño si la URL lo soporta
    const separator = url.includes('?') ? '&' : '?';
    return `${url}${separator}sz=${settings.googleSize}`;
  }

  /**
   * Pre-carga una imagen optimizada
   * @param imageUrl URL de la imagen
   * @param quality Calidad deseada
   * @returns Promise que se resuelve cuando la imagen está cargada
   */
  preloadOptimizedImage(imageUrl: string, quality: 'low' | 'medium' | 'high' = 'low'): Promise<void> {
    return new Promise((resolve, reject) => {
      const optimizedUrl = this.optimizeImageUrl(imageUrl, quality);
      const img = new Image();
      
      img.onload = () => resolve();
      img.onerror = () => reject(new Error(`Failed to load: ${optimizedUrl}`));
      
      img.src = optimizedUrl;
    });
  }

  /**
   * Carga progresiva: primero baja calidad, luego alta calidad
   * @param imageUrl URL de la imagen
   * @returns Objeto con URLs de baja y alta calidad
   */
  getProgressiveUrls(imageUrl: string): { lowQuality: string; highQuality: string } {
    return {
      lowQuality: this.optimizeImageUrl(imageUrl, 'low'),
      highQuality: this.optimizeImageUrl(imageUrl, 'high')
    };
  }
}
