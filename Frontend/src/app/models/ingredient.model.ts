export class Ingredient {
  id?: number;
  name: string;
  extraCost: number;
  isAvailable: boolean;

  constructor(
    name: string,
    extraCost: number,
    isAvailable: boolean = true,
    id?: number
  ) {
    this.id = id;
    this.name = name;
    this.extraCost = extraCost;
    this.isAvailable = isAvailable;
  }

  static fromDTO(dto: any): Ingredient {
    return new Ingredient(
      dto.name,
      dto.extraCost,
      dto.isAvailable ?? true,
      dto.id
    );
  }

  toDTO(): any {
    return {
      id: this.id,
      name: this.name,
      extraCost: this.extraCost,
      isAvailable: this.isAvailable
    };
  }
}
