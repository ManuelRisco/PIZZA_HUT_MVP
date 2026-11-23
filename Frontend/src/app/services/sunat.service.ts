import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { UsuarioDTO } from '../models/usuario.interface';

export interface DniLookupResponse {
  dni: string;
  nombres: string;
  apellidoPaterno: string;
  apellidoMaterno: string;
  nombreCompleto: string;
  source?: string;
}

export interface BoletaEmisionResponse {
  orderId: number;
  serie: string;
  numero: number;
  comprobante: string;
  clienteDni: string;
  clienteNombre: string;
  clienteDireccion: string;
  totalGravada: number;
  totalIgv: number;
  total: number;
  pdfUrl: string;
  xmlUrl?: string;
  cdrUrl?: string;
  enlace?: string;
  hash?: string;
  qr?: string;
  estado: string;
  emitidaAt?: string;
}

@Injectable({
  providedIn: 'root'
})
export class SunatService {
  private readonly apiUrl = environment.apiUrl;

  constructor(private readonly http: HttpClient) {}

  /**
   * Consulta un DNI de 8 dígitos en RENIEC (Decolecta API + respaldo RENIEC)
   */
  consultarDni(dni: string): Observable<DniLookupResponse> {
    const clean = (dni || '').trim();
    return this.http.get<any>(`${this.apiUrl}/sunat/dni/${encodeURIComponent(clean)}`).pipe(
      map(res => res.data !== undefined ? res.data : res)
    );
  }

  /**
   * Vincula/actualiza el DNI del usuario autenticado confirmando mayoría de edad (+18) y titularidad
   */
  configurarDni(dni: string, ageVerified: boolean, name?: string): Observable<UsuarioDTO> {
    return this.http.post<any>(`${this.apiUrl}/sunat/configurar-dni`, {
      dni: (dni || '').trim(),
      ageVerified,
      name: name ? name.trim() : undefined
    }).pipe(
      map(res => res.data !== undefined ? res.data : res)
    );
  }

  /**
   * Emite (o recupera si ya fue emitida) la Boleta de Venta Electrónica de un pedido
   */
  emitirBoletaPedido(orderId: number, payload: { dni?: string; nombre?: string; enviarCorreo?: boolean } = {}): Observable<BoletaEmisionResponse> {
    return this.http.post<any>(`${this.apiUrl}/sunat/boleta/order/${orderId}/emitir`, payload).pipe(
      map(res => res.data !== undefined ? res.data : res)
    );
  }

  /**
   * Descarga el PDF de la Boleta de Venta Electrónica como Blob autenticado
   */
  obtenerPdfBlob(orderId: number, download: boolean = false): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/sunat/boleta/order/${orderId}/pdf?download=${download}`, {
      responseType: 'blob'
    });
  }

  /**
   * Abre el PDF de la Boleta de Venta Electrónica en una nueva pestaña
   * (usa el enlace oficial de Nubefact si es público o descarga el Blob autenticado del backend).
   */
  abrirBoletaPdf(orderId: number, externalPdfUrl?: string | null): void {
    if (externalPdfUrl && externalPdfUrl.startsWith('http')) {
      window.open(externalPdfUrl, '_blank', 'noopener,noreferrer');
      return;
    }
    this.obtenerPdfBlob(orderId, false).subscribe({
      next: (blob) => {
        const blobUrl = URL.createObjectURL(blob);
        window.open(blobUrl, '_blank', 'noopener,noreferrer');
        setTimeout(() => URL.revokeObjectURL(blobUrl), 60000);
      },
      error: (err) => {
        console.error('Error al abrir PDF de boleta:', err);
      }
    });
  }
}
