export interface PaymentMethodDTO {
  id?: number;
  name: string;
  description?: string;
  isActive: boolean;
  displayOrder: number;
  createdAt?: string;
  updatedAt?: string;
  inUse?: boolean;
}
