import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { AddToCartPayload, CartItem, CartTotals } from '../models/cart-item.model';

const STORAGE_KEY = 'pizza-hut-cart';

@Injectable({ providedIn: 'root' })
export class CartService {
  private readonly itemsSubject = new BehaviorSubject<CartItem[]>(this.loadItemsFromStorage());
  readonly items$ = this.itemsSubject.asObservable();

  getItemsSnapshot(): CartItem[] {
    return [...this.itemsSubject.value];
  }

  /**
   * Calcula los totales del carrito.
   * El deliveryFee es solo para cálculo base, el componente carrito lo ajusta según el tipo de delivery.
   */
  getTotals(): CartTotals {
    const items = this.itemsSubject.value;
    const subtotal = items.reduce((acc, item) => acc + item.unitPrice * item.quantity, 0);
    const deliveryFee = subtotal > 0 ? 5.0 : 0; // Fee base, ajustable según tipo de entrega
    return {
      itemsCount: items.reduce((count, item) => count + item.quantity, 0),
      subtotal,
      deliveryFee,
      total: subtotal + deliveryFee
    };
  }

  /**
   * Agrega un item al carrito.
   * El precio ya debe venir calculado desde el componente (incluyendo tamaño y extras).
   */
  addItem(payload: AddToCartPayload): CartItem {
    const quantity = payload.quantity && payload.quantity > 0 ? payload.quantity : 1;
    const unitPrice = payload.pizza.price;
    const itemId = this.buildItemId(payload.pizza, payload.size, payload.extras);

    const items = this.getItemsSnapshot();
    const existingIndex = items.findIndex(item => item.id === itemId);

    if (existingIndex >= 0) {
      const updated = { ...items[existingIndex] };
      const nextQty = updated.quantity + quantity;
      updated.quantity = payload.maxStock !== undefined ? Math.min(nextQty, payload.maxStock) : nextQty;
      if (payload.maxStock !== undefined) {
        updated.maxStock = payload.maxStock;
      }
      if (payload.pizza.imageUrl) {
        updated.imageUrl = payload.pizza.imageUrl;
      }
      items.splice(existingIndex, 1, updated);
      this.persist(items);
      return updated;
    }

    const newItem: CartItem = {
      id: itemId,
      pizzaId: payload.pizza.id,
      type: 'pizza',
      name: payload.pizza.name,
      size: payload.size,
      sizeId: payload.sizeId,
      quantity,
      unitPrice,
      imageUrl: payload.pizza.imageUrl,
      extras: payload.extras || [],
      maxStock: payload.maxStock
    };

    items.push(newItem);
    this.persist(items);
    return newItem;
  }

  /**
   * Agrega un extra (bebida, postre, etc.) al carrito conservando su imagen local y stock máximo
   */
  addExtra(
    extraId: number,
    name: string,
    price: number,
    category: string,
    quantity: number = 1,
    imageUrl?: string,
    maxStock?: number
  ): CartItem {
    const itemId = `extra-${extraId}`;
    const items = this.getItemsSnapshot();
    const existingIndex = items.findIndex(item => item.id === itemId);

    if (existingIndex >= 0) {
      const updated = { ...items[existingIndex] };
      const nextQty = updated.quantity + quantity;
      updated.quantity = maxStock !== undefined ? Math.min(nextQty, maxStock) : nextQty;
      if (imageUrl) {
        updated.imageUrl = imageUrl;
      }
      if (maxStock !== undefined) {
        updated.maxStock = maxStock;
      }
      items.splice(existingIndex, 1, updated);
      this.persist(items);
      return updated;
    }

    const newItem: CartItem = {
      id: itemId,
      extraId: extraId,
      type: 'extra',
      name: name,
      size: '-',
      quantity,
      unitPrice: price,
      category: category,
      imageUrl: imageUrl || '/combo1.webp',
      maxStock: maxStock
    };

    items.push(newItem);
    this.persist(items);
    return newItem;
  }

  updateQuantity(itemId: string, quantity: number): void {
    const parsedQuantity = Number(quantity);
    if (Number.isNaN(parsedQuantity)) {
      return;
    }

    const items = this.getItemsSnapshot();
    const index = items.findIndex(item => item.id === itemId);
    if (index === -1) {
      return;
    }

    if (parsedQuantity <= 0) {
      items.splice(index, 1);
    } else {
      const maxStock = items[index].maxStock;
      const clamped = maxStock !== undefined && maxStock > 0 ? Math.min(parsedQuantity, maxStock) : parsedQuantity;
      items[index] = { ...items[index], quantity: clamped };
    }

    this.persist(items);
  }

  removeItem(itemId: string): void {
    const items = this.getItemsSnapshot().filter(item => item.id !== itemId);
    this.persist(items);
  }

  clearCart(): void {
    this.persist([]);
  }

  getItemCount(): number {
    return this.getTotals().itemsCount;
  }

  private buildItemId(
    pizza: { id?: number; name: string },
    size: string,
    extras?: string[]
  ): string {
    const baseId = pizza.id != null ? pizza.id.toString() : pizza.name.trim().toLowerCase().replace(/\s+/g, '-');
    const sizeKey = size.toLowerCase();
    const extrasKey = extras && extras.length > 0 ? extras.toSorted((a, b) => a.localeCompare(b)).join(',') : 'none';
    return `${baseId}-${sizeKey}-${extrasKey}`;
  }

  private loadItemsFromStorage(): CartItem[] {
    if (typeof window === 'undefined') {
      return [];
    }

    try {
      const raw = window.localStorage.getItem(STORAGE_KEY);
      if (!raw) {
        return [];
      }
      const parsed = JSON.parse(raw);
      if (!Array.isArray(parsed)) {
        return [];
      }
      return parsed.filter(item => item?.id && item?.name);
    } catch {
      return [];
    }
  }

  private persist(items: CartItem[]): void {
    this.itemsSubject.next(items);
    if (typeof window === 'undefined') {
      return;
    }

    try {
      window.localStorage.setItem(STORAGE_KEY, JSON.stringify(items));
    } catch {
    }
  }
}
