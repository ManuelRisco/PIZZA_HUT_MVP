import { ApplicationConfig, Injectable, inject, provideBrowserGlobalErrorListeners, provideZoneChangeDetection } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { provideRouter, RouterStateSnapshot, TitleStrategy } from '@angular/router';
import { provideHttpClient, withInterceptors, HttpInterceptorFn } from '@angular/common/http';

import { routes } from './app.routes';
import { AdminGuard } from './guards/admin.guard';
import { AuthService } from './services/auth.service';
import { errorInterceptor } from './interceptors/error.interceptor';

@Injectable({ providedIn: 'root' })
export class EcoPizzaTitleStrategy extends TitleStrategy {
  constructor(private readonly title: Title) {
    super();
  }

  override updateTitle(routerState: RouterStateSnapshot): void {
    const pageTitle = this.buildTitle(routerState);
    this.title.setTitle(pageTitle ? `Eco Pizza - ${pageTitle}` : 'Eco Pizza - Inicio');
  }
}

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const token = authService.getToken();

  if (token && !authService.isTokenExpired()) {
    const authReq = req.clone({
      headers: req.headers.set('Authorization', `Bearer ${token}`)
    });
    return next(authReq);
  }

  return next(req);
};

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    { provide: TitleStrategy, useClass: EcoPizzaTitleStrategy },
    provideHttpClient(withInterceptors([authInterceptor, errorInterceptor])),
    AdminGuard
  ]
};
