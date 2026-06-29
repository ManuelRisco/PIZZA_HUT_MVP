import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { IdleTimeoutService } from '../../services/idle-timeout.service';

@Component({
  selector: 'app-afk-modal',
  standalone: true,
  imports: [CommonModule],
  template: `
    @if (idleService.showAfkModal()) {
      <div class="modal fade show d-block" tabindex="-1" role="dialog" aria-labelledby="afkModalLabel" aria-modal="true" style="background-color: rgba(0,0,0,0.5);">
        <div class="modal-dialog modal-dialog-centered" role="document">
          <div class="modal-content text-center p-4" style="border-radius: 15px; border: none; box-shadow: 0 10px 30px rgba(0,0,0,0.2);">
            <div class="modal-header border-0 justify-content-center pb-0">
              <div class="warning-icon-container mb-2">
                <i class="bi bi-clock-history text-warning" style="font-size: 3rem;"></i>
              </div>
            </div>
            <div class="modal-body border-0 pt-2">
              <h4 class="modal-title mb-3" id="afkModalLabel" style="font-weight: 700; color: #2c3e50;">¿Sigues ahí?</h4>
              <p class="text-muted" style="font-size: 1.1rem;">
                Tu sesión expirará pronto por inactividad. ¿Deseas mantener tu sesión activa?
              </p>
            </div>
            <div class="modal-footer border-0 justify-content-center gap-3">
              <button type="button" class="btn btn-outline-danger px-4 py-2" (click)="salir()" style="border-radius: 8px; font-weight: 600;">
                Salir
              </button>
              <button type="button" class="btn btn-warning px-4 py-2 text-dark" (click)="quedarme()" style="border-radius: 8px; font-weight: 600;">
                Sí, sigo aquí
              </button>
            </div>
          </div>
        </div>
      </div>
    }
  `,
  styles: [`
    .modal.fade.show {
      animation: fadeIn 0.3s ease-out;
    }
    .modal-content {
      animation: slideDown 0.3s ease-out;
    }
    @keyframes fadeIn {
      from { opacity: 0; }
      to { opacity: 1; }
    }
    @keyframes slideDown {
      from { transform: translateY(-20px); opacity: 0; }
      to { transform: translateY(0); opacity: 1; }
    }
  `]
})
export class AfkModalComponent {
  idleService = inject(IdleTimeoutService);

  quedarme() {
    this.idleService.stayActive();
  }

  salir() {
    this.idleService.forceLogout();
  }
}
