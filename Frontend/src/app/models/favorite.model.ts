export class Favorite {
  userId: number;
  pizzaId: number;

  constructor(userId: number, pizzaId: number) {
    this.userId = userId;
    this.pizzaId = pizzaId;
  }

  static fromDTO(dto: any): Favorite {
    return new Favorite(dto.userId, dto.pizzaId);
  }

  toDTO(): any {
    return {
      userId: this.userId,
      pizzaId: this.pizzaId
    };
  }
}
