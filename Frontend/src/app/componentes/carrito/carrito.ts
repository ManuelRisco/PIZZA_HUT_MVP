import { Component, NgZone, OnDestroy, OnInit, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { CartService } from '../../services/cart.service';
import { CartItem, CartTotals } from '../../models/cart-item.model';
import { CheckoutService } from '../../services/checkout.service';
import { CheckoutPayload } from '../../models/checkout.model';
import { PaymentMethodService } from '../../services/payment-method.service';
import { PaymentMethodDTO } from '../../models/payment-method.interface';
import { IngredientService } from '../../services/ingredient.service';
import { IngredientDTO } from '../../models/admin.interface';
import { AddressService, AddressDTO } from '../../services/address.service';
import { AuthService } from '../../services/auth.service';
import { ImageOptimizerService } from '../../services/image-optimizer.service';
import { PromocionService } from '../../services/promocion.service';
import { AccessibilityService } from '../../services/accessibility.service';
import { ToastService } from '../../services/toast.service';
import { Payment } from '../../services/payment.service';
import { SunatService } from '../../services/sunat.service';
import { UsuarioService } from '../../services/usuario.service';
import { StoreSettingsService } from '../../services/store-settings.service';

declare global {
  interface Window {
    MercadoPago?: any;
  }
}

@Component({
  selector: 'app-carrito',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './carrito.html',
  styleUrl: './carrito.css',
})
export class CarritoComponent implements OnInit, OnDestroy, OnChanges {
  onSavedAddressChange(): void {
    if (this.addressOption === 'saved' && this.selectedAddressId) {
      const dir = this.direcciones.find((d) => d.id === this.selectedAddressId);
      if (dir) {
        this.addressForm.line1 = dir.line1;
        this.addressForm.city = dir.city;
        this.addressForm.district = dir.district;
        this.addressForm.reference =
          typeof dir.reference === 'string' && dir.reference.trim().length > 0 ? dir.reference : '';
      } else {
        this.addressForm.reference = '';
      }
    }
  }
  addressOption: 'saved' | 'new' = 'saved';
  showNewAddressForm = false;
  newAddressForm = {
    line1: '',
    city: '',
    district: '',
    reference: '',
  };
  newAddressError = '';

  toggleNewAddressForm(): void {
    this.showNewAddressForm = !this.showNewAddressForm;
    this.newAddressError = '';
    this.newAddressForm = { line1: '', city: '', district: '', reference: '' };
    if (this.showNewAddressForm) {
      this.selectedAddressId = null;
      this.addressForm = { line1: '', city: '', district: '', reference: '' };
    }
  }

  saveNewAddress(): void {
    const userId = this.authService.obtenerUsuarioId();
    if (!userId) {
      this.newAddressError = 'No se pudo identificar el usuario.';
      return;
    }
    if (
      !this.newAddressForm.line1.trim() ||
      !this.newAddressForm.city.trim() ||
      !this.newAddressForm.district.trim()
    ) {
      this.newAddressError = 'Completa todos los campos obligatorios.';
      return;
    }
    const newAddress: AddressDTO = {
      userId,
      line1: this.newAddressForm.line1.trim(),
      city: this.newAddressForm.city.trim(),
      district: this.newAddressForm.district.trim(),
      reference: this.newAddressForm.reference.trim(),
    };
    this.addressService.crear(newAddress).subscribe({
      next: (address) => {
        const idx = this.direcciones.findIndex((d) => d.id === address.id);
        if (idx === -1) {
          this.direcciones.push(address);
        } else {
          this.direcciones[idx] = address;
        }
        this.selectedAddressId = address.id || null;
        this.actualizarAddressForm();
        this.showNewAddressForm = false;
        this.newAddressError = '';
      },
      error: () => {
        this.newAddressError = 'No se pudo guardar la dirección.';
      },
    });
  }
  ngOnChanges(changes: SimpleChanges): void {
    this.actualizarAddressForm();
  }

  onAddressSelectChange(): void {
    this.actualizarAddressForm();
  }

  private actualizarAddressForm(): void {
    if (this.selectedAddressId && this.direcciones.length > 0) {
      const dir = this.direcciones.find((d) => d.id === this.selectedAddressId);
      if (dir) {
        this.addressForm.line1 = dir.line1;
        this.addressForm.city = dir.city;
        this.addressForm.district = dir.district;
        this.addressForm.reference = dir.reference || '';
      }
    } else {
      this.addressForm.line1 = '';
      this.addressForm.city = '';
      this.addressForm.district = '';
      this.addressForm.reference = '';
    }
  }
  items: CartItem[] = [];
  totals: CartTotals = { itemsCount: 0, subtotal: 0, deliveryFee: 0, total: 0 };
  mensaje = '';
  paymentMethods: PaymentMethodDTO[] = [];
  selectedPaymentMethodId: number | null = null;
  selectedPaymentMethod: PaymentMethodDTO | null = null;
  paymentMethodsLoaded = false;
  cardForm = { holder: '', number: '', expiry: '', cvv: '' };
  mobileForm = { phone: '', reference: '' };
  notes = '';
  checkoutError = '';

  showCvv = false;

  checkoutSuccess = '';

  isProcessing = false;
  isSuccess = false;
  confirmedOrderId: number | null = null;
  confirmedEmailSent = false;
  confirmedEmailAddress = '';
  confirmedAdminEmailSent = false;
  confirmedBoletaComprobante: string | null = null;
  confirmedBoletaPdfUrl: string | null = null;
  confirmedPaymentViaMp = false;
  confirmedPaymentMethodLabel = '';

  mpConfigLoaded = false;
  mpConfigured = false;
  mpPublicKey = '';
  mpSandboxMode = true;
  mpLocale = 'es-PE';
  mpSdkReady = false;
  mpBrickLoading = false;
  mpBrickReady = false;
  mpBrickError = '';
  requiresCardDetails = false;
  isYapeMethod = false;
  isPlinOrTransferMethod = false;
  isCashMethod = false;
  requiresMobileDetails = false;
  useMercadoPagoCardBrick = false;
  useMercadoPagoYape = false;
  isUserAuthenticated = false;
  private mpInstance: any = null;
  private cardBrickController: any = null;
  private lastMountedBrickAmount = -1;
  private brickMountToken = 0;
  private brickMountTimer: ReturnType<typeof setTimeout> | null = null;

  wantsBoleta = false;
  userHasSavedDni = false;
  editingSavedDni = false;
  boletaDni = '';
  boletaNombre = '';
  boletaAgeVerified = false;
  buscandoDni = false;
  guardandoDni = false;
  boletaDniMsg = '';
  boletaDniError = '';

  processingStep: 1 | 2 | 3 | 4 = 1;
  processingProgress = 12;
  processingTitle = 'Registrando tu pedido...';
  processingSubtitle = 'Verificando disponibilidad de ingredientes en cocina';
  private processingTimers: Array<ReturnType<typeof setTimeout>> = [];
  private progressInterval: ReturnType<typeof setInterval> | null = null;
  private readonly subs: Subscription[] = [];
  direcciones: AddressDTO[] = [];
  selectedAddressId: number | null = null;

  showDeleteModal = false;
  itemToDelete: CartItem | null = null;

  deliveryType: 'PICKUP' | 'DELIVERY' = 'DELIVERY';
  baseDeliveryFee = 5.0;

  addressForm = {
    line1: '',
    city: '',
    district: '',
    reference: '',
  };

  ingredientesMap: Map<string, IngredientDTO> = new Map();

  promoCode = '';
  promoApplied = false;
  promoDiscount = 0;
  promoError = '';
  promoSuccess = '';
  isValidatingPromo = false;

  constructor(
    private readonly cartService: CartService,
    private readonly checkoutService: CheckoutService,
    private readonly paymentMethodService: PaymentMethodService,
    private readonly ingredientService: IngredientService,
    private readonly imageOptimizer: ImageOptimizerService,
    private readonly addressService: AddressService,
    private readonly authService: AuthService,
    private readonly promocionService: PromocionService,
    private readonly accessibility: AccessibilityService,
    private readonly toastService: ToastService,
    private readonly paymentService: Payment,
    private readonly sunatService: SunatService,
    private readonly usuarioService: UsuarioService,
    private readonly storeSettingsService: StoreSettingsService,
    private readonly router: Router,
    private readonly ngZone: NgZone,
  ) {}

  ngOnInit(): void {
    this.subs.push(
      this.storeSettingsService.deliveryFee$.subscribe((fee) => {
        this.baseDeliveryFee = fee;
        this.cartService.setDeliveryFeeRate(fee);
        this.recalcularTotales();
      }),
    );
    this.storeSettingsService.loadDeliveryFee().subscribe();

    this.subs.push(
      this.cartService.items$.subscribe((items) => {
        this.items = items;
        this.recalcularTotales();
        if (items.length === 0) {
          void this.unmountCardBrick();
          if (!this.isProcessing && !this.isSuccess) {
            this.accessibility.announce('Carrito vacío');
          }
        } else {
          this.accessibility.announceCartTotal(this.totals.total);
        }
      }),
    );

    this.loadPaymentMethods();
    this.loadMercadoPagoConfig();
    this.loadIngredientes();
    this.cargarPromocionGuardada();

    const userId = this.authService.obtenerUsuarioId();
    const currentUser = this.authService.getCurrentUser();
    this.isUserAuthenticated = Boolean(userId && currentUser?.id);
    this.subs.push(
      this.authService.currentUser$.subscribe((u) => {
        this.isUserAuthenticated = Boolean(u?.id && this.authService.obtenerUsuarioId());
      }),
    );
    if (currentUser?.dni) {
      this.boletaDni = currentUser.dni;
      this.boletaNombre = currentUser.name || '';
      this.boletaAgeVerified = true;
      this.userHasSavedDni = true;
    }
    if (userId) {
      this.usuarioService.obtenerUsuarioActual().subscribe({
        next: (u) => {
          if (u) {
            this.authService.updateCurrentUser(u);
            if (u.dni && /^\d{8}$/.test(u.dni.trim())) {
              this.boletaDni = u.dni.trim();
              this.boletaNombre = u.name || '';
              this.boletaAgeVerified = true;
              this.userHasSavedDni = true;
            }
          }
        },
        error: () => {},
      });

      this.addressService.obtenerPorUserId(userId).subscribe({
        next: (direcciones) => {
          this.direcciones = direcciones.filter(
            (dir, idx, arr) =>
              arr.findIndex(
                (d) =>
                  (d.line1 || '').trim().toLowerCase() === (dir.line1 || '').trim().toLowerCase() &&
                  (d.city || '').trim().toLowerCase() === (dir.city || '').trim().toLowerCase() &&
                  (d.district || '').trim().toLowerCase() === (dir.district || '').trim().toLowerCase(),
              ) === idx,
          );
          if (this.direcciones.length > 0 && !this.selectedAddressId) {
            const def = this.direcciones.find((d) => d.isDefault) || this.direcciones[0];
            this.selectedAddressId = def?.id || null;
            this.onSavedAddressChange();
          } else if (this.direcciones.length === 0) {
            this.addressOption = 'new';
          }
        },
        error: () => {
          this.direcciones = [];
          this.addressOption = 'new';
        },
      });
    }

    if (this.deliveryType === 'DELIVERY') {
      this.addressOption = 'saved';
    }

    this.accessibility.announce('Carrito cargado. Revisa tus items y procede al pago');
  }

  ngOnDestroy(): void {
    this.clearProcessingTimers();
    if (this.brickMountTimer !== null) {
      clearTimeout(this.brickMountTimer);
      this.brickMountTimer = null;
    }
    void this.unmountCardBrick();
    this.subs.forEach((sub) => sub.unsubscribe());
  }

  private loadMercadoPagoConfig(): void {
    const sub = this.paymentService.obtenerConfiguracionMercadoPago().subscribe({
      next: (config) => {
        this.mpConfigLoaded = true;
        this.mpConfigured = Boolean(config?.configured && config?.publicKey);
        this.mpPublicKey = config?.publicKey || '';
        this.mpSandboxMode = config?.sandboxMode ?? true;
        this.mpLocale = config?.locale || 'es-PE';
        this.syncPaymentMethodFlags();

        if (this.mpConfigured && this.mpPublicKey) {
          this.loadMercadoPagoSdk()
            .then(() => {
              this.ngZone.run(() => {
                if (typeof window.MercadoPago === 'function') {
                  this.mpInstance = new window.MercadoPago(this.mpPublicKey, {
                    locale: this.mpLocale,
                  });
                  this.mpSdkReady = true;
                  this.mpBrickError = '';
                  this.syncPaymentMethodFlags();
                  if (this.useMercadoPagoCardBrick) {
                    this.scheduleCardBrickMount(true);
                  }
                }
              });
            })
            .catch(() => {
              this.ngZone.run(() => {
                this.mpSdkReady = false;
                this.mpBrickError = 'No se pudo cargar el SDK seguro de Mercado Pago. Revisa tu conexión.';
              });
            });
        }
      },
      error: () => {
        this.mpConfigLoaded = true;
        this.mpConfigured = false;
        this.syncPaymentMethodFlags();
      },
    });
    this.subs.push(sub);
  }

  private loadMercadoPagoSdk(): Promise<void> {
    if (typeof window.MercadoPago === 'function') {
      return Promise.resolve();
    }

    return new Promise<void>((resolve, reject) => {
      const existing = document.getElementById('mercado-pago-sdk-v2') as HTMLScriptElement | null;
      if (existing) {
        if (typeof window.MercadoPago === 'function') {
          resolve();
          return;
        }
        existing.addEventListener('load', () => resolve(), { once: true });
        existing.addEventListener('error', () => reject(new Error('MP SDK load error')), { once: true });
        return;
      }

      const script = document.createElement('script');
      script.id = 'mercado-pago-sdk-v2';
      script.src = 'https://sdk.mercadopago.com/js/v2';
      script.async = true;
      script.onload = () => resolve();
      script.onerror = () => reject(new Error('MP SDK load error'));
      document.head.appendChild(script);
    });
  }

  reintentarCardBrick(): void {
    this.mpBrickError = '';
    if (!this.mpSdkReady && this.mpConfigured && this.mpPublicKey) {
      this.loadMercadoPagoConfig();
      return;
    }
    this.scheduleCardBrickMount(true);
  }

  private async unmountCardBrick(): Promise<void> {
    this.brickMountToken++;
    if (this.brickMountTimer !== null) {
      clearTimeout(this.brickMountTimer);
      this.brickMountTimer = null;
    }
    if (this.cardBrickController) {
      const ctrl = this.cardBrickController;
      this.cardBrickController = null;
      try {
        await ctrl.unmount();
      } catch {
      }
    }
    this.lastMountedBrickAmount = -1;
    this.mpBrickReady = false;
    this.mpBrickLoading = false;
  }

  private scheduleCardBrickMount(force = false): void {
    if (!this.useMercadoPagoCardBrick || !this.mpSdkReady || !this.mpInstance) {
      return;
    }
    if (this.items.length === 0 || this.isProcessing || this.isSuccess) {
      return;
    }

    const amount = Number(this.totals.total.toFixed(2));
    if (amount <= 0) {
      return;
    }

    if (!force && this.cardBrickController && this.lastMountedBrickAmount === amount) {
      return;
    }

    if (this.brickMountTimer !== null) {
      clearTimeout(this.brickMountTimer);
    }

    const myToken = ++this.brickMountToken;
    this.mpBrickLoading = true;
    this.mpBrickReady = false;
    this.mpBrickError = '';

    this.brickMountTimer = setTimeout(() => {
      void this.mountCardBrick(amount, myToken);
    }, 90);
  }

  private async mountCardBrick(amount: number, expectedToken: number): Promise<void> {
    const container = document.getElementById('mp-card-brick-container');
    if (!container || expectedToken !== this.brickMountToken || !this.mpInstance) {
      this.ngZone.run(() => {
        this.mpBrickLoading = false;
      });
      return;
    }

    if (this.cardBrickController) {
      const prev = this.cardBrickController;
      this.cardBrickController = null;
      try {
        await prev.unmount();
      } catch {
      }
    }

    if (expectedToken !== this.brickMountToken) {
      return;
    }

    container.innerHTML = '';

    const currentUser = this.authService.getCurrentUser();
    const cleanDni = /^\d{8}$/.test((this.boletaDni || '').trim())
      ? this.boletaDni.trim()
      : (currentUser?.dni && /^\d{8}$/.test(currentUser.dni.trim()) ? currentUser.dni.trim() : '');

    const payerInit: Record<string, any> = {};
    if (currentUser?.email) {
      payerInit['email'] = currentUser.email;
    }
    if (cleanDni) {
      payerInit['identification'] = { type: 'DNI', number: cleanDni };
    }

    try {
      const bricksBuilder = this.mpInstance.bricks();
      const controller = await bricksBuilder.create('cardPayment', 'mp-card-brick-container', {
        initialization: {
          amount,
          payer: payerInit,
        },
        customization: {
          visual: {
            hideFormTitle: true,
            style: {
              theme: 'default',
              customVariables: {
                baseColor: '#c8102e',
                baseColorFirstVariant: '#a00d24',
                baseColorSecondVariant: '#7f0a1c',
              },
            },
          },
          paymentMethods: {
            maxInstallments: 1,
          },
        },
        callbacks: {
          onReady: () => {
            this.ngZone.run(() => {
              if (expectedToken === this.brickMountToken) {
                this.mpBrickLoading = false;
                this.mpBrickReady = true;
                this.mpBrickError = '';
              }
            });
          },
          onSubmit: (cardFormData: any) => {
            return this.handleMercadoPagoCardSubmit(cardFormData);
          },
          onError: (error: any) => {
            console.error('Mercado Pago Card Brick error:', error);
            this.ngZone.run(() => {
              this.mpBrickLoading = false;
              this.mpBrickReady = false;
              this.mpBrickError =
                'No se pudo inicializar el módulo de tarjeta de Mercado Pago. Verifica tu Public Key en .env.';
            });
          },
        },
      });

      if (expectedToken !== this.brickMountToken) {
        try {
          await controller.unmount();
        } catch {
        }
        return;
      }

      this.cardBrickController = controller;
      this.lastMountedBrickAmount = amount;
    } catch (err) {
      console.error('Error al montar Mercado Pago Card Brick:', err);
      this.ngZone.run(() => {
        this.mpBrickLoading = false;
        this.mpBrickReady = false;
        this.mpBrickError = 'Ocurrió un error al cargar Mercado Pago. Intenta recargar el módulo.';
      });
    }
  }

  private handleMercadoPagoCardSubmit(cardFormData: any): Promise<void> {
    return new Promise<void>((resolve) => {
      this.ngZone.run(() => {
        if (this.items.length === 0 || this.isProcessing) {
          resolve();
          return;
        }

        const precheckError = this.validateDeliveryAndBoletaBeforePay();
        if (precheckError) {
          this.checkoutError = precheckError;
          this.toastService.showError(precheckError);
          resolve();
          return;
        }

        const token = cardFormData?.token;
        if (!token) {
          this.checkoutError = 'Mercado Pago no generó el token seguro de tu tarjeta. Verifica los datos ingresados.';
          this.toastService.showError(this.checkoutError);
          resolve();
          return;
        }

        const payload = this.buildBaseCheckoutPayload();
        const currentUser = this.authService.getCurrentUser();

        payload.payment = {
          paymentMethodId: this.selectedPaymentMethodId!,
          amount: this.totals.total,
          status: 'PENDING',
          mercadoPagoToken: token,
          mercadoPagoPaymentMethodId: cardFormData?.payment_method_id || 'visa',
          mercadoPagoIssuerId: cardFormData?.issuer_id ? String(cardFormData.issuer_id) : undefined,
          mercadoPagoInstallments: Number(cardFormData?.installments) || 1,
          mercadoPagoPayerEmail: cardFormData?.payer?.email || currentUser?.email || undefined,
          mercadoPagoPayerDocType: cardFormData?.payer?.identification?.type || 'DNI',
          mercadoPagoPayerDocNumber:
            cardFormData?.payer?.identification?.number ||
            (this.wantsBoleta ? this.boletaDni.trim() : currentUser?.dni || undefined),
        };

        this.checkoutError = '';
        this.checkoutSuccess = '';
        this.confirmedPaymentViaMp = true;
        this.confirmedPaymentMethodLabel = 'Tarjeta vía Mercado Pago';

        this.finalizarCheckoutBackend(payload, () => {
          resolve();
        });
      });
    });
  }

  private async generarTokenYapeMercadoPago(phone: string, otp: string): Promise<string> {
    if (!this.mpInstance) {
      throw new Error('El SDK seguro de Mercado Pago todavía está cargando. Espera un segundo e intenta nuevamente.');
    }

    const yapePayload = {
      otp: otp.trim(),
      phoneNumber: phone.trim(),
    };

    try {
      let tokenResponse: any;
      if (typeof this.mpInstance.yape === 'function') {
        const yapeClient = this.mpInstance.yape(yapePayload);
        tokenResponse = await yapeClient.create();
      } else if (this.mpInstance.yape && typeof this.mpInstance.yape.create === 'function') {
        tokenResponse = await this.mpInstance.yape.create(yapePayload);
      } else {
        throw new Error('Tu versión del SDK de Mercado Pago no expuso el módulo Yape.');
      }

      const token = tokenResponse?.id || tokenResponse?.token;
      if (!token) {
        throw new Error('No se recibió un token válido de Yape.');
      }
      return String(token);
    } catch (err: any) {
      const detail =
        err?.message ||
        err?.cause?.[0]?.description ||
        'Verifica que tu número celular (9 dígitos) y el código de aprobación Yape (6 dígitos) sean correctos.';
      throw new Error(`No se pudo validar tu código Yape en Mercado Pago: ${detail}`);
    }
  }

  actualizarCantidad(item: CartItem, valor: string): void {
    const nuevaCantidad = Number(valor);
    if (Number.isNaN(nuevaCantidad)) {
      return;
    }

    this.cartService.updateQuantity(item.id, nuevaCantidad);
    if (this.promoApplied) {
      this.quitarPromocion();
      this.mostrarMensaje('Cantidad actualizada y promoción removida');
    } else {
      this.mostrarMensaje('Cantidad actualizada');
    }
  }

  incrementar(item: CartItem): void {
    if (item.maxStock !== undefined && item.maxStock > 0 && item.quantity >= item.maxStock) {
      this.toastService.showError(`Stock máximo disponible alcanzado (${item.maxStock})`);
      return;
    }
    this.cartService.updateQuantity(item.id, item.quantity + 1);
    this.accessibility.announceQuantityChange(item.quantity + 1, item.name);
    if (this.promoApplied) this.quitarPromocion();
  }

  decrementar(item: CartItem): void {
    this.cartService.updateQuantity(item.id, item.quantity - 1);
    this.accessibility.announceQuantityChange(item.quantity - 1, item.name);
    if (this.promoApplied) this.quitarPromocion();
  }

  eliminar(item: CartItem): void {
    this.itemToDelete = item;
    this.showDeleteModal = true;
  }

  confirmarEliminar(): void {
    if (this.itemToDelete) {
      this.cartService.removeItem(this.itemToDelete.id);
      if (this.promoApplied) this.quitarPromocion();
      this.mostrarMensaje(`${this.itemToDelete.name} eliminado del carrito`);
      this.accessibility.announceRemoveFromCart(this.itemToDelete.name);
      this.cerrarModalEliminar();
    }
  }

  cerrarModalEliminar(): void {
    this.showDeleteModal = false;
    this.itemToDelete = null;
  }

  vaciar(): void {
    if (this.items.length === 0) {
      return;
    }
    if (confirm('¿Deseas vaciar el carrito?')) {
      this.cartService.clearCart();
      if (this.promoApplied) this.quitarPromocion();
      this.mostrarMensaje('Carrito vacío');
      this.accessibility.announce('Carrito vacío');
    }
  }

  private validateDeliveryAndBoletaBeforePay(): string | null {
    if (!this.authService.obtenerUsuarioId() || !this.authService.getCurrentUser()?.id) {
      return 'Debes iniciar sesión para confirmar tu pedido.';
    }

    if (!this.selectedPaymentMethodId) {
      return this.paymentMethods.length === 0
        ? 'No hay métodos de pago disponibles en este momento.'
        : 'Selecciona un método de pago para continuar.';
    }

    if (
      this.deliveryType === 'DELIVERY' &&
      this.addressOption === 'saved' &&
      this.selectedAddressId
    ) {
      const dir = this.direcciones.find((d) => d.id === this.selectedAddressId);
      if (dir) {
        this.addressForm.line1 = dir.line1;
        this.addressForm.city = dir.city;
        this.addressForm.district = dir.district;
        this.addressForm.reference = dir.reference || '';
      }
    }
    const addressError = this.validateAddressForm();
    if (addressError) {
      return addressError;
    }

    if (this.wantsBoleta) {
      const cleanDni = (this.boletaDni || '').trim();
      if (!/^\d{8}$/.test(cleanDni)) {
        return 'Para emitir Boleta de Venta Electrónica debes ingresar un DNI válido de 8 dígitos.';
      }
      if (!this.userHasSavedDni && !this.boletaAgeVerified) {
        return 'Debes confirmar que eres mayor de 18 años (+18) y que el DNI ingresado te pertenece.';
      }
    }

    return null;
  }

  private buildBaseCheckoutPayload(): CheckoutPayload {
    const combinedNotes = this.composeNotes();

    const payload: CheckoutPayload = {
      notes: combinedNotes,
      paymentMethodId: this.selectedPaymentMethodId!,
      deliveryType: this.deliveryType,
      wantsBoleta: this.wantsBoleta,
      clienteDni: this.wantsBoleta ? this.boletaDni.trim() : undefined,
      clienteNombre: this.wantsBoleta
        ? this.boletaNombre.trim() || this.authService.getCurrentUser()?.name || ''
        : undefined,
      ageVerified: this.wantsBoleta ? true : undefined,
    };

    if (this.deliveryType === 'DELIVERY' && this.addressOption === 'saved' && this.selectedAddressId) {
      payload.addressId = this.selectedAddressId;
    } else if (this.deliveryType === 'DELIVERY' && this.addressOption === 'new') {
      const existingMatch = this.direcciones.find(
        (d) =>
          (d.line1 || '').trim().toLowerCase() === this.addressForm.line1.trim().toLowerCase() &&
          (d.city || '').trim().toLowerCase() === this.addressForm.city.trim().toLowerCase() &&
          (d.district || '').trim().toLowerCase() === this.addressForm.district.trim().toLowerCase(),
      );
      if (existingMatch?.id) {
        payload.addressId = existingMatch.id;
      }
    }

    if (this.promoApplied && this.promoCode.trim()) {
      payload.promoCode = this.promoCode.trim();
      payload.discount = this.promoDiscount;
    }

    if (this.deliveryType === 'DELIVERY') {
      payload.address = {
        line1: this.addressForm.line1.trim(),
        city: this.addressForm.city.trim(),
        district: this.addressForm.district.trim(),
        reference: this.addressForm.reference.trim() || undefined,
      };
    }

    return payload;
  }

  async checkout(): Promise<void> {
    if (this.items.length === 0 || this.isProcessing) {
      return;
    }

    const precheckError = this.validateDeliveryAndBoletaBeforePay();
    if (precheckError) {
      this.checkoutError = precheckError;
      return;
    }

    if (this.useMercadoPagoCardBrick) {
      this.checkoutError = '';
      const brickEl = document.getElementById('mp-card-brick-section');
      if (brickEl) {
        brickEl.scrollIntoView({ behavior: 'smooth', block: 'center' });
      }
      this.toastService.showInfo(
        'Completa tu tarjeta en el módulo seguro de Mercado Pago y presiona el botón "Pagar".',
      );
      return;
    }

    const detailError = this.validatePaymentDetails();
    if (detailError) {
      this.checkoutError = detailError;
      return;
    }

    this.checkoutError = '';
    this.checkoutSuccess = '';

    const payload = this.buildBaseCheckoutPayload();
    const currentUser = this.authService.getCurrentUser();

    if (this.useMercadoPagoYape) {
      this.isProcessing = true;
      try {
        const yapeToken = await this.generarTokenYapeMercadoPago(
          this.mobileForm.phone,
          this.mobileForm.reference,
        );
        payload.payment = {
          paymentMethodId: this.selectedPaymentMethodId!,
          amount: this.totals.total,
          status: 'PENDING',
          mercadoPagoToken: yapeToken,
          mercadoPagoPaymentMethodId: 'yape',
          mercadoPagoInstallments: 1,
          mercadoPagoPayerEmail: currentUser?.email || undefined,
          mercadoPagoPayerDocType: 'DNI',
          mercadoPagoPayerDocNumber: this.wantsBoleta
            ? this.boletaDni.trim()
            : currentUser?.dni || undefined,
          mercadoPagoPayerPhone: this.mobileForm.phone.trim(),
        };
        this.confirmedPaymentViaMp = true;
        this.confirmedPaymentMethodLabel = 'Yape vía Mercado Pago';
        this.finalizarCheckoutBackend(payload);
      } catch (err: any) {
        this.isProcessing = false;
        this.checkoutError =
          err?.message || 'No se pudo procesar tu código Yape con Mercado Pago.';
        this.toastService.showError(this.checkoutError);
      }
      return;
    }

    if (this.isCashMethod) {
      this.confirmedPaymentViaMp = false;
      this.confirmedPaymentMethodLabel = 'Efectivo (pago al recibir)';
      payload.payment = {
        paymentMethodId: this.selectedPaymentMethodId!,
        amount: this.totals.total,
        status: 'PENDING',
      };
      this.finalizarCheckoutBackend(payload);
      return;
    }

    this.confirmedPaymentViaMp = false;
    this.confirmedPaymentMethodLabel = this.selectedPaymentMethod?.name || 'Pago registrado';
    payload.payment = {
      paymentMethodId: this.selectedPaymentMethodId!,
      amount: this.totals.total,
      status: 'PAID',
      transactionId:
        this.requiresMobileDetails && this.mobileForm.reference.trim()
          ? 'REF-' + this.mobileForm.reference.trim().toUpperCase()
          : 'LOCAL-' + Math.random().toString(36).substring(2, 11).toUpperCase(),
    };

    this.finalizarCheckoutBackend(payload);
  }

  private clearProcessingTimers(): void {
    this.processingTimers.forEach((timerId) => clearTimeout(timerId));
    this.processingTimers = [];
    if (this.progressInterval !== null) {
      clearInterval(this.progressInterval);
      this.progressInterval = null;
    }
  }

  private startProcessingAnimation(viaMercadoPago = false): void {
    this.clearProcessingTimers();
    this.isProcessing = true;
    this.processingStep = 1;
    this.processingProgress = 12;
    this.processingTitle = viaMercadoPago
      ? 'Procesando pago seguro en Mercado Pago...'
      : 'Registrando tu pedido...';
    this.processingSubtitle = viaMercadoPago
      ? 'Validando autorización de pago y stock disponible en cocina'
      : 'Verificando disponibilidad de ingredientes en cocina';

    this.processingTimers.push(
      setTimeout(() => {
        if (!this.isProcessing || this.processingStep >= 4) return;
        this.processingStep = 2;
        this.processingProgress = Math.max(this.processingProgress, 42);
        this.processingTitle = this.wantsBoleta
          ? 'Emitiendo Boleta de Venta Electrónica SUNAT...'
          : 'Confirmando pago y preparando orden...';
        this.processingSubtitle = this.wantsBoleta
          ? 'Generando comprobante electrónico serie BBB1 en Nubefact'
          : 'Asegurando los detalles de tu pedido';
      }, 1100),
      setTimeout(() => {
        if (!this.isProcessing || this.processingStep >= 4) return;
        this.processingStep = 3;
        this.processingProgress = Math.max(this.processingProgress, 70);
        this.processingTitle = this.wantsBoleta
          ? 'Enviando tu Boleta Electrónica (PDF) por correo...'
          : 'Enviando confirmación a tu correo...';
        this.processingSubtitle = 'Despachando el comprobante a tu bandeja y avisando a tienda';
      }, 2200),
    );

    this.progressInterval = setInterval(() => {
      if (!this.isProcessing || this.processingStep >= 4) {
        return;
      }
      if (this.processingStep === 1 && this.processingProgress < 38) {
        this.processingProgress = Math.min(38, this.processingProgress + 3);
      } else if (this.processingStep === 2 && this.processingProgress < 66) {
        this.processingProgress = Math.min(66, this.processingProgress + 3);
      } else if (this.processingStep === 3 && this.processingProgress < 93) {
        const stepIncrement = this.processingProgress < 85 ? 2 : 0.6;
        this.processingProgress = Number(Math.min(93, this.processingProgress + stepIncrement).toFixed(1));
      }
    }, 140);
  }

  finalizarCheckoutBackend(payload: CheckoutPayload, onFinished?: (ok: boolean) => void): void {
    const startTime = Date.now();
    const MIN_ANIMATION_MS = 3300;
    const isMpPayment = Boolean(payload.payment?.mercadoPagoToken);
    this.startProcessingAnimation(isMpPayment);

    const sub = this.checkoutService.checkout(payload).subscribe({
      next: (result) => {
        this.confirmedOrderId = result?.order?.id ?? null;
        this.confirmedEmailSent = Boolean(result?.order?.emailSent);
        this.confirmedEmailAddress =
          result?.order?.emailSentTo || this.authService.getCurrentUser()?.email || '';
        this.confirmedAdminEmailSent = Boolean(result?.order?.adminEmailSent);
        this.confirmedBoletaComprobante = result?.order?.boletaComprobante || null;
        this.confirmedBoletaPdfUrl = result?.order?.boletaPdfUrl || null;

        if (this.wantsBoleta && /^\d{8}$/.test((this.boletaDni || '').trim())) {
          this.userHasSavedDni = true;
          this.editingSavedDni = false;
          const cur = this.authService.getCurrentUser();
          if (cur) {
            this.authService.updateCurrentUser({
              ...cur,
              dni: this.boletaDni.trim(),
              ageVerified: true,
            });
          }
        }

        const elapsed = Date.now() - startTime;
        const waitBeforeCompleteMs = Math.max(0, MIN_ANIMATION_MS - elapsed);

        this.processingTimers.push(
          setTimeout(() => {
            this.processingStep = 4;
            this.processingProgress = 100;
            this.processingTitle = this.confirmedEmailSent
              ? '¡Pedido confirmado y correo enviado!'
              : '¡Pedido registrado con éxito!';
            this.processingSubtitle = this.confirmedEmailSent
              ? `Comprobante enviado a ${this.confirmedEmailAddress || 'tu correo electrónico'}`
              : 'Tu orden ya entró en preparación en cocina';

            this.processingTimers.push(
              setTimeout(() => {
                this.clearProcessingTimers();
                this.isProcessing = false;
                this.isSuccess = true;
                void this.unmountCardBrick();
                if (this.confirmedEmailSent) {
                  this.toastService.showSuccess(
                    `¡Se envió el correo de confirmación a ${this.confirmedEmailAddress || 'tu bandeja'}!`,
                  );
                  this.accessibility.announceSuccess(
                    'Pedido confirmado con éxito. Se envió el correo de confirmación.',
                  );
                } else {
                  this.toastService.showInfo(
                    'Pedido registrado en cocina. El envío de correo quedó pendiente de conexión SMTP.',
                  );
                  this.accessibility.announceSuccess('Pedido confirmado con éxito.');
                }
                this.resetPaymentForms();
                this.limpiarPromocionDeStorage();
                onFinished?.(true);
              }, 450),
            );
          }, waitBeforeCompleteMs),
        );
      },
      error: (err) => {
        this.clearProcessingTimers();
        this.checkoutError = err?.message ?? 'No se pudo completar el pedido.';
        this.isProcessing = false;
        this.confirmedPaymentViaMp = false;
        this.toastService.showError(this.checkoutError);
        this.accessibility.announceError(this.checkoutError);
        onFinished?.(false);
      },
    });

    this.subs.push(sub);
  }

  cerrarExito(): void {
    this.isSuccess = false;
    this.router.navigate(['/menu']);
  }

  consultarDniCarrito(): void {
    this.boletaDniError = '';
    this.boletaDniMsg = '';
    const clean = (this.boletaDni || '').trim();
    if (!/^\d{8}$/.test(clean)) {
      this.boletaDniError = 'Ingresa un DNI válido de 8 dígitos numéricos.';
      return;
    }
    this.buscandoDni = true;
    this.sunatService.consultarDni(clean).subscribe({
      next: (res) => {
        this.buscandoDni = false;
        if (res?.nombreCompleto) {
          this.boletaNombre = res.nombreCompleto;
          this.boletaDniMsg = `Titular RENIEC: ${res.nombreCompleto}`;
        }
      },
      error: (err) => {
        this.buscandoDni = false;
        this.boletaDniError = err?.error?.message || 'No se pudo consultar el DNI en RENIEC.';
      },
    });
  }

  guardarDniEnCuenta(): void {
    this.boletaDniError = '';
    this.boletaDniMsg = '';
    if (!this.authService.obtenerUsuarioId() || !this.authService.getCurrentUser()?.id) {
      this.boletaDniError = 'Debes iniciar sesión para guardar tu DNI en tu cuenta.';
      this.toastService.showInfo('Inicia sesión para guardar tu DNI en tu cuenta.');
      return;
    }
    const clean = (this.boletaDni || '').trim();
    if (!/^\d{8}$/.test(clean)) {
      this.boletaDniError = 'Ingresa un DNI válido de 8 dígitos.';
      return;
    }
    if (!this.boletaAgeVerified) {
      this.boletaDniError = 'Debes confirmar que eres mayor de 18 años y titular del DNI.';
      return;
    }
    this.guardandoDni = true;
    this.sunatService.configurarDni(clean, true, this.boletaNombre || undefined).subscribe({
      next: (u) => {
        this.guardandoDni = false;
        this.userHasSavedDni = true;
        this.editingSavedDni = false;
        if (u?.name) {
          this.boletaNombre = u.name;
        }
        this.authService.updateCurrentUser(u);
        this.boletaDniMsg = 'DNI guardado en tu cuenta para este y próximos pedidos.';
        this.toastService.showSuccess('DNI vinculado correctamente a tu cuenta.');
      },
      error: (err) => {
        this.guardandoDni = false;
        if (err?.status === 401 || err?.status === 403) {
          this.boletaDniError = 'Debes iniciar sesión para guardar tu DNI en tu cuenta.';
          return;
        }
        this.boletaDniError = err?.error?.message || 'No se pudo vincular el DNI.';
      },
    });
  }

  abrirBoletaConfirmadaPdf(): void {
    if (!this.confirmedOrderId) return;
    this.sunatService.abrirBoletaPdf(this.confirmedOrderId, this.confirmedBoletaPdfUrl);
  }

  get isCheckoutDisabled(): boolean {
    if (this.deliveryType === 'DELIVERY') {
      if (this.addressOption === 'saved') {
        return (
          this.items.length === 0 ||
          this.isProcessing ||
          !this.selectedPaymentMethodId ||
          this.direcciones.length === 0 ||
          !this.selectedAddressId
        );
      }
      if (this.addressOption === 'new') {
        return (
          this.items.length === 0 ||
          this.isProcessing ||
          !this.selectedPaymentMethodId ||
          !this.addressForm.line1.trim() ||
          !this.addressForm.city.trim() ||
          !this.addressForm.district.trim()
        );
      }
    }
    return this.items.length === 0 || this.isProcessing || !this.selectedPaymentMethodId;
  }

  private mostrarMensaje(texto: string): void {
    this.toastService.showSuccess(texto);
  }

  private loadPaymentMethods(): void {
    const sub = this.paymentMethodService.listarMetodosPagoActivos().subscribe({
      next: (metodos) => {
        this.paymentMethods = metodos;
        this.paymentMethodsLoaded = true;
        if (metodos.length > 0) {
          this.onPaymentMethodChange(metodos[0] ?? null);
        } else {
          this.selectedPaymentMethodId = null;
          this.selectedPaymentMethod = null;
          this.syncPaymentMethodFlags();
        }
      },
      error: () => {
        this.paymentMethods = [];
        this.paymentMethodsLoaded = true;
        this.selectedPaymentMethodId = null;
        this.selectedPaymentMethod = null;
        this.syncPaymentMethodFlags();
      },
    });

    this.subs.push(sub);
  }

  loadIngredientes(): void {
    this.ingredientService.obtenerTodos().subscribe({
      next: (ingredientes) => {
        ingredientes.forEach((ing) => {
          if (ing.id) {
            this.ingredientesMap.set(ing.id.toString(), ing);
          }
        });
      },
      error: (error) => {
        console.error('Error al cargar ingredientes:', error);
      },
    });
  }

  getExtrasNombres(item: CartItem): string {
    if (!item.extras || item.extras.length === 0) {
      return '';
    }

    const nombres = item.extras
      .map((id) => this.ingredientesMap.get(id)?.name || id)
      .filter((nombre) => nombre);

    return nombres.join(', ');
  }

  tieneExtras(item: CartItem): boolean {
    return !!(item.extras && item.extras.length > 0);
  }

  onPaymentMethodChange(method: PaymentMethodDTO | number | null): void {
    if (typeof method === 'number') {
      this.selectedPaymentMethod = this.paymentMethods.find((m) => m.id === method) ?? null;
    } else {
      this.selectedPaymentMethod = method;
    }

    this.selectedPaymentMethodId = this.selectedPaymentMethod?.id ?? null;
    this.syncPaymentMethodFlags();
    this.checkoutError = '';
    this.resetPaymentForms();

    if (this.useMercadoPagoCardBrick) {
      if (this.mpSdkReady) {
        this.scheduleCardBrickMount(true);
      }
    } else {
      void this.unmountCardBrick();
    }
  }

  selectPaymentMethod(method: PaymentMethodDTO): void {
    this.onPaymentMethodChange(method);
  }

  get paymentMethodsSorted(): PaymentMethodDTO[] {
    return [...this.paymentMethods].toSorted((a, b) => (a.displayOrder ?? 0) - (b.displayOrder ?? 0));
  }

  private syncPaymentMethodFlags(): void {
    const name = this.selectedPaymentMethod?.name?.toLowerCase() ?? '';
    this.requiresCardDetails = name.includes('tarjeta') || name.includes('card');
    this.isYapeMethod = name.includes('yape');
    this.isPlinOrTransferMethod =
      !this.isYapeMethod && (name.includes('plin') || name.includes('transferencia'));
    this.isCashMethod = name.includes('efectivo') || name.includes('cash');
    this.requiresMobileDetails = this.isYapeMethod || this.isPlinOrTransferMethod;
    this.useMercadoPagoCardBrick = this.requiresCardDetails && this.mpConfigured;
    this.useMercadoPagoYape = this.isYapeMethod && this.mpConfigured;
  }

  private validatePaymentDetails(): string | null {
    if (!this.selectedPaymentMethodId) {
      return 'Selecciona un método de pago para continuar.';
    }

    if (this.requiresCardDetails && !this.mpConfigured) {
      const { holder, number, expiry, cvv } = this.cardForm;
      if (!holder.trim()) {
        return 'Ingresa el nombre del titular de la tarjeta.';
      }
      if (!/^\d{16}$/.test(number.replace(/\s+/g, ''))) {
        return 'Ingresa un número de tarjeta válido de 16 dígitos.';
      }
      if (!/^(0[1-9]|1[0-2])\/\d{2}$/.test(expiry.trim())) {
        return 'Ingresa la fecha de expiración en formato MM/AA.';
      }
      if (!/^\d{3,4}$/.test(cvv.trim())) {
        return 'Ingresa el CVV válido (3 o 4 dígitos).';
      }
    }

    if (this.useMercadoPagoYape) {
      const { phone, reference } = this.mobileForm;
      const phoneRegex = this.mpSandboxMode ? /^\d{9}$/ : /^9\d{8}$/;
      if (!phoneRegex.test(phone.trim())) {
        return this.mpSandboxMode
          ? 'Ingresa un número de celular Yape de 9 dígitos (en Sandbox usa 111111111).'
          : 'Ingresa tu número de celular Yape (9 dígitos, empieza con 9).';
      }
      if (!/^\d{6}$/.test(reference.trim())) {
        return 'Ingresa el código de aprobación Yape de 6 dígitos generado en tu app Yape.';
      }
      return null;
    }

    if (this.requiresMobileDetails) {
      const { phone, reference } = this.mobileForm;
      const appLabel = this.isYapeMethod ? 'Yape' : 'Plin / transferencia';
      if (!/^9\d{8}$/.test(phone.trim())) {
        return `Ingresa el número de celular asociado a ${appLabel} (9 dígitos).`;
      }
      if (!reference.trim()) {
        return 'Ingresa el código o referencia de la operación.';
      }
    }

    return null;
  }

  private resetPaymentForms(): void {
    this.cardForm = { holder: '', number: '', expiry: '', cvv: '' };
    this.mobileForm = { phone: '', reference: '' };
  }

  private composeNotes(): string | undefined {
    const userNotes = this.notes ? this.notes.trim() : '';
    const details: string[] = [];

    if (this.useMercadoPagoYape) {
      details.push(`Pago Yape Mercado Pago (${this.mobileForm.phone.trim()})`);
    } else if (this.requiresMobileDetails) {
      const label = this.isYapeMethod ? 'Yape' : 'Plin/Transferencia';
      details.push(
        `Pago ${label} ${this.mobileForm.phone.trim()} referencia ${this.mobileForm.reference.trim()}`,
      );
    }

    const extra = details.join(' | ');
    if (!userNotes && !extra) {
      return undefined;
    }

    if (userNotes && extra) {
      return `${userNotes} | ${extra}`;
    }

    return userNotes || extra;
  }

  getPaymentIcon(method: PaymentMethodDTO): string {
    const name = method.name?.toLowerCase() ?? '';
    if (name.includes('efectivo') || name.includes('cash')) {
      return 'bi-cash-stack';
    }
    if (name.includes('tarjeta') || name.includes('card')) {
      return 'bi-credit-card';
    }
    if (name.includes('yape') || name.includes('plin') || name.includes('transferencia')) {
      return 'bi-phone-flip';
    }
    return 'bi-wallet2';
  }

  onDeliveryTypeChange(): void {
    this.recalcularTotales();

    if (this.deliveryType === 'PICKUP') {
      this.addressForm = {
        line1: '',
        city: '',
        district: '',
        reference: '',
      };
    }

    if (this.deliveryType === 'DELIVERY') {
      if (this.addressOption === 'saved') {
        if (this.selectedAddressId) {
          this.onSavedAddressChange();
        } else {
          this.addressForm = {
            line1: '',
            city: '',
            district: '',
            reference: '',
          };
        }
      } else if (this.addressOption === 'new') {
        this.addressForm = {
          line1: '',
          city: '',
          district: '',
          reference: '',
        };
        this.selectedAddressId = null;
      }
    }
  }

  private recalcularTotales(): void {
    const baseTotals = this.cartService.getTotals();

    if (this.deliveryType === 'PICKUP') {
      this.totals = {
        ...baseTotals,
        deliveryFee: 0,
        total: Math.max(0, baseTotals.subtotal - this.promoDiscount),
      };
    } else {
      this.totals = {
        ...baseTotals,
        deliveryFee: this.baseDeliveryFee,
        total: Math.max(0, baseTotals.subtotal + this.baseDeliveryFee - this.promoDiscount),
      };
    }

    if (this.useMercadoPagoCardBrick && this.mpSdkReady) {
      this.scheduleCardBrickMount();
    }
  }

  private validateAddressForm(): string | null {
    if (this.deliveryType === 'PICKUP') {
      return null;
    }

    if (!this.addressForm.line1.trim()) {
      return 'La dirección es requerida para entregas a domicilio.';
    }

    if (!this.addressForm.city.trim()) {
      return 'La ciudad es requerida para entregas a domicilio.';
    }

    if (!this.addressForm.district.trim()) {
      return 'El distrito es requerido para entregas a domicilio.';
    }

    return null;
  }

  getOptimizedImageUrl(imageUrl: string | undefined): string {
    return this.imageOptimizer.optimizeImageUrl(imageUrl, 'low');
  }

  aplicarPromocion(): void {
    if (!this.promoCode.trim()) {
      this.promoError = 'Ingresa un código promocional';
      return;
    }

    if (this.isValidatingPromo) {
      return;
    }

    this.isValidatingPromo = true;
    this.promoError = '';
    this.promoSuccess = '';

    const subtotal = this.cartService.getTotals().subtotal;
    const userId = this.authService.obtenerUsuarioId();
    const items = this.cartService.getItemsSnapshot();

    this.promocionService
      .validarPromocion(this.promoCode.trim(), subtotal, userId || undefined, items)
      .subscribe({
        next: (response) => {
          if (response.valid) {
            this.promoApplied = true;
            this.promoDiscount = response.discount;
            this.promoSuccess =
              response.message || `Código aplicado: -S/. ${response.discount.toFixed(2)}`;
            this.guardarPromocionEnStorage();
            this.recalcularTotales();
          } else {
            this.promoError = response.message || 'Código promocional inválido';
            this.promoApplied = false;
            this.promoDiscount = 0;
          }
          this.isValidatingPromo = false;
        },
        error: (err) => {
          this.promoError = err?.error?.message || 'No se pudo validar el código';
          this.promoApplied = false;
          this.promoDiscount = 0;
          this.isValidatingPromo = false;
        },
      });
  }

  quitarPromocion(): void {
    this.promoCode = '';
    this.promoApplied = false;
    this.promoDiscount = 0;
    this.promoSuccess = '';
    this.promoError = '';
    this.limpiarPromocionDeStorage();
    this.recalcularTotales();
  }

  private guardarPromocionEnStorage(): void {
    const promoData = {
      code: this.promoCode,
      discount: this.promoDiscount,
      applied: this.promoApplied,
    };
    localStorage.setItem('cart_promo', JSON.stringify(promoData));
  }

  private cargarPromocionGuardada(): void {
    const promoData = localStorage.getItem('cart_promo');
    if (promoData) {
      try {
        const promo = JSON.parse(promoData);
        this.promoCode = promo.code || '';
        this.promoApplied = promo.applied || false;
        this.promoDiscount = promo.discount || 0;

        if (this.promoCode && this.promoApplied) {
          setTimeout(() => {
            this.aplicarPromocion();
          }, 500);
        }
      } catch (e) {
        this.limpiarPromocionDeStorage();
      }
    }
  }

  private limpiarPromocionDeStorage(): void {
    localStorage.removeItem('cart_promo');
  }
}
