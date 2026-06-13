import { Component, signal, OnInit, OnDestroy, HostListener, Inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { RouterOutlet } from '@angular/router';
import { NavbarComponent } from './componentes/navbar/navbar';
import { FooterComponent } from "./componentes/footer/footer";
import { ToastComponent } from './componentes/toast/toast.component';

@Component({    
  selector: 'app-root',
  standalone: true,
  imports: [
    NavbarComponent,
    FooterComponent,
    ToastComponent,
    RouterOutlet,
],
  templateUrl: './app.html',
  styleUrls: ['./app.css']
})
export class App implements OnInit, OnDestroy { 
  protected readonly title = signal('pizzahut');
  private observer: MutationObserver | null = null;
  private lastFocusedElement: HTMLElement | null = null;

  constructor(@Inject(PLATFORM_ID) private platformId: Object) {}

  ngOnInit() {
    if (isPlatformBrowser(this.platformId)) {
      this.setupModalObserver();
    }
  }

  ngOnDestroy() {
    if (this.observer) {
      this.observer.disconnect();
    }
  }

  private setupModalObserver() {
    this.observer = new MutationObserver((mutations) => {
      for (const mutation of mutations) {
        // Detect modals being added to the DOM
        for (let i = 0; i < mutation.addedNodes.length; i++) {
          const node = mutation.addedNodes[i];
          if (node.nodeType === 1) { // Element node
            const element = node as HTMLElement;
            if (element.classList?.contains('modal') && element.classList?.contains('show')) {
              this.onModalOpened(element);
            } else if (element.querySelector) {
              const modal = element.querySelector('.modal.show');
              if (modal) this.onModalOpened(modal as HTMLElement);
            }
          }
        }
        
        // Detect modals being removed
        for (let i = 0; i < mutation.removedNodes.length; i++) {
          const node = mutation.removedNodes[i];
          if (node.nodeType === 1) {
            const element = node as HTMLElement;
            if (element.classList?.contains('modal') && element.classList?.contains('show')) {
              this.onModalClosed();
            } else if (element.querySelector) {
              const modal = element.querySelector('.modal.show');
              if (modal) this.onModalClosed();
            }
          }
        }
      }
    });

    this.observer.observe(document.body, { childList: true, subtree: true });
  }

  private onModalOpened(modal: HTMLElement) {
    if (!this.lastFocusedElement) {
      this.lastFocusedElement = document.activeElement as HTMLElement;
    }
    // Auto focus the modal wrapper, not the inner elements, to let Screen Readers announce title only
    setTimeout(() => {
      modal.focus();
    }, 100);
  }

  private onModalClosed() {
    if (this.lastFocusedElement) {
      setTimeout(() => {
        this.lastFocusedElement?.focus();
        this.lastFocusedElement = null;
      }, 100);
    }
  }

  @HostListener('document:keydown', ['$event'])
  manejarTecladoModalGlobal(event: KeyboardEvent): void {
    if (!isPlatformBrowser(this.platformId)) return;

    const modales = document.querySelectorAll('.modal.show');
    if (modales.length === 0) return;

    const modalActivo = modales[modales.length - 1] as HTMLElement;

    if (event.key === 'Escape') {
      // Simulate close by clicking the X button or cancel button
      const closeBtn = modalActivo.querySelector('.btn-close, [data-bs-dismiss="modal"]') as HTMLElement | null;
      if (closeBtn) {
        closeBtn.click();
      } else {
        const cancelBtn = modalActivo.querySelector('.btn-secondary') as HTMLElement | null;
        if (cancelBtn) cancelBtn.click();
      }
      return;
    }

    if (event.key === 'Tab') {
      const focusableSelectors = 'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])';
      const focusableElements = Array.from(modalActivo.querySelectorAll(focusableSelectors)) as HTMLElement[];
      
      if (focusableElements.length === 0) return;

      const firstElement = focusableElements[0];
      const lastElement = focusableElements[focusableElements.length - 1];

      if (!modalActivo.contains(document.activeElement)) {
        firstElement.focus();
        event.preventDefault();
        return;
      }

      if (event.shiftKey) { // Shift + Tab
        if (document.activeElement === firstElement) {
          lastElement.focus();
          event.preventDefault();
        }
      } else { // Tab
        if (document.activeElement === lastElement) {
          firstElement.focus();
          event.preventDefault();
        }
      }
    }
  }
}