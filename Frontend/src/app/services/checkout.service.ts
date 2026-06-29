import { Injectable } from '@angular/core';
import { of, throwError, Observable } from 'rxjs';
import { catchError, map, tap } from 'rxjs/operators';
import { CheckoutPayload, CheckoutResult } from '../models/checkout.model';
import { CartService } from './cart.service';
import { Order } from './order.service';
import { AuthService } from './auth.service';

@Injectable({ providedIn: 'root' })
export class CheckoutService {
  constructor(
    private cartService: CartService,
    private orderService: Order,
    private authService: AuthService
  ) {}

  checkout(payload: CheckoutPayload = {}): Observable<CheckoutResult> {
    const user = this.authService.getCurrentUser();
    if (!user?.id) {
      return throwError(() => new Error('Debes iniciar sesi\u00f3n para confirmar tu pedido.'));
    }

    const itemsSnapshot = this.cartService.getItemsSnapshot();
    if (itemsSnapshot.length === 0) {
      return throwError(() => new Error('Tu carrito est\u00e1 vac\u00edo.'));
    }

    const totals = this.cartService.getTotals();
    const deliveryType = payload.deliveryType || 'DELIVERY';

    const deliveryFee = deliveryType === 'PICKUP' ? 0 : Number(totals.deliveryFee.toFixed(2));
    const finalTotal = Number((totals.subtotal + deliveryFee).toFixed(2));
    const discount = payload.promoCode && payload.discount ? payload.discount : 0;
    const finalTotalWithDiscount = finalTotal - discount;

    const unifiedPayload = {
      order: {
        userId: user.id,
        addressId: payload.addressId ?? null,
        status: payload.status ?? 'PENDING',
        deliveryType: deliveryType,
        paymentMethodId: payload.payment?.paymentMethodId ?? payload.paymentMethodId ?? null,
        subtotal: Number(totals.subtotal.toFixed(2)),
        deliveryFee: deliveryFee,
        discount: discount,
        total: Number(finalTotalWithDiscount.toFixed(2)),
        promoCode: payload.promoCode ?? null,
        notes: payload.notes ?? null,
        estimatedDelivery: payload.estimatedDelivery ?? null
      },
      address: payload.address && deliveryType === 'DELIVERY' ? {
        line1: payload.address.line1,
        city: payload.address.city,
        district: payload.address.district,
        reference: payload.address.reference,
        isDefault: false
      } : null,
      items: itemsSnapshot.map(item => {
        const isExtra = item.type === 'extra';
        return {
          itemType: isExtra ? 'EXTRA' : 'PIZZA',
          pizzaId: isExtra ? null : item.pizzaId,
          extraId: isExtra ? item.extraId : null,
          sizeId: isExtra ? null : item.sizeId,
          quantity: item.quantity,
          unitPrice: item.unitPrice,
          lineTotal: Number((item.unitPrice * item.quantity).toFixed(2)),
          extraIngredientIds: item.extras ? item.extras.map(e => parseInt(e, 10)).filter(e => !isNaN(e)) : []
        };
      }),
      payment: (payload.payment?.paymentMethodId || payload.paymentMethodId) ? {
        paymentMethodId: payload.payment?.paymentMethodId ?? payload.paymentMethodId,
        amount: payload.payment?.amount ?? finalTotalWithDiscount,
        status: payload.payment?.status ?? 'PENDING',
        transactionId: payload.payment?.transactionId ?? null
      } : null
    };

    return this.orderService.checkout(unifiedPayload).pipe(
      tap(() => this.cartService.clearCart()),
      map(orderResponse => {
        const updatedTotals = { ...totals, deliveryFee, total: finalTotal };
        return { 
          order: orderResponse, 
          items: [], // El backend ahora devuelve todo en OrderDTO, asi que esto es solo para no romper el modelo
          payment: unifiedPayload.payment ? { ...unifiedPayload.payment, orderId: orderResponse.id } : undefined,
          totals: updatedTotals 
        } as any;
      }),
      catchError((error) => throwError(() => this.normalizeError(error)))
    );
  }

  private normalizeError(error: any): Error {
    if (error instanceof Error) {
      return error;
    }

    const message =
      error?.error?.message ||
      error?.message ||
      'No se pudo completar el pedido. Int\u00e9ntalo nuevamente.';

    return new Error(message);
  }
}
