import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { AddToCartPayload, CartItem, CartTotals } from '../models/cart-item.model';

const STORAGE_KEY = 'pizza-hut-cart';

@Injectable({ providedIn: 'root' })
export class CartService {
  private readonly itemsSubject= new BehaviorSubject<CartItem[]>(this.loadItemsFromStorage());
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
    
    // Si el precio ya viene calculado (con tamaño y extras), usarlo directamente
    const unitPrice = payload.pizza.price;
    
    // El ID ahora incluye extras para distinguir pizzas iguales con diferentes ingredientes
    const itemId = this.buildItemId(payload.pizza, payload.size, payload.extras);

    const items = this.getItemsSnapshot();
    const existingIndex = items.findIndex(item => item.id === itemId);

    if (existingIndex >= 0) {
      const updated = { ...items[existingIndex] };
      updated.quantity += quantity;
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
      sizeId: payload.sizeId, // Guardar el ID del tamaño
      quantity,
      unitPrice,
      imageUrl: payload.pizza.imageUrl,
      extras: payload.extras || []
    };

    items.push(newItem);
    this.persist(items);
    return newItem;
  }

  /**
   * Agrega un extra (bebida, postre, etc.) al carrito
   */
  addExtra(extraId: number, name: string, price: number, category: string, quantity: number = 1): CartItem {
    const itemId = `extra-${extraId}`;
    const items = this.getItemsSnapshot();
    const existingIndex = items.findIndex(item => item.id === itemId);

    if (existingIndex >= 0) {
      const updated = { ...items[existingIndex] };
      updated.quantity += quantity;
      items.splice(existingIndex, 1, updated);
      this.persist(items);
      return updated;
    }

    const newItem: CartItem = {
      id: itemId,
      extraId: extraId,
      type: 'extra',
      name: name,
      size: '-', // Los extras no tienen tamaño
      quantity,
      unitPrice: price,
      category: category,
      imageUrl: '/combo1.webp' // Imagen por defecto para extras
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
      items[index] = { ...items[index], quantity: parsedQuantity };
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

  /**
   * Construye un ID único para el item del carrito.
   * Incluye extras para diferenciar la misma pizza con diferentes ingredientes.
   */
  private buildItemId(
    pizza: { id?: number; name: string }, 
    size: string, 
    extras?: string[]
  ): string {
    const baseId = pizza.id != null ? pizza.id.toString() : pizza.name.trim().toLowerCase().replace(/\s+/g, '-');
    const sizeKey = size.toLowerCase();
    const extrasKey = (extras && extras.length > 0) ? extras.toSorted((a, b) => a.localeCompare(b)).join(',') : 'none';
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
    } catch (error) {
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
    } catch (error) {
    }
  }
}

