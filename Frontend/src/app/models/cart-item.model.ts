import { PizzaDTO } from './pizza.interface';

export interface CartItem {
  id: string;
  pizzaId?: number;
  extraId?: number;
  type: 'pizza' | 'extra';
  name: string;
  size: string;
  sizeId?: number;
  quantity: number;
  unitPrice: number;
  imageUrl?: string;
  notes?: string;
  extras?: string[];
  category?: string;
  maxStock?: number;
}

export interface CartTotals {
  itemsCount: number;
  subtotal: number;
  deliveryFee: number;
  total: number;
}

export interface AddToCartPayload {
  pizza: PizzaDTO;
  size: string;
  sizeId?: number;
  quantity?: number;
  extras?: string[];
  maxStock?: number;
}
