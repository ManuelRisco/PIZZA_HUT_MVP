import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import {
  InventorySummaryDTO,
  StockAlertItemDTO,
  StockMovementDTO,
  StockOperationRequestDTO
} from '../models/admin.interface';

@Injectable({
  providedIn: 'root'
})
export class InventoryService {
  private readonly apiUrl = `${environment.apiUrl}/inventory`;

  constructor(private readonly http: HttpClient) {}

  obtenerResumen(): Observable<InventorySummaryDTO> {
    return this.http.get<any>(`${this.apiUrl}/summary`).pipe(
      map(res => (res.data !== undefined ? res.data : res))
    );
  }

  obtenerAlertas(): Observable<StockAlertItemDTO[]> {
    return this.http.get<any>(`${this.apiUrl}/alerts`).pipe(
      map(res => (res.data !== undefined ? res.data : res))
    );
  }

  obtenerMovimientos(itemType?: string, movementType?: string, limit = 100): Observable<StockMovementDTO[]> {
    let params = new HttpParams().set('limit', limit.toString());
    if (itemType && itemType !== 'TODOS') {
      params = params.set('itemType', itemType);
    }
    if (movementType && movementType !== 'TODOS') {
      params = params.set('movementType', movementType);
    }
    return this.http.get<any>(`${this.apiUrl}/movements`, { params }).pipe(
      map(res => (res.data !== undefined ? res.data : res))
    );
  }

  reabastecer(payload: StockOperationRequestDTO): Observable<StockMovementDTO> {
    return this.http.post<any>(`${this.apiUrl}/restock`, payload).pipe(
      map(res => (res.data !== undefined ? res.data : res))
    );
  }

  ajustarStock(payload: StockOperationRequestDTO): Observable<StockMovementDTO> {
    return this.http.post<any>(`${this.apiUrl}/adjust`, payload).pipe(
      map(res => (res.data !== undefined ? res.data : res))
    );
  }
}

