import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import {
  ProductionCatalogCostDTO,
  ProductionEstimateRequestDTO,
  ProductionEstimateResponseDTO
} from '../models/admin.interface';

@Injectable({
  providedIn: 'root'
})
export class ProductionCostService {
  private readonly apiUrl = `${environment.apiUrl}/production`;

  constructor(private readonly http: HttpClient) {}

  obtenerCatalogoCostos(): Observable<ProductionCatalogCostDTO> {
    return this.http.get<any>(`${this.apiUrl}/catalog-costs`).pipe(
      map(res => (res.data !== undefined ? res.data : res))
    );
  }

  estimarLoteProduccion(payload: ProductionEstimateRequestDTO): Observable<ProductionEstimateResponseDTO> {
    return this.http.post<any>(`${this.apiUrl}/estimate`, payload).pipe(
      map(res => (res.data !== undefined ? res.data : res))
    );
  }
}

