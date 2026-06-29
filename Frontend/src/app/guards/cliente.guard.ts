import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const ClienteGuard = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const user = authService.getCurrentUser();

  // Si no hay usuario autenticado, redirigir a login
  if (!user) {
    router.navigate(['/join']);
    return false;
  }

  // Permitir acceso tanto a ADMIN como a CLIENT/USER
  // Los administradores también pueden acceder a las funcionalidades de cliente
  return true;
};
