import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { ToastService } from '../services/toast.service';

let lastOfflineToastTimestamp = 0;
const OFFLINE_TOAST_COOLDOWN_MS = 4500;

function extractServerMessage(error: HttpErrorResponse, fallback: string): string {
  if (!error.error) return fallback;
  if (typeof error.error === 'string' && error.error.trim().length > 0 && !error.error.trim().startsWith('<')) {
    return error.error.trim();
  }
  return error.error?.message || error.error?.error || fallback;
}

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const toastService = inject(ToastService);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      // Ignorar rutas silenciosas o con manejo propio en pantalla
      if (
        req.url.includes('/api/ingresar') ||
        req.url.includes('/api/registro') ||
        req.url.includes('/api/verificar-estado-usuario') ||
        req.url.includes('/api/refresh-token')
      ) {
        return throwError(() => error);
      }

      if (error.error instanceof ErrorEvent) {
        toastService.showError(`Error de cliente: ${error.error.message}`);
        return throwError(() => error);
      }

      switch (error.status) {
        case 0:
        case 502:
        case 503:
        case 504: {
          const now = Date.now();
          if (now - lastOfflineToastTimestamp > OFFLINE_TOAST_COOLDOWN_MS) {
            lastOfflineToastTimestamp = now;
            toastService.showError('No se pudo conectar con el servidor. Verifique que el backend esté activo.');
          }
          break;
        }
        case 400:
          toastService.showError(extractServerMessage(error, 'Petición inválida o errores de validación.'));
          break;
        case 401:
          toastService.showError('Sesión expirada o credenciales inválidas.');
          break;
        case 403:
          toastService.showError(extractServerMessage(error, 'Acceso denegado.'));
          break;
        case 404:
          toastService.showError(extractServerMessage(error, 'Recurso no encontrado.'));
          break;
        case 409:
          toastService.showError(extractServerMessage(error, 'Conflicto de datos (posible registro duplicado o en uso).'));
          break;
        case 413:
          toastService.showError(extractServerMessage(error, 'El archivo supera el tamaño máximo permitido (5MB).'));
          break;
        case 500:
          toastService.showError(extractServerMessage(error, 'Error interno del servidor.'));
          break;
        default:
          toastService.showError(extractServerMessage(error, `Error del servidor (${error.status}).`));
          break;
      }

      return throwError(() => error);
    })
  );
};
