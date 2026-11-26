import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { environment } from '../../environments/environment';

export interface DeliveryFeeConfig {
  deliveryFee: number;
  currency?: string;
  updatedAt?: string | null;
  message?: string;
}

@Injectable({ providedIn: 'root' })
export class StoreSettingsService {
  private readonly apiUrl = `${environment.apiUrl}/settings`;
  private readonly deliveryFeeSubject = new BehaviorSubject<number>(5.0);
  readonly deliveryFee$ = this.deliveryFeeSubject.asObservable();

  constructor(private readonly http: HttpClient) {}

  getCurrentDeliveryFee(): number {
    return this.deliveryFeeSubject.value;
  }

  loadDeliveryFee(): Observable<DeliveryFeeConfig> {
    return this.http.get<DeliveryFeeConfig>(`${this.apiUrl}/delivery-fee`).pipe(
      tap((res) => {
        const fee = Number(res?.deliveryFee);
        if (!Number.isNaN(fee) && fee >= 0) {
          this.deliveryFeeSubject.next(fee);
        }
      }),
      catchError(() => of({ deliveryFee: this.deliveryFeeSubject.value, currency: 'PEN' })),
    );
  }

  updateDeliveryFee(deliveryFee: number): Observable<DeliveryFeeConfig> {
    return this.http.put<DeliveryFeeConfig>(`${this.apiUrl}/delivery-fee`, { deliveryFee }).pipe(
      tap((res) => {
        const fee = Number(res?.deliveryFee);
        if (!Number.isNaN(fee) && fee >= 0) {
          this.deliveryFeeSubject.next(fee);
        }
      }),
    );
  }
}
