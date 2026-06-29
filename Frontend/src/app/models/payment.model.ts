export type PaymentStatus = 'PENDING' | 'PAID' | 'FAILED' | 'REFUNDED';
export type PaymentMethodType = 'CASH' | 'CARD' | 'WALLET';

export class Payment {
  id?: number;
  orderId: number;
  amount: number;
  paymentMethod: PaymentMethodType;
  status: PaymentStatus;
  transactionId?: string;
  createdAt?: Date;

  constructor(
    orderId: number,
    amount: number,
    paymentMethod: PaymentMethodType,
    status: PaymentStatus,
    transactionId?: string,
    createdAt?: Date,
    id?: number
  ) {
    this.id = id;
    this.orderId = orderId;
    this.amount = amount;
    this.paymentMethod = paymentMethod;
    this.status = status;
    this.transactionId = transactionId;
    this.createdAt = createdAt;
  }

  static fromDTO(dto: any): Payment {
    return new Payment(
      dto.orderId,
      dto.amount,
      dto.paymentMethod,
      dto.status,
      dto.transactionId,
      dto.createdAt ? new Date(dto.createdAt) : undefined,
      dto.id
    );
  }

  toDTO(): any {
    return {
      id: this.id,
      orderId: this.orderId,
      amount: this.amount,
      paymentMethod: this.paymentMethod,
      status: this.status,
      transactionId: this.transactionId,
      createdAt: this.createdAt?.toISOString()
    };
  }
}
