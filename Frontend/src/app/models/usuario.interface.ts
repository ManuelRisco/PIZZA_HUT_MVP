// DTOs para el usuario - coinciden con el backend
export interface UsuarioDTO {
  id?: number;
  email: string;
  name: string;
  phone?: string;
  dni?: string | null;
  ageVerified?: boolean;
  role: 'CUSTOMER' | 'ADMIN';
  createdAt?: string | null;
  deletedAt?: string | null;
  lastLogin?: string | null;
  loginAttempts?: number;
  lockedUntil?: string | null;
  googleLinked?: boolean;
  authProvider?: 'GOOGLE' | 'LOCAL' | string;
  passwordSet?: boolean;
  active?: boolean;
}

export interface UsuarioCreateDTO {
  email: string;
  password: string;
  name: string;
  phone?: string;
  dni?: string | null;
  ageVerified?: boolean;
  role?: 'CUSTOMER' | 'ADMIN';
}

export interface LoginDTO {
  email: string;
  password: string;
}

export interface AuthResponseDTO {
  token: string;
  usuario: UsuarioDTO;
  message: string;
}

// Alias para compatibilidad con codigo existente
export interface Usuario extends UsuarioDTO {}
