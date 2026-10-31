import { Injectable } from '@angular/core';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class ImageOptimizerService {
  private readonly backendBaseUrl = environment.apiUrl.replace(/\/api\/?$/, '');

  /**
   * Resuelve rutas de archivos locales servidos por el backend (/uploads/**)
   * hacia la URL real del servidor Spring Boot.
   */
  resolveImageUrl(imageUrl?: string | null, useThumb = false): string {
    if (!imageUrl || !imageUrl.trim()) {
      return '/combo1.webp';
    }

    const trimmed = imageUrl.trim();
    if (trimmed.startsWith('data:') || trimmed.startsWith('blob:')) {
      return trimmed;
    }

    if (trimmed.startsWith('/uploads/') || trimmed.startsWith('uploads/')) {
      let normalized = trimmed.startsWith('/') ? trimmed : `/${trimmed}`;
      if (useThumb && !normalized.includes('/thumbs/')) {
        const lastSlash = normalized.lastIndexOf('/');
        if (lastSlash > 0) {
          normalized = `${normalized.substring(0, lastSlash)}/thumbs${normalized.substring(lastSlash)}`;
        }
      }
      return `${this.backendBaseUrl}${normalized}`;
    }

    return trimmed;
  }

  /**
   * Optimiza y resuelve URLs de imágenes (priorizando archivos locales del backend /uploads/**)
   * @param imageUrl URL o ruta relativa de la imagen
   * @param quality Calidad deseada: 'low' | 'medium' | 'high'
   * @returns URL lista para el navegador
   */
  optimizeImageUrl(imageUrl: string | undefined, quality: 'low' | 'medium' | 'high' = 'medium'): string {
    if (!imageUrl || !imageUrl.trim()) {
      return '/combo1.webp';
    }

    if (imageUrl.startsWith('/uploads/') || imageUrl.startsWith('uploads/')) {
      return this.resolveImageUrl(imageUrl, quality === 'low');
    }

    if (imageUrl.startsWith('data:') || imageUrl.startsWith('blob:')) {
      return imageUrl;
    }

    // Configuraciones de calidad para proveedores legados si existieran
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
      if (imageUrl.includes('unsplash.com')) {
        return this.optimizeUnsplashUrl(imageUrl, settings);
      }
      if (imageUrl.includes('drive.google.com')) {
        return this.optimizeGoogleDriveUrl(imageUrl, settings);
      }
      if (imageUrl.includes('googleusercontent.com') || imageUrl.includes('ggpht.com')) {
        return this.optimizeGooglePhotosUrl(imageUrl, settings);
      }
      if (imageUrl.includes('google') || imageUrl.includes('gstatic')) {
        return this.optimizeGenericGoogleUrl(imageUrl, settings);
      }
      return imageUrl;
    } catch {
      return imageUrl;
    }
  }

  private optimizeUnsplashUrl(url: string, settings: any): string {
    const baseUrl = url.split('?')[0];
    const params = new URLSearchParams({
      w: settings.unsplashWidth.toString(),
      h: settings.unsplashWidth.toString(),
      q: settings.unsplashQuality.toString(),
      fm: 'webp',
      fit: 'crop',
      auto: 'compress,format'
    });
    return `${baseUrl}?${params.toString()}`;
  }

  private optimizeGoogleDriveUrl(url: string, settings: any): string {
    const fileIdMatch = url.match(/\/d\/([a-zA-Z0-9_-]+)/);
    if (fileIdMatch) {
      const fileId = fileIdMatch[1];
      return `https://drive.google.com/thumbnail?id=${fileId}&sz=${settings.googleSize}`;
    }
    return url;
  }

  private optimizeGooglePhotosUrl(url: string, settings: any): string {
    let optimizedUrl = url.split('=')[0];
    optimizedUrl += `=${settings.googleSize}-${settings.googleQuality}`;
    return optimizedUrl;
  }

  private optimizeGenericGoogleUrl(url: string, settings: any): string {
    const separator = url.includes('?') ? '&' : '?';
    return `${url}${separator}sz=${settings.googleSize}`;
  }

  preloadOptimizedImage(imageUrl: string, quality: 'low' | 'medium' | 'high' = 'low'): Promise<void> {
    return new Promise((resolve, reject) => {
      const optimizedUrl = this.optimizeImageUrl(imageUrl, quality);
      const img = new Image();
      img.onload = () => resolve();
      img.onerror = () => reject(new Error(`Failed to load: ${optimizedUrl}`));
      img.src = optimizedUrl;
    });
  }

  getProgressiveUrls(imageUrl: string): { lowQuality: string; highQuality: string } {
    return {
      lowQuality: this.optimizeImageUrl(imageUrl, 'low'),
      highQuality: this.optimizeImageUrl(imageUrl, 'high')
    };
  }
}
