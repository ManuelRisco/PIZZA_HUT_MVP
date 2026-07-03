import { Injectable, inject, NgZone, signal } from '@angular/core';
import { AuthService } from './auth.service';
import { ToastService } from './toast.service';
import { Router } from '@angular/router';

@Injectable({
  providedIn: 'root',
})
export class IdleTimeoutService {
  private readonly authService = inject(AuthService);
  private readonly toastService = inject(ToastService);
  private readonly router = inject(Router);
  private readonly ngZone = inject(NgZone);

  private readonly timeoutSeconds= 300; // 5 minutos (300 segundos) para cierre total
  private readonly warningSeconds= 180; // 3 minutos (180 segundos) para mostrar el modal

  private idleTimer: any;
  private warningTimer: any;
  
  // Señal reactiva para mostrar u ocultar el modal AFK
  public showAfkModal = signal(false);
  
  private isTracking = false;

  private readonly userActivityEvents= [
    'mousemove',
    'keydown',
    'wheel',
    'DOMMouseScroll',
    'mouseWheel',
    'mousedown',
    'touchstart',
    'touchmove',
    'scroll',
  ];

  private readonly resetTimerBound= this.resetTimer.bind(this);

  constructor() {
    this.authService.currentUser$.subscribe((user) => {
      if (user) {
        this.startTracking();
      } else {
        this.stopTracking();
      }
    });
  }

  private startTracking(): void {
    if (this.isTracking) return;
    this.isTracking = true;
    this.showAfkModal.set(false);
    this.attachListeners();
    this.resetTimer();
  }

  private stopTracking(): void {
    if (!this.isTracking) return;
    this.isTracking = false;

    this.clearTimers();
    this.showAfkModal.set(false);
    this.removeListeners();
  }

  private attachListeners(): void {
    this.ngZone.runOutsideAngular(() => {
      this.userActivityEvents.forEach((eventName) => {
        window.addEventListener(eventName, this.resetTimerBound, true);
      });
    });
  }

  private removeListeners(): void {
    this.userActivityEvents.forEach((eventName) => {
      window.removeEventListener(eventName, this.resetTimerBound, true);
    });
  }

  private clearTimers(): void {
    if (this.idleTimer) clearTimeout(this.idleTimer);
    if (this.warningTimer) clearTimeout(this.warningTimer);
  }

  private resetTimer(): void {
    if (!this.isTracking) return;
    
    // Si el modal está visible, ignorar eventos de usuario hasta que decida
    if (this.showAfkModal()) return;

    this.clearTimers();

    this.ngZone.runOutsideAngular(() => {
      // Timer para la advertencia (Minuto 3)
      this.warningTimer = setTimeout(() => {
        this.ngZone.run(() => {
          this.showWarning();
        });
      }, this.warningSeconds * 1000);

      // Timer para el cierre de sesión (Minuto 5)
      this.idleTimer = setTimeout(() => {
        this.ngZone.run(() => {
          this.forceLogout();
        });
      }, this.timeoutSeconds * 1000);
    });
  }

  private showWarning(): void {
    // Al mostrar la advertencia, removemos listeners temporalmente 
    // para que no se reinicie si mueve el mouse dentro del modal
    this.removeListeners();
    this.showAfkModal.set(true);
  }

  public stayActive(): void {
    this.showAfkModal.set(false);
    this.toastService.showSuccess('Sesión mantenida activa.');
    this.attachListeners();
    this.resetTimer();
  }

  public forceLogout(): void {
    this.stopTracking();
    this.toastService.showError('Sesión expirada por inactividad.');
    this.authService.logout();
  }
}
