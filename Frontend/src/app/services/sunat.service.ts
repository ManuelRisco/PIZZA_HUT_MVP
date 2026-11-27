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

export interface RucLookupResponse {
  ruc: string;
  razonSocial: string;
  direccion: string;
  estado?: string;
  condicion?: string;
  distrito?: string;
  provincia?: string;
  departamento?: string;
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

export interface NotaCreditoEmisionResponse {
  orderId: number;
  serie: string;
  numero: number;
  comprobante: string;
  boletaModificadaComprobante: string;
  tipoNota: number;
  motivo: string;
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

  consultarDni(dni: string): Observable<DniLookupResponse> {
    const clean = (dni || '').trim();
    return this.http.get<any>(`${this.apiUrl}/sunat/dni/${encodeURIComponent(clean)}`).pipe(
      map(res => res.data !== undefined ? res.data : res)
    );
  }

  configurarDni(dni: string, ageVerified: boolean, name?: string): Observable<UsuarioDTO> {
    return this.http.post<any>(`${this.apiUrl}/sunat/configurar-dni`, {
      dni: (dni || '').trim(),
      ageVerified,
      name: name ? name.trim() : undefined
    }).pipe(
      map(res => res.data !== undefined ? res.data : res)
    );
  }

  consultarRuc(ruc: string): Observable<RucLookupResponse> {
    const clean = (ruc || '').trim();
    return this.http.get<any>(`${this.apiUrl}/sunat/ruc/${encodeURIComponent(clean)}`).pipe(
      map(res => res.data !== undefined ? res.data : res)
    );
  }

  configurarRuc(
    ruc: string,
    rucVerified: boolean,
    razonSocial?: string,
    direccionFiscal?: string
  ): Observable<UsuarioDTO> {
    return this.http.post<any>(`${this.apiUrl}/sunat/configurar-ruc`, {
      ruc: (ruc || '').trim(),
      rucVerified,
      razonSocial: razonSocial ? razonSocial.trim() : undefined,
      direccionFiscal: direccionFiscal ? direccionFiscal.trim() : undefined
    }).pipe(
      map(res => res.data !== undefined ? res.data : res)
    );
  }

  emitirBoletaPedido(
    orderId: number,
    payload: {
      dni?: string;
      ruc?: string;
      nombre?: string;
      direccion?: string;
      tipoComprobante?: 1 | 2;
      rucVerified?: boolean;
      enviarCorreo?: boolean;
    } = {}
  ): Observable<BoletaEmisionResponse> {
    return this.http.post<any>(`${this.apiUrl}/sunat/boleta/order/${orderId}/emitir`, payload).pipe(
      map(res => res.data !== undefined ? res.data : res)
    );
  }

  obtenerPdfBlob(orderId: number, download: boolean = false): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/sunat/boleta/order/${orderId}/pdf?download=${download}`, {
      responseType: 'blob'
    });
  }

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

  emitirNotaCreditoPedido(
    orderId: number,
    payload: { motivo?: string; enviarCorreo?: boolean } = {}
  ): Observable<NotaCreditoEmisionResponse> {
    return this.http.post<any>(`${this.apiUrl}/sunat/nota-credito/order/${orderId}/emitir`, payload).pipe(
      map(res => res.data !== undefined ? res.data : res)
    );
  }

  obtenerPdfNotaCreditoBlob(orderId: number, download: boolean = false): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/sunat/nota-credito/order/${orderId}/pdf?download=${download}`, {
      responseType: 'blob'
    });
  }

  abrirNotaCreditoPdf(orderId: number, externalPdfUrl?: string | null): void {
    if (externalPdfUrl && externalPdfUrl.startsWith('http')) {
      window.open(externalPdfUrl, '_blank', 'noopener,noreferrer');
      return;
    }
    this.obtenerPdfNotaCreditoBlob(orderId, false).subscribe({
      next: (blob) => {
        const blobUrl = URL.createObjectURL(blob);
        window.open(blobUrl, '_blank', 'noopener,noreferrer');
        setTimeout(() => URL.revokeObjectURL(blobUrl), 60000);
      },
      error: (err) => {
        console.error('Error al abrir PDF de nota de crédito:', err);
      }
    });
  }
}
