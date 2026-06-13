import { Injectable, signal } from '@angular/core';

export interface Toast {
  id: number;
  message: string;
  type: 'success' | 'error' | 'info';
}

@Injectable({
  providedIn: 'root'
})
export class ToastService {
  toasts = signal<Toast[]>([]);
  private counter = 0;

  showSuccess(message: string) {
    this.addToast(message, 'success');
  }

  showError(message: string) {
    this.addToast(message, 'error');
  }

  showInfo(message: string) {
    this.addToast(message, 'info');
  }

  private addToast(message: string, type: 'success' | 'error' | 'info') {
    const id = ++this.counter;
    this.toasts.update(current => [...current, { id, message, type }]);

    setTimeout(() => {
      this.removeToast(id);
    }, 4000);
  }

  removeToast(id: number) {
    this.toasts.update(current => current.filter(t => t.id !== id));
  }
}
