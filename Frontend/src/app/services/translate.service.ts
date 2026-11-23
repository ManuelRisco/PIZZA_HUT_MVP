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

  getCurrentLang(): string {
    return this.currentLang;
  }

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

      let container = document.getElementById('google_translate_element');
      if (!container) {
        container = document.createElement('div');
        container.id = 'google_translate_element';
        document.body.appendChild(container);
      }
      
      container.style.position = 'absolute';
      container.style.left = '-9999px';
      container.style.top = '-9999px';
      container.style.width = '1px';
      container.style.height = '1px';
      container.style.overflow = 'hidden';

      (window as any).googleTranslateElementInit = () => {
        try {
          new (window as any).google.translate.TranslateElement(
            {
              pageLanguage: 'es',
              includedLanguages: 'es,en,fr,pt,de,it,ja,ko,zh-CN',
              layout: (window as any).google.translate.TranslateElement.InlineLayout.SIMPLE,
              autoDisplay: true,
            },
            'google_translate_element'
          );
          this.scriptLoaded = true;
          this.scriptLoading = false;
          resolve();
        } catch (err) {
          this.scriptLoading = false;
          reject(err);
        }
      };

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

  changeLanguage(langCode: string): void {
    this.currentLang = langCode;
    localStorage.setItem('preferred-lang', langCode);

    if (langCode === 'es') {
      document.cookie = 'googtrans=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;';
      document.cookie = 'googtrans=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/; domain=.' + window.location.hostname;
    } else {
      const cookieString = `/es/${langCode}`;
      document.cookie = `googtrans=${cookieString}; path=/;`;
      document.cookie = `googtrans=${cookieString}; path=/; domain=.${window.location.hostname};`;
    }

    window.location.reload();
  }

  retranslate(): void {
    if (this.currentLang === 'es') return;

    document.querySelectorAll('script[src*="translate.google.com"]').forEach(s => s.remove());
    
    const container = document.getElementById('google_translate_element');
    if (container) {
      container.innerHTML = '';
    }

    document.querySelectorAll('iframe.goog-te-menu-frame, iframe.goog-te-banner-frame').forEach(f => f.remove());
    
    if ((window as any).google?.translate) {
      delete (window as any).google.translate;
    }
    
    this.scriptLoaded = false;
    this.scriptLoading = false;

    this.loadGoogleTranslate();
  }

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
