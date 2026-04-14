import { Injectable } from '@angular/core';
import { ApiService } from './api.service';
import { Observable, map, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

export interface WatchProgress {
  movieId: string;
  isSeries: boolean;
  season?: number;
  episode?: number;
  updatedAt?: number;
}

@Injectable({ providedIn: 'root' })
export class ContinueWatchingService {
  
  constructor(private apiService: ApiService) {}

  getProgress(movieId: string): Observable<WatchProgress | null> {
    return this.getAllProgress().pipe(
      map(progressList => progressList.find(p => p.movieId === movieId) || null)
    );
  }

  saveProgress(movieId: string, isSeries: boolean, season?: number, episode?: number): void {
    this.apiService.saveWatchProgress(movieId, isSeries, season, episode).subscribe({
      error: (e: any) => console.error('Failed to save progress', e)
    });
  }

  getAllProgress(): Observable<WatchProgress[]> {
    return this.apiService.getAllWatchProgress().pipe(
      map(res => res?.data || []),
      catchError(() => of([]))
    );
  }
}
