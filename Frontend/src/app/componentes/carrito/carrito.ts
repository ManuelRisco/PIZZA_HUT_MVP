import { Component, OnDestroy, OnInit, inject } from '@angular/core';
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

declare var KR: any;

@Component({
  selector: 'app-carrito',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './carrito.html',
  styleUrl: './carrito.css',
})
export class CarritoComponent implements OnInit, OnDestroy {
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
        this.direcciones.push(address);
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
  ngOnChanges(): void {
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
  cardForm = { holder: '', number: '', expiry: '', cvv: ''  };
  mobileForm = { phone: '', reference: '' };
  notes = '';
  checkoutError = '';

  showCvv = false;

  checkoutSuccess = '';

  isProcessing = false;
  isSuccess = false;
  private subs: Subscription[] = [];
  direcciones: AddressDTO[] = [];
  selectedAddressId: number | null = null;

  // Modal de eliminación
  showDeleteModal = false;
  itemToDelete: CartItem | null = null;

  // Delivery type
  deliveryType: 'PICKUP' | 'DELIVERY' = 'DELIVERY';
  baseDeliveryFee = 5.0; // Costo de delivery por defecto

  // Address form
  addressForm = {
    line1: '',
    city: '',
    district: '',
    reference: '',
  };

  // Mapa de ingredientes para mostrar nombres
  ingredientesMap: Map<string, IngredientDTO> = new Map();

  // Sistema de promociones
  promoCode = '';
  promoApplied = false;
  promoDiscount = 0;
  promoError = '';
  promoSuccess = '';
  isValidatingPromo = false;

  constructor(
    private cartService: CartService,
    private checkoutService: CheckoutService,
    private paymentMethodService: PaymentMethodService,
    private ingredientService: IngredientService,
    private imageOptimizer: ImageOptimizerService,
    private addressService: AddressService,
    private authService: AuthService,
    private promocionService: PromocionService,
    private accessibility: AccessibilityService,
    private toastService: ToastService,
    private paymentService: Payment,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.subs.push(
      this.cartService.items$.subscribe((items) => {
        this.items = items;
        this.recalcularTotales(); // Recalcular según el tipo de delivery
        // Announce cart update
        if (items.length === 0) {
          this.accessibility.announce('Carrito vacío');
        } else {
          this.accessibility.announceCartTotal(this.totals.total);
        }
      }),
    );

    this.loadPaymentMethods();
    this.loadIngredientes();
    this.cargarPromocionGuardada(); // Cargar código promocional guardado

    // Cargar direcciones del usuario autenticado
    const userId = this.authService.obtenerUsuarioId();
    if (userId) {
      this.addressService.obtenerPorUserId(userId).subscribe({
        next: (direcciones) => {
          // Filtrar direcciones únicas por line1, city, district
          this.direcciones = direcciones.filter(
            (dir, idx, arr) =>
              arr.findIndex(
                (d) => d.line1 === dir.line1 && d.city === dir.city && d.district === dir.district,
              ) === idx,
          );
        },
        error: () => {
          this.direcciones = [];
        },
      });
    }

    if (this.deliveryType === 'DELIVERY') {
      this.addressOption = 'saved';
    }

    // Announce cart ready
    this.accessibility.announce('Carrito cargado. Revisa tus items y procede al pago');
  }

  ngOnDestroy(): void {
    this.subs.forEach((sub) => sub.unsubscribe());
  }

  actualizarCantidad(item: CartItem, valor: string): void {
    const nuevaCantidad = Number(valor);
    if (Number.isNaN(nuevaCantidad)) {
      return;
    }

    this.cartService.updateQuantity(item.id, nuevaCantidad);
    this.mostrarMensaje('Cantidad actualizada');
  }

  incrementar(item: CartItem): void {
    this.cartService.updateQuantity(item.id, item.quantity + 1);
    this.accessibility.announceQuantityChange(item.quantity + 1, item.name);
  }

  decrementar(item: CartItem): void {
    this.cartService.updateQuantity(item.id, item.quantity - 1);
    this.accessibility.announceQuantityChange(item.quantity - 1, item.name);
  }

  eliminar(item: CartItem): void {
    this.itemToDelete = item;
    this.showDeleteModal = true;
  }

  confirmarEliminar(): void {
    if (this.itemToDelete) {
      this.cartService.removeItem(this.itemToDelete.id);
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
      this.mostrarMensaje('Carrito vacío');
      this.accessibility.announce('Carrito vacío');
    }
  }

  checkout(): void {
    if (this.items.length === 0 || this.isProcessing) {
      return;
    }

    if (!this.selectedPaymentMethodId) {
      this.checkoutError =
        this.paymentMethods.length === 0
          ? 'No hay métodos de pago disponibles en este momento.'
          : 'Selecciona un método de pago para continuar.';
      return;
    }

    const detailError = this.validatePaymentDetails();
    if (detailError) {
      this.checkoutError = detailError;
      return;
    }

    // Si opción es 'saved', copiar datos al formulario antes de validar
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
    // Validar dirección si es DELIVERY
    const addressError = this.validateAddressForm();
    if (addressError) {
      this.checkoutError = addressError;
      return;
    }

    this.isProcessing = true;
    this.checkoutError = '';
    this.checkoutSuccess = '';

    const combinedNotes = this.composeNotes();

    const payload: CheckoutPayload = {
      notes: combinedNotes,
      paymentMethodId: this.selectedPaymentMethodId,
      deliveryType: this.deliveryType, // Agregar tipo de delivery
    };

    // Agregar código promocional y descuento si está aplicado
    if (this.promoApplied && this.promoCode.trim()) {
      payload.promoCode = this.promoCode.trim();
      payload.discount = this.promoDiscount;
    }

    // Agregar dirección si es DELIVERY
    if (this.deliveryType === 'DELIVERY') {
      payload.address = {
        line1: this.addressForm.line1.trim(),
        city: this.addressForm.city.trim(),
        district: this.addressForm.district.trim(),
        reference: this.addressForm.reference.trim() || undefined,
      };
    }

    // Simular que el pago fue exitoso automáticamente
    payload.payment = {
      paymentMethodId: this.selectedPaymentMethodId,
      amount: this.totals.total,
      status: 'PAID',
      transactionId: 'SIM-' + Math.random().toString(36).substr(2, 9).toUpperCase(),
    };

    // Llamar directamente al backend
    this.finalizarCheckoutBackend(payload);
  }



  finalizarCheckoutBackend(payload: CheckoutPayload): void {
    this.isProcessing = true;
    
    const sub = this.checkoutService.checkout(payload).subscribe({
      next: (result) => {
        this.isProcessing = false;
        this.isSuccess = true;
        this.accessibility.announceSuccess('Pedido confirmado con éxito.');
        this.resetPaymentForms();
        this.limpiarPromocionDeStorage();
      },
      error: (err) => {
        this.checkoutError = err?.message ?? 'No se pudo completar el pedido.';
        this.isProcessing = false;
        this.accessibility.announceError(this.checkoutError);
      },
    });

    this.subs.push(sub);
  }

  cerrarExito(): void {
    this.isSuccess = false;
    this.router.navigate(['/menu']);
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
        }
      },
      error: () => {
        this.paymentMethods = [];
        this.paymentMethodsLoaded = true;
        this.selectedPaymentMethodId = null;
        this.selectedPaymentMethod = null;
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
    this.resetPaymentForms();
  }

