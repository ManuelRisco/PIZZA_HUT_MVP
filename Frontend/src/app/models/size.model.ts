export class Size {
  id?: number;
  name: string;
  extraCost: number;
  description?: string;
  displayOrder?: number;

  constructor(
    name: string,
    extraCost: number,
    description?: string,
    displayOrder?: number,
    id?: number
  ) {
    this.id = id;
    this.name = name;
    this.extraCost = extraCost;
    this.description = description;
    this.displayOrder = displayOrder;
  }

  static fromDTO(dto: any): Size {
    return new Size(
      dto.name,
      dto.extraCost,
      dto.description,
      dto.displayOrder,
      dto.id
    );
  }

  toDTO(): any {
    return {
      id: this.id,
      name: this.name,
      extraCost: this.extraCost,
      description: this.description,
      displayOrder: this.displayOrder
    };
  }
}
