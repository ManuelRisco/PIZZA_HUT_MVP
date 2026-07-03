export class OrderItem {
  id?: number;
  orderId!: number;
  pizzaId!: number;
  sizeId?: number;
  quantity!: number;
  unitPrice!: number;
  sizeExtra!: number;
  lineTotal!: number;

  constructor(data: Partial<OrderItem>) {
    Object.assign(this, data);
  }

  static fromDTO(dto: any): OrderItem {
    return new OrderItem({
      orderId: dto.orderId,
      pizzaId: dto.pizzaId,
      quantity: dto.quantity,
      unitPrice: dto.unitPrice,
      sizeExtra: dto.sizeExtra,
      lineTotal: dto.lineTotal,
      sizeId: dto.sizeId,
      id: dto.id
    });
  }

  toDTO(): any {
    return {
      id: this.id,
      orderId: this.orderId,
      pizzaId: this.pizzaId,
      sizeId: this.sizeId,
      quantity: this.quantity,
      unitPrice: this.unitPrice,
      sizeExtra: this.sizeExtra,
      lineTotal: this.lineTotal
    };
  }
}
