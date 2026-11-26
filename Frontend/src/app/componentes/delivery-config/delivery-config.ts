import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';
import { finalize } from 'rxjs/operators';
import { StoreSettingsService } from '../../services/store-settings.service';
import { CartService } from '../../services/cart.service';
import { ToastService } from '../../services/toast.service';

@Component({
  selector: 'app-delivery-config',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './delivery-config.html',
})
export class DeliveryConfigComponent implements OnInit, OnDestroy {
  deliveryFeeActual: number = 5.0;
  deliveryFeeInput: number = 5.0;
  guardandoDeliveryFee: boolean = false;
  deliveryFeeActualizadoAt: string | null = null;
  deliveryFeeMensaje: string = '';
  deliveryFeePresets: number[] = [0, 3, 5, 7, 10];

  private deliveryFeeSub?: Subscription;

  constructor(
    private readonly storeSettingsService: StoreSettingsService,
    private readonly cartService: CartService,
    private readonly toastService: ToastService,
  ) {}

  ngOnInit(): void {
    this.deliveryFeeSub = this.storeSettingsService.deliveryFee$.subscribe((fee) => {
      this.deliveryFeeActual = fee;
      this.deliveryFeeInput = fee;
      this.cartService.setDeliveryFeeRate(fee);
    });
    this.cargarTarifaDelivery();
  }

  ngOnDestroy(): void {
    this.deliveryFeeSub?.unsubscribe();
  }

  cargarTarifaDelivery(): void {
    this.storeSettingsService.loadDeliveryFee().subscribe((cfg) => {
      const fee = Number(cfg?.deliveryFee);
      if (!Number.isNaN(fee) && fee >= 0) {
        this.deliveryFeeActual = fee;
        this.deliveryFeeInput = fee;
        this.deliveryFeeActualizadoAt = cfg?.updatedAt ?? null;
        this.cartService.setDeliveryFeeRate(fee);
      }
    });
  }

  seleccionarTarifaRapida(monto: number): void {
    this.deliveryFeeInput = monto;
  }

  guardarTarifaDelivery(): void {
    const valor = Number(this.deliveryFeeInput);
    if (Number.isNaN(valor) || valor < 0 || valor > 500) {
      this.toastService.showError('Ingresa un monto válido entre S/ 0.00 y S/ 500.00');
      return;
    }

    const normalizado = Number(valor.toFixed(2));
    this.guardandoDeliveryFee = true;
    this.deliveryFeeMensaje = '';
    this.storeSettingsService
      .updateDeliveryFee(normalizado)
      .pipe(
        finalize(() => {
          this.guardandoDeliveryFee = false;
        }),
      )
      .subscribe({
        next: (res) => {
          const guardado = Number(res?.deliveryFee ?? normalizado);
          this.deliveryFeeActual = guardado;
          this.deliveryFeeInput = guardado;
          this.deliveryFeeActualizadoAt = res?.updatedAt ?? new Date().toISOString();
          this.cartService.setDeliveryFeeRate(guardado);
          const msg =
            guardado === 0
              ? 'Delivery configurado como gratis (S/ 0.00)'
              : `Tarifa de delivery actualizada a S/ ${guardado.toFixed(2)}`;
          this.deliveryFeeMensaje = msg;
          this.toastService.showSuccess(msg);
        },
        error: (err) => {
          this.toastService.showError(err?.error?.message || 'No se pudo actualizar la tarifa de delivery');
        },
      });
  }
}
