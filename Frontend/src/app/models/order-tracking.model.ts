export type TrackingStatus = 'PENDING' | 'CONFIRMED' | 'IN_DELIVERY' | 'DELIVERED' | 'CANCELLED';

export class OrderTracking {
  id?: number;
  orderId: number;
  status: TrackingStatus;
  description?: string;
  createdAt?: Date;

  constructor(
    orderId: number,
    status: TrackingStatus,
    description?: string,
    createdAt?: Date,
    id?: number
  ) {
    this.id = id;
    this.orderId = orderId;
    this.status = status;
    this.description = description;
    this.createdAt = createdAt;
  }

  static fromDTO(dto: any): OrderTracking {
    return new OrderTracking(
      dto.orderId,
      dto.status,
      dto.description,
      dto.createdAt ? new Date(dto.createdAt) : undefined,
      dto.id
    );
  }

  toDTO(): any {
    return {
      id: this.id,
      orderId: this.orderId,
      status: this.status,
      description: this.description,
      createdAt: this.createdAt?.toISOString()
    };
  }
}
