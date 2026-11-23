import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class ImageCacheService {
  private imageCache= new Map<string, boolean>();
  private readonly preloadedImages= new Map<string, HTMLImageElement>();

  constructor() {
    this.loadCacheFromStorage();
  }

  markAsLoaded(imageUrl: string): void {
    this.imageCache.set(imageUrl, true);
    this.saveCacheToStorage();
  }

  isImageCached(imageUrl: string): boolean {
    return this.imageCache.has(imageUrl) && this.imageCache.get(imageUrl) === true;
  }

  preloadImage(imageUrl: string): Promise<void> {
    if (this.isImageCached(imageUrl)) {
      return Promise.resolve();
    }

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

  preloadImages(imageUrls: string[]): Promise<void[]> {
    const promises = imageUrls
      .filter(url => url?.trim() !== '')
      .map(url => this.preloadImage(url).catch(() => {}));
    
    return Promise.all(promises);
  }

  private saveCacheToStorage(): void {
    try {
      const cacheArray = Array.from(this.imageCache.entries());
      sessionStorage.setItem('image-cache', JSON.stringify(cacheArray));
    } catch (error) {
    }
  }

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

  clearCache(): void {
    this.imageCache.clear();
    this.preloadedImages.clear();
    sessionStorage.removeItem('image-cache');
  }

  getCacheSize(): number {
    return this.imageCache.size;
  }
}