  selectPaymentMethod(method: PaymentMethodDTO): void {
    this.onPaymentMethodChange(method);
  }

  get paymentMethodsSorted(): PaymentMethodDTO[] {
    return [...this.paymentMethods].sort((a, b) => (a.displayOrder ?? 0) - (b.displayOrder ?? 0));
  }

  get requiresCardDetails(): boolean {
    const name = this.selectedPaymentMethod?.name?.toLowerCase() ?? '';
    return name.includes('tarjeta');
  }

  get requiresMobileDetails(): boolean {
    const name = this.selectedPaymentMethod?.name?.toLowerCase() ?? '';
    return name.includes('yape') || name.includes('plin') || name.includes('transferencia');
  }

  private validatePaymentDetails(): string | null {
    if (!this.selectedPaymentMethodId) {
      return 'Selecciona un método de pago para continuar.';
    }

    if (this.requiresCardDetails) {
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

    if (this.requiresMobileDetails) {
      const { phone, reference } = this.mobileForm;
      if (!/^9\d{8}$/.test(phone.trim())) {
        return 'Ingresa el número de celular Yape (9 dígitos).';
      }
      if (!reference.trim()) {
        return 'Ingresa el código o referencia de la transferencia.';
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

    if (this.requiresMobileDetails) {
      details.push(
        `Pago móvil ${this.mobileForm.phone.trim()} referencia ${this.mobileForm.reference.trim()}`,
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

  private generateTransactionId(): string | undefined {
    if (!this.selectedPaymentMethod) {
      return undefined;
    }

    const methodName = this.selectedPaymentMethod.name?.toLowerCase() ?? '';
    const timestamp = Date.now().toString(36).toUpperCase();

    if (this.requiresCardDetails) {
      const digits = this.cardForm.number.replace(/\s+/g, '');
      const lastDigits = digits.slice(-4);
      return `CARD-${lastDigits}-${timestamp}`;
    }

    if (this.requiresMobileDetails) {
      return `YAPE-${this.mobileForm.reference.trim()}-${timestamp}`;
    }

    if (methodName.includes('efectivo')) {
      return `CASH-${timestamp}`;
    }

    return `PAY-${timestamp}`;
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

  /**
   * Maneja el cambio de tipo de delivery
   */
  onDeliveryTypeChange(): void {
    this.recalcularTotales();

    // Limpiar el formulario de dirección si se selecciona PICKUP
    if (this.deliveryType === 'PICKUP') {
      this.addressForm = {
        line1: '',
        city: '',
        district: '',
        reference: '',
      };
    }

    // Si cambia la opción de dirección, limpiar el formulario
    if (this.deliveryType === 'DELIVERY') {
      if (this.addressOption === 'saved') {
        // Si hay una dirección seleccionada, cargar sus datos
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
        // Limpiar formulario para nueva dirección
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

  /**
   * Recalcula los totales según el tipo de delivery
   */
  private recalcularTotales(): void {
    const baseTotals = this.cartService.getTotals();

    if (this.deliveryType === 'PICKUP') {
      this.totals = {
        ...baseTotals,
        deliveryFee: 0,
        total: baseTotals.subtotal - this.promoDiscount,
      };
    } else {
      this.totals = {
        ...baseTotals,
        deliveryFee: this.baseDeliveryFee,
        total: baseTotals.subtotal + this.baseDeliveryFee - this.promoDiscount,
      };
    }
  }

  /**
   * Valida los datos de dirección para delivery
   */
  private validateAddressForm(): string | null {
    if (this.deliveryType === 'PICKUP') {
      return null; // No se requiere dirección para pickup
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

  /**
   * Obtiene la URL optimizada de una imagen
   */
  getOptimizedImageUrl(imageUrl: string | undefined): string {
    return this.imageOptimizer.optimizeImageUrl(imageUrl, 'low');
  }

  /**
   * Aplica un código promocional al carrito
   */
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

    // Calcular subtotal antes del delivery
    const subtotal = this.cartService.getTotals().subtotal;

    // Obtener userId para validación completa
    const userId = this.authService.obtenerUsuarioId();

    // Obtener items del carrito para calcular descuento solo sobre items aplicables
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
            this.guardarPromocionEnStorage(); // Guardar en localStorage
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

  /**
   * Quita el código promocional aplicado
   */
  quitarPromocion(): void {
    this.promoCode = '';
    this.promoApplied = false;
    this.promoDiscount = 0;
    this.promoSuccess = '';
    this.promoError = '';
    this.limpiarPromocionDeStorage(); // Limpiar de localStorage
    this.recalcularTotales();
  }

  /**
   * Guarda el código promocional en localStorage
   */
  private guardarPromocionEnStorage(): void {
    const promoData = {
      code: this.promoCode,
      discount: this.promoDiscount,
      applied: this.promoApplied,
    };
    localStorage.setItem('cart_promo', JSON.stringify(promoData));
  }

  /**
   * Carga el código promocional desde localStorage
   */
  private cargarPromocionGuardada(): void {
    const promoData = localStorage.getItem('cart_promo');
    if (promoData) {
      try {
        const promo = JSON.parse(promoData);
        this.promoCode = promo.code || '';
        this.promoApplied = promo.applied || false;
        this.promoDiscount = promo.discount || 0;

        // Revalidar la promoción para asegurar que sigue siendo válida
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

  /**
   * Limpia el código promocional de localStorage
   */
  private limpiarPromocionDeStorage(): void {
    localStorage.removeItem('cart_promo');
  }
}
