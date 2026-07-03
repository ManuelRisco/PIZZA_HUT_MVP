import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { ToastService } from '../services/toast.service';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const toastService = inject(ToastService);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      // Ignorar la ruta de login para que maneje sus propios errores en pantalla
      if (req.url.includes('/api/ingresar') || req.url.includes('/api/registro')) {
        return throwError(() => error);
      }

      let errorMsg = 'Ocurrió un error inesperado';

      if (error.error instanceof ErrorEvent) {
        // Error del lado del cliente
        errorMsg = `Error: ${error.error.message}`;
        toastService.showError(errorMsg);
      } else {
        // Error del lado del servidor
        switch (error.status) {
          case 0:
            errorMsg = 'Error de conexión con el servidor.';
            toastService.showError(errorMsg);
            break;
          case 400:
            errorMsg = error.error?.message || 'Petición inválida o errores de validación.';
            toastService.showError(errorMsg);
            break;
          case 401:
            errorMsg = 'Sesión expirada o credenciales inválidas.';
            toastService.showError(errorMsg);
            break;
          case 403:
            errorMsg = error.error?.message || 'Acceso denegado.';
            toastService.showError(errorMsg);
            break;
          case 404:
            errorMsg = 'Recurso no encontrado.';
            toastService.showError(errorMsg);
            break;
          case 409:
            errorMsg = error.error?.message || 'Conflicto de datos (posible registro duplicado).';
            toastService.showError(errorMsg);
            break;
          case 500:
            errorMsg = 'Error interno del servidor.';
            toastService.showError(errorMsg);
            break;
          default:
            errorMsg = error.error?.message || `Error del servidor: ${error.status}`;
            toastService.showError(errorMsg);
            break;
        }
      }

      return throwError(() => error);
    })
  );
};
