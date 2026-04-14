import { Injectable, signal } from '@angular/core';
import { catchError, map, of, tap } from 'rxjs';
import { ApiService } from './api.service';
import { CustomerSession } from '../models/auth.model';

@Injectable({
  providedIn: 'root'
})
export class AuthStateService {
  user = signal<CustomerSession | null>(null);
  loading = signal(false);

  constructor(private apiService: ApiService) {}

  refreshSession() {
    this.loading.set(true);
    return this.apiService.checkSession().pipe(
      map((res: any) => res?.data as CustomerSession),
      tap((user) => {
        this.user.set(user);
        this.loading.set(false);
      }),
      catchError(() => {
        this.user.set(null);
        this.loading.set(false);
        return of(null);
      })
    );
  }

  clearSession() {
    this.user.set(null);
  }
}
