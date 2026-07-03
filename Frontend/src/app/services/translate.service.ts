import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class TranslateService {
  private currentLang: string = 'es';
  private scriptLoaded: boolean = false;
  private scriptLoading: boolean = false;

  constructor() {
    this.currentLang = localStorage.getItem('preferred-lang') || 'es';
  }

  /**
   * Obtiene el idioma actual
   */
  getCurrentLang(): string {
    return this.currentLang;
  }

  /**
   * Carga el script de Google Translate
   */
  loadGoogleTranslate(): Promise<void> {
    return new Promise((resolve, reject) => {
      if (this.scriptLoaded) {
        resolve();
        return;
      }

      if (this.scriptLoading) {
        const checkInterval = setInterval(() => {
          if (this.scriptLoaded) {
            clearInterval(checkInterval);
            resolve();
          }
        }, 100);
        return;
      }

      this.scriptLoading = true;

      // Crear el contenedor para Google Translate
      let container = document.getElementById('google_translate_element');
      if (!container) {
        container = document.createElement('div');
        container.id = 'google_translate_element';
        document.body.appendChild(container);
      }
      
      // Posicionar fuera de pantalla para que no ocupe espacio visual
      container.style.position = 'absolute';
      container.style.left = '-9999px';
      container.style.top = '-9999px';
      container.style.width = '1px';
      container.style.height = '1px';
      container.style.overflow = 'hidden';

      // Definir la callback global
      (window as any).googleTranslateElementInit = () => {
        try {
          new (window as any).google.translate.TranslateElement(
            {
              pageLanguage: 'es',
              includedLanguages: 'es,en,fr,pt,de,it,ja,ko,zh-CN',
              layout: (window as any).google.translate.TranslateElement.InlineLayout.SIMPLE,
              autoDisplay: true, // Debe ser true para que se genere el combo con opciones
            },
            'google_translate_element'
          );
          this.scriptLoaded = true;
          this.scriptLoading = false;
          resolve(); // Resolvemos inmediatamente
        } catch (err) {
          this.scriptLoading = false;
          reject(err);
        }
      };

      // Cargar el script
      const script = document.createElement('script');
      script.src = '//translate.google.com/translate_a/element.js?cb=googleTranslateElementInit';
      script.async = true;
      script.onerror = () => {
        this.scriptLoading = false;
        reject(new Error('Failed to load Google Translate script'));
      };
      document.head.appendChild(script);
    });
  }

  /**
   * Cambia el idioma de la página mediante Cookies + Reload (Método 100% confiable para GT)
   */
  changeLanguage(langCode: string): void {
    // 1. Guardar la preferencia en localStorage
    this.currentLang = langCode;
    localStorage.setItem('preferred-lang', langCode);

    // 2. Manipular la cookie nativa de Google Translate (googtrans)
    if (langCode === 'es') {
      // Limpiar cookie para volver al original
      document.cookie = 'googtrans=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;';
      document.cookie = 'googtrans=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/; domain=.' + window.location.hostname;
    } else {
      // Formato de Google Translate: /idiomaPagina/idiomaDestino
      const cookieString = `/es/${langCode}`;
      document.cookie = `googtrans=${cookieString}; path=/;`;
      document.cookie = `googtrans=${cookieString}; path=/; domain=.${window.location.hostname};`;
    }

    // 3. Recargar la página para que el script inicial de GT lea la cookie y traduzca automáticamente
    window.location.reload();
  }

  /**
   * Fuerza a Google Translate a retraducir el contenido dinámico (DOM actualizado)
   * Útil para cuando se carga data desde APIs (ej: lista de pizzas en el menú)
   */
  retranslate(): void {
    if (this.currentLang === 'es') return;

    // Hard reset de Google Translate para forzar escaneo del DOM (Pizzas dinámicas)
    // 1. Eliminar scripts antiguos de Google Translate
    document.querySelectorAll('script[src*="translate.google.com"]').forEach(s => s.remove());
    
    // 2. Limpiar el contenedor del widget
    const container = document.getElementById('google_translate_element');
    if (container) {
      container.innerHTML = '';
    }

    // 3. Limpiar iframes inyectados por Google Translate
    document.querySelectorAll('iframe.goog-te-menu-frame, iframe.goog-te-banner-frame').forEach(f => f.remove());
    
    // 4. Limpiar objetos globales
    if ((window as any).google?.translate) {
      delete (window as any).google.translate;
    }
    
    // 5. Resetear estado del servicio
    this.scriptLoaded = false;
    this.scriptLoading = false;

    // 6. Recargar el script (escaneará el DOM actual, incluyendo las pizzas recién cargadas)
    this.loadGoogleTranslate();
  }

  /**
   * Obtiene la lista de idiomas disponibles
   */
  getAvailableLanguages(): { code: string; name: string; flag: string }[] {
    return [
      { code: 'es', name: 'Español', flag: '🇪🇸' },
      { code: 'en', name: 'English', flag: '🇺🇸' },
      { code: 'fr', name: 'Français', flag: '🇫🇷' },
      { code: 'pt', name: 'Português', flag: '🇧🇷' },
      { code: 'de', name: 'Deutsch', flag: '🇩🇪' },
      { code: 'it', name: 'Italiano', flag: '🇮🇹' },
      { code: 'ja', name: '日本語', flag: '🇯🇵' },
      { code: 'ko', name: '한국어', flag: '🇰🇷' },
      { code: 'zh-CN', name: '中文', flag: '🇨🇳' },
    ];
  }
}
