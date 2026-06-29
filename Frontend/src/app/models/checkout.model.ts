import { CartItem, CartTotals } from './cart-item.model';
import { OrderDTO, OrderItemDTO, PaymentDTO } from './admin.interface';

export interface CheckoutSizeConfig {
  sizeId?: number;
  extra?: number;
}

export interface CheckoutPayload {
  addressId?: number;
  deliveryType?: 'PICKUP' | 'DELIVERY'; // Tipo de entrega
  address?: {  // Datos de dirección para crear nueva dirección
    line1: string;
    city: string;
    district: string;
    reference?: string;
  };
  paymentMethodId?: number;
  notes?: string;
  estimatedDelivery?: string;
  status?: OrderDTO['status'];
  sizeMapping?: Record<string, CheckoutSizeConfig>;
  promoCode?: string; // Código promocional aplicado
  discount?: number; // Descuento aplicado
  payment?: {
    paymentMethodId: number;
    amount?: number;
    status?: PaymentDTO['status'];
    transactionId?: string;
  };
}

export interface CheckoutResult {
  order: OrderDTO;
  items: OrderItemDTO[];
  payment?: PaymentDTO;
  totals: CartTotals;
}

export interface CheckoutContext {
  items: CartItem[];
  totals: CartTotals;
}
