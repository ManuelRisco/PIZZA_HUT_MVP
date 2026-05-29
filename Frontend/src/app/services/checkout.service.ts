import { Injectable } from '@angular/core';
import { from, of, throwError, Observable, forkJoin } from 'rxjs';
import { catchError, concatMap, map, switchMap, tap, toArray } from 'rxjs/operators';
import { CheckoutPayload, CheckoutResult } from '../models/checkout.model';
import { CartService } from './cart.service';
import { Order } from './order.service';
import { Payment } from './payment.service';
import { AuthService } from './auth.service';
import { AddressService } from './address.service';
import { OrderItemExtraService } from './order-item-extra.service';
import { IngredientService } from './ingredient.service';
import { CartItem } from '../models/cart-item.model';
import { OrderItemDTO, PaymentDTO } from '../models/admin.interface';

@Injectable({ providedIn: 'root' })
export class CheckoutService {
  constructor(
    private cartService: CartService,
    private orderService: Order,
    private paymentService: Payment,
    private authService: AuthService,
    private addressService: AddressService,
    private orderItemExtraService: OrderItemExtraService,
    private ingredientService: IngredientService
  ) {}

  checkout(payload: CheckoutPayload = {}): Observable<CheckoutResult> {
    const user = this.authService.getCurrentUser();
    if (!user?.id) {
      return throwError(() => new Error('Debes iniciar sesión para confirmar tu pedido.'));
    }

    const itemsSnapshot = this.cartService.getItemsSnapshot();
    if (itemsSnapshot.length === 0) {
      return throwError(() => new Error('Tu carrito está vacío.'));
    }

    const totals = this.cartService.getTotals();
    const deliveryType = payload.deliveryType || 'DELIVERY';

    // Calcular delivery fee según el tipo
    const deliveryFee = deliveryType === 'PICKUP' ? 0 : Number(totals.deliveryFee.toFixed(2));
    const finalTotal = Number((totals.subtotal + deliveryFee).toFixed(2));

    // Si es DELIVERY y se proporcionó dirección, crear la dirección primero
    const addressObservable: Observable<{ id?: number } | null> = (deliveryType === 'DELIVERY' && payload.address) 
      ? this.addressService.crear({
          userId: user.id,
          line1: payload.address.line1,
          city: payload.address.city,
          district: payload.address.district,
          reference: payload.address.reference,
          isDefault: false
        })
      : of(null);

    return addressObservable.pipe(
      switchMap(createdAddress => {
        // Calcular el descuento si hay promoCode
        const discount = payload.promoCode && payload.discount ? payload.discount : 0;
        const finalTotalWithDiscount = finalTotal - discount;
        
        const orderPayload = {
          userId: user.id,
          addressId: createdAddress?.id ?? payload.addressId ?? null,
          status: payload.status ?? 'PENDING',
          deliveryType: deliveryType, // Agregar tipo de delivery
          paymentMethodId: payload.payment?.paymentMethodId ?? payload.paymentMethodId ?? null,
          subtotal: Number(totals.subtotal.toFixed(2)),
          deliveryFee: deliveryFee,
          discount: discount,
          total: Number(finalTotalWithDiscount.toFixed(2)),
          promoCode: payload.promoCode ?? null,
          notes: payload.notes ?? null,
          estimatedDelivery: payload.estimatedDelivery ?? null
        } as any;

        return this.orderService.crear(orderPayload).pipe(
          switchMap(order => {
            if (!order?.id) {
              return throwError(() => new Error('No se pudo obtener el identificador del pedido recién creado.'));
            }

            return this.persistOrderItems(order.id!, itemsSnapshot, payload).pipe(
              switchMap(createdItems => {
                const shouldCreatePayment = !!payload.payment && !!payload.payment.paymentMethodId;
                if (!shouldCreatePayment) {
                  return of({ order, items: createdItems, payment: undefined });
                }

                const paymentPayload: Partial<PaymentDTO> = {
                  orderId: order.id!,
                  amount: payload.payment?.amount ?? order.total ?? finalTotal,
                  paymentMethodId: payload.payment!.paymentMethodId,
                  status: payload.payment?.status ?? 'PENDING',
                  transactionId: payload.payment?.transactionId ?? null
                };

                return this.paymentService.crear(paymentPayload as PaymentDTO).pipe(
                  map(payment => ({ order, items: createdItems, payment }))
                );
              })
            );
          })
        );
      }),
      tap(() => this.cartService.clearCart()),
      map(result => {
        const updatedTotals = { ...totals, deliveryFee, total: finalTotal };
        return { ...result, totals: updatedTotals };
      }),
      catchError((error) => throwError(() => this.normalizeError(error)))
    );
  }

