import { Injectable } from '@angular/core';
import { CanActivate, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

@Injectable({
  providedIn: 'root'
})
export class AdminGuard implements CanActivate {

  constructor(private authService: AuthService, private router: Router) {}

  canActivate(): boolean {
    // Verificar login, rol admin, token no expirado y validar integridad
    if (this.authService.isLoggedIn() && 
        this.authService.isAdmin() && 
        !this.authService.isTokenExpired() &&
        this.authService.validateUserIntegrity()) {
      return true;
    } else {
      // Si alguna validación falla, hacer logout y redirigir
      this.authService.logout();
      this.router.navigate(['/join']);
      return false;
    }
  }
}