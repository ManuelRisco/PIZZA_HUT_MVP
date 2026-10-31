import { Component, signal, OnInit, OnDestroy, Inject, PLATFORM_ID, inject, NgZone } from '@angular/core';
import { isPlatformBrowser, AsyncPipe, Location } from '@angular/common';
import { Router, RouterOutlet, NavigationEnd } from '@angular/router';
import { Subscription, filter } from 'rxjs';
import { NavbarComponent } from './componentes/navbar/navbar';
import { FooterComponent } from "./componentes/footer/footer";
import { ToastComponent } from './componentes/toast/toast.component';
import { IdleTimeoutService } from './services/idle-timeout.service';
import { AfkModalComponent } from './componentes/afk-modal/afk-modal.component';
import { AccessibilityFabComponent } from './componentes/accessibility-fab/accessibility-fab';
import { AccessibilityService } from './services/accessibility.service';

const PANEL_EXACT_PATHS = new Set<string>([
  '/panel-admin',
  '/usuarios',
  '/pizzas',
  '/categorias',
  '/ingredientes',
  '/sizes',
  '/orders',
  '/payments',
  '/metodos-pago',
  '/reviews',
  '/cliente/dashboard',
  '/cliente/perfil',
  '/cliente/pedidos',
  '/cliente/resenas',
  '/cliente/favoritos'
]);

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
  protected readonly isPanelView = signal(false);

  private observer: MutationObserver | null = null;
  private lastFocusedElement: HTMLElement | null = null;
  private routerSub?: Subscription;
  private removeLocationListener?: VoidFunction;

  private readonly idleTimeoutService = inject(IdleTimeoutService);
  private readonly ngZone = inject(NgZone);
  private readonly router = inject(Router);
  private readonly location = inject(Location);
  public readonly accessibilityService = inject(AccessibilityService);

  private readonly boundKeydownHandler = (event: KeyboardEvent) => {
    if (event.key !== 'Escape' && event.key !== 'Tab') return;
    this.manejarTecladoModalGlobal(event);
  };

  constructor(@Inject(PLATFORM_ID) private readonly platformId: Object) {}

  ngOnInit() {
    this.updatePanelViewFlag(this.location.path() || this.router.url);

    this.routerSub = this.router.events
      .pipe(filter((e): e is NavigationEnd => e instanceof NavigationEnd))
      .subscribe((e) => {
        this.updatePanelViewFlag(e.urlAfterRedirects || e.url);
      });

    this.removeLocationListener = this.location.onUrlChange((url) => {
      this.updatePanelViewFlag(url);
    });

    if (isPlatformBrowser(this.platformId)) {
      this.ngZone.runOutsideAngular(() => {
        this.setupModalObserver();
        document.addEventListener('keydown', this.boundKeydownHandler);
      });
    }
  }

  ngOnDestroy() {
    this.routerSub?.unsubscribe();
    if (this.removeLocationListener) {
      this.removeLocationListener();
    }
    if (this.observer) {
      this.observer.disconnect();
    }
    if (isPlatformBrowser(this.platformId)) {
      document.removeEventListener('keydown', this.boundKeydownHandler);
    }
  }

  private updatePanelViewFlag(rawUrl: string): void {
    const cleanPath = (rawUrl || '').split('?')[0].split('#')[0] || '/';
    const normalized = cleanPath.startsWith('/') ? cleanPath : `/${cleanPath}`;
    const isPanel = normalized.startsWith('/admin/') || PANEL_EXACT_PATHS.has(normalized);
    if (this.isPanelView() !== isPanel) {
      this.isPanelView.set(isPanel);
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
      if (node.nodeType === 1) {
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

  manejarTecladoModalGlobal(event: KeyboardEvent): void {
    if (!isPlatformBrowser(this.platformId)) return;

    const modales = document.querySelectorAll('.modal.show');
    if (modales.length === 0) return;

    const modalActivo = modales[modales.length - 1] as HTMLElement;

    if (event.key === 'Escape') {
      this.ngZone.run(() => this.closeModal(modalActivo));
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

    if (event.shiftKey) {
      if (document.activeElement === firstElement) {
        lastElement.focus();
        event.preventDefault();
      }
    } else if (document.activeElement === lastElement) {
      firstElement.focus();
      event.preventDefault();
    }
  }
}
