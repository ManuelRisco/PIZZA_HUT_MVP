export class Address {
  id?: number;
  userId: number;
  line1: string;
  city: string;
  district?: string;
  reference?: string;
  isDefault?: boolean;

  constructor(
    userId: number,
    line1: string,
    city: string,
    district?: string,
    reference?: string,
    isDefault?: boolean,
    id?: number
  ) {
    this.id = id;
    this.userId = userId;
    this.line1 = line1;
    this.city = city;
    this.district = district;
    this.reference = reference;
    this.isDefault = isDefault;
  }

  static fromDTO(dto: any): Address {
    return new Address(
      dto.userId,
      dto.line1,
      dto.city,
      dto.district,
      dto.reference,
      dto.isDefault,
      dto.id
    );
  }

  toDTO(): any {
    return {
      id: this.id,
      userId: this.userId,
      line1: this.line1,
      city: this.city,
      district: this.district,
      reference: this.reference,
      isDefault: this.isDefault
    };
  }
}
