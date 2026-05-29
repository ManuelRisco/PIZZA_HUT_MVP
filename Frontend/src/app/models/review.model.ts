export class Review {
  id?: number;
  userId: number;
  pizzaId: number;
  rating: number;
  comment?: string;
  createdAt?: Date;

  constructor(
    userId: number,
    pizzaId: number,
    rating: number,
    comment?: string,
    createdAt?: Date,
    id?: number
  ) {
    this.id = id;
    this.userId = userId;
    this.pizzaId = pizzaId;
    this.rating = rating;
    this.comment = comment;
    this.createdAt = createdAt;
  }

  static fromDTO(dto: any): Review {
    return new Review(
      dto.userId,
      dto.pizzaId,
      dto.rating,
      dto.comment,
      dto.createdAt ? new Date(dto.createdAt) : undefined,
      dto.id
    );
  }

  toDTO(): any {
    return {
      id: this.id,
      userId: this.userId,
      pizzaId: this.pizzaId,
      rating: this.rating,
      comment: this.comment,
      createdAt: this.createdAt?.toISOString()
    };
  }
}
