import { Injectable } from '@angular/core';
import { CanActivate, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

@Injectable({
  providedIn: 'root'
})
export class AdminGuard implements CanActivate {

  constructor(private readonly authService: AuthService, private readonly router: Router) {}

  canActivate(): boolean {
    if (this.authService.isLoggedIn() && 
        this.authService.isAdmin() && 
        !this.authService.isTokenExpired() &&
        this.authService.validateUserIntegrity()) {
      return true;
    } else {
      this.authService.logout();
      this.router.navigate(['/join']);
      return false;
    }
  }
}
