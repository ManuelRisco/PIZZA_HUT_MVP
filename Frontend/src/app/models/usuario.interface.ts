// DTOs para el usuario - coinciden con el backend
export interface UsuarioDTO {
  id?: number;
  email: string;
  name: string;
  phone?: string;
  role: 'CUSTOMER' | 'ADMIN';
  createdAt?: string | null; // Fecha de creación del usuario
  deletedAt?: string | null; // Para soft delete
  lastLogin?: string | null; // Último inicio de sesión
  loginAttempts?: number; // Intentos de login fallidos
  lockedUntil?: string | null; // Fecha hasta la que está bloqueada la cuenta
}

export interface UsuarioCreateDTO {
  email: string;
  password: string;
  name: string;
  phone?: string;
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

// Alias para compatibilidad con código existente
export interface Usuario extends UsuarioDTO {}