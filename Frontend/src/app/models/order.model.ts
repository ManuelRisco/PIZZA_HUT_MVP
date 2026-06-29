export type OrderStatus = 'PENDING' | 'CONFIRMED' | 'IN_DELIVERY' | 'DELIVERED' | 'CANCELLED';
export type PaymentMethod = 'CASH' | 'CARD' | 'WALLET';

export class Order {
  id?: number;
  userId: number;
  addressId?: number;
  status: OrderStatus;
  paymentMethod: PaymentMethod;
  subtotal: number;
  deliveryFee: number;
  discount: number;
  total: number;
  promoCode?: string;
  notes?: string;
  estimatedDelivery?: Date;
  createdAt?: Date;

  constructor(
    userId: number,
    status: OrderStatus,
    paymentMethod: PaymentMethod,
    subtotal: number,
    deliveryFee: number,
    total: number,
    discount: number = 0,
    addressId?: number,
    promoCode?: string,
    notes?: string,
    estimatedDelivery?: Date,
    createdAt?: Date,
    id?: number
  ) {
    this.id = id;
    this.userId = userId;
    this.addressId = addressId;
    this.status = status;
    this.paymentMethod = paymentMethod;
    this.subtotal = subtotal;
    this.deliveryFee = deliveryFee;
    this.discount = discount;
    this.total = total;
    this.promoCode = promoCode;
    this.notes = notes;
    this.estimatedDelivery = estimatedDelivery;
    this.createdAt = createdAt;
  }

  static fromDTO(dto: any): Order {
    return new Order(
      dto.userId,
      dto.status,
      dto.paymentMethod,
      dto.subtotal,
      dto.deliveryFee,
      dto.total,
      dto.discount || 0,
      dto.addressId,
      dto.promoCode,
      dto.notes,
      dto.estimatedDelivery ? new Date(dto.estimatedDelivery) : undefined,
      dto.createdAt ? new Date(dto.createdAt) : undefined,
      dto.id
    );
  }

  toDTO(): any {
    return {
      id: this.id,
      userId: this.userId,
      addressId: this.addressId,
      status: this.status,
      paymentMethod: this.paymentMethod,
      subtotal: this.subtotal,
      deliveryFee: this.deliveryFee,
      discount: this.discount,
      total: this.total,
      promoCode: this.promoCode,
      notes: this.notes,
      estimatedDelivery: this.estimatedDelivery?.toISOString(),
      createdAt: this.createdAt?.toISOString()
    };
  }
}