  private persistOrderItems(orderId: number, items: CartItem[], payload: CheckoutPayload) {
    return from(items).pipe(
      concatMap(item => {
        const lineTotal = Number((item.unitPrice * item.quantity).toFixed(2));

        // Manejar extras (bebidas, postres, etc.)
        if (item.type === 'extra') {
          if (!item.extraId) {
            console.warn(`Extra "${item.name}" no tiene extraId válido, se omitirá`);
            return of(null);
          }

          const extraItemPayload: any = {
            orderId,
            extraId: item.extraId,
            itemType: 'EXTRA',
            quantity: item.quantity,
            unitPrice: item.unitPrice,
            sizeExtra: 0,
            lineTotal
          };

          return this.orderService.crearItem(extraItemPayload as OrderItemDTO);
        }

        // Manejar pizzas
        if (!item.pizzaId) {
          return throwError(() => new Error(`La pizza "${item.name}" no tiene identificador válido.`));
        }

        const pizzaItemPayload: Partial<OrderItemDTO> = {
          orderId,
          pizzaId: item.pizzaId,
          sizeId: item.sizeId,
          quantity: item.quantity,
          unitPrice: item.unitPrice,
          sizeExtra: 0,
          lineTotal
        };

        return this.orderService.crearItem(pizzaItemPayload as OrderItemDTO).pipe(
          switchMap(createdItem => {
            // Si la pizza tiene ingredientes extras, guardarlos en order_item_extras
            if (item.extras && item.extras.length > 0 && createdItem.id) {
              return this.persistOrderItemExtras(createdItem.id, item.extras).pipe(
                map(() => createdItem)
              );
            }
            return of(createdItem);
          })
        );
      }),
      toArray(),
      map(items => items.filter(item => item !== null)) // Filtrar los nulls
    );
  }

  private persistOrderItemExtras(orderItemId: number, extraIds: string[]): Observable<any> {
    // Convertir IDs de string a number
    const ingredientIds = extraIds.map(id => parseInt(id, 10)).filter(id => !isNaN(id));
    
    if (ingredientIds.length === 0) {
      return of(null);
    }

    // Obtener información de cada ingrediente y crear los extras
    const extraCreations = ingredientIds.map(ingredientId => 
      this.ingredientService.obtenerPorId(ingredientId).pipe(
        switchMap(ingredient => {
          const extraPayload = {
            orderItemId: orderItemId,
            ingredientId: ingredient.id!,
            ingredientName: ingredient.name,
            extraCost: ingredient.extraCost || 0
          };
          return this.orderItemExtraService.crear(extraPayload);
        }),
        catchError(error => {
          console.error(`Error al guardar extra para ingrediente ${ingredientId}:`, error);
          return of(null); // Continuar aunque falle un extra
        })
      )
    );

    return forkJoin(extraCreations);
  }

  private normalizeError(error: any): Error {
    if (error instanceof Error) {
      return error;
    }

    const message =
      error?.error?.message ||
      error?.message ||
      'No se pudo completar el pedido. Inténtalo nuevamente.';

    return new Error(message);
  }
}
