import { CartItem, CartTotals } from './cart-item.model';
import { OrderDTO, OrderItemDTO, PaymentDTO } from './admin.interface';

export interface MercadoPagoConfigDTO {
  configured: boolean;
  publicKey: string;
  sandboxMode: boolean;
  currency: string;
  locale: string;
  supportedGateways: string[];
}

export interface CheckoutSizeConfig {
  sizeId?: number;
  extra?: number;
}

export interface CheckoutPayload {
  addressId?: number;
  deliveryType?: 'PICKUP' | 'DELIVERY';
  address?: {
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
  promoCode?: string;
  discount?: number;
  wantsBoleta?: boolean;
  clienteDni?: string;
  clienteNombre?: string;
  ageVerified?: boolean;
  payment?: {
    paymentMethodId: number;
    amount?: number;
    status?: PaymentDTO['status'];
    transactionId?: string;
    mercadoPagoToken?: string;
    mercadoPagoPaymentMethodId?: string;
    mercadoPagoIssuerId?: string;
    mercadoPagoInstallments?: number;
    mercadoPagoPayerEmail?: string;
    mercadoPagoPayerDocType?: string;
    mercadoPagoPayerDocNumber?: string;
    mercadoPagoPayerPhone?: string;
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
