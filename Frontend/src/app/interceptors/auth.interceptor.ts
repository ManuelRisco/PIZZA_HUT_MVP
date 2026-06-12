import { Injectable } from '@angular/core';
import { HttpInterceptor, HttpRequest, HttpHandler, HttpEvent, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { AuthService } from '../services/auth.service';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {

  constructor(private authService: AuthService) {}

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    const token = this.authService.getToken();

    // Solo enviar token si existe Y no está expirado
    if (token && !this.authService.isTokenExpired()) {
      const authReq = req.clone({
        headers: req.headers.set('Authorization', `Bearer ${token}`)
      });
      
      return next.handle(authReq).pipe(
        catchError((error: HttpErrorResponse) => {
          // Si el backend rechaza el token (401/403), cerrar sesión
          if (error.status === 401 || error.status === 403) {
            this.authService.logout();
          }
          return throwError(() => error);
        })
      );
    }

    // Si hay token pero está expirado, limpiar sin redirigir
    if (token && this.authService.isTokenExpired()) {
      this.authService.clearSessionSilently();
    }

    // Sin token: continuar con la petición
    // El backend permitirá acceso público
    return next.handle(req);
  }
}
