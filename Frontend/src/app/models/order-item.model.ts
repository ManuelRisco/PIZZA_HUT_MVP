export class OrderItem {
  id?: number;
  orderId: number;
  pizzaId: number;
  sizeId?: number;
  quantity: number;
  unitPrice: number;
  sizeExtra: number;
  lineTotal: number;

  constructor(
    orderId: number,
    pizzaId: number,
    quantity: number,
    unitPrice: number,
    sizeExtra: number,
    lineTotal: number,
    sizeId?: number,
    id?: number
  ) {
    this.id = id;
    this.orderId = orderId;
    this.pizzaId = pizzaId;
    this.sizeId = sizeId;
    this.quantity = quantity;
    this.unitPrice = unitPrice;
    this.sizeExtra = sizeExtra;
    this.lineTotal = lineTotal;
  }

  static fromDTO(dto: any): OrderItem {
    return new OrderItem(
      dto.orderId,
      dto.pizzaId,
      dto.quantity,
      dto.unitPrice,
      dto.sizeExtra,
      dto.lineTotal,
      dto.sizeId,
      dto.id
    );
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
