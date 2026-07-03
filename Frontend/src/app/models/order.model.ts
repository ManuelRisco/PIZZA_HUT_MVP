export type OrderStatus = 'PENDING' | 'CONFIRMED' | 'IN_DELIVERY' | 'DELIVERED' | 'CANCELLED';
export type PaymentMethod = 'CASH' | 'CARD' | 'WALLET';

export class Order {
  id?: number;
  userId!: number;
  addressId?: number;
  status!: OrderStatus;
  paymentMethod!: PaymentMethod;
  subtotal!: number;
  deliveryFee!: number;
  discount!: number;
  total!: number;
  promoCode?: string;
  notes?: string;
  estimatedDelivery?: Date;
  createdAt?: Date;

  constructor(data: Partial<Order>) {
    Object.assign(this, data);
  }

  static fromDTO(dto: any): Order {
    return new Order({
      userId: dto.userId,
      status: dto.status,
      paymentMethod: dto.paymentMethod,
      subtotal: dto.subtotal,
      deliveryFee: dto.deliveryFee,
      total: dto.total,
      discount: dto.discount || 0,
      addressId: dto.addressId,
      promoCode: dto.promoCode,
      notes: dto.notes,
      estimatedDelivery: dto.estimatedDelivery ? new Date(dto.estimatedDelivery) : undefined,
      createdAt: dto.createdAt ? new Date(dto.createdAt) : undefined,
      id: dto.id,
    });
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
      createdAt: this.createdAt?.toISOString(),
    };
  }
}
