import { Component, signal, OnInit, OnDestroy, HostListener, Inject, PLATFORM_ID, inject } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { RouterOutlet } from '@angular/router';
import { NavbarComponent } from './componentes/navbar/navbar';
import { FooterComponent } from "./componentes/footer/footer";
import { ToastComponent } from './componentes/toast/toast.component';
import { IdleTimeoutService } from './services/idle-timeout.service';
import { AfkModalComponent } from './componentes/afk-modal/afk-modal.component';
import { AccessibilityFabComponent } from './componentes/accessibility-fab/accessibility-fab';
import { AccessibilityService } from './services/accessibility.service';
import { AsyncPipe } from '@angular/common';

@Component({    
  selector: 'app-root',
  standalone: true,
  imports: [
    NavbarComponent,
    FooterComponent,
    ToastComponent,
    AfkModalComponent,
    AccessibilityFabComponent,
    RouterOutlet,
    AsyncPipe
],
  templateUrl: './app.html',
  styleUrls: ['./app.css']
})
export class App implements OnInit, OnDestroy { 
  protected readonly title = signal('pizzahut');
  private observer: MutationObserver | null = null;
  private lastFocusedElement: HTMLElement | null = null;
  private readonly idleTimeoutService = inject(IdleTimeoutService);
  public readonly accessibilityService = inject(AccessibilityService);

  constructor(@Inject(PLATFORM_ID) private readonly platformId: Object) {}

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
        this.processAddedNodes(mutation.addedNodes);
        this.processRemovedNodes(mutation.removedNodes);
      }
    });

    this.observer.observe(document.body, { childList: true, subtree: true });
  }

  private processAddedNodes(nodes: NodeList) {
    for (const node of Array.from(nodes)) {
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
  }

  private processRemovedNodes(nodes: NodeList) {
    for (const node of Array.from(nodes)) {
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

  private onModalOpened(modal: HTMLElement) {
    if (!this.lastFocusedElement) {
      this.lastFocusedElement = document.activeElement as HTMLElement;
    }
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
      this.closeModal(modalActivo);
      return;
    }

    if (event.key === 'Tab') {
      this.handleTabKey(event, modalActivo);
    }
  }

  private closeModal(modalActivo: HTMLElement) {
    const closeBtn = modalActivo.querySelector('.btn-close, [data-bs-dismiss="modal"]') as HTMLElement | null;
    if (closeBtn) {
      closeBtn.click();
    } else {
      const cancelBtn = modalActivo.querySelector('.btn-secondary') as HTMLElement | null;
      if (cancelBtn) cancelBtn.click();
    }
  }

  private handleTabKey(event: KeyboardEvent, modalActivo: HTMLElement) {
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
    } else if (document.activeElement === lastElement) { // Tab
      firstElement.focus();
      event.preventDefault();
    }
  }
}
