import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { ApiService } from '../../services/api.service';
import { TmdbService } from '../../services/tmdb.service';
import { LibraryItem } from '../../models/auth.model';
import { forkJoin, of, Observable } from 'rxjs';
import { catchError, map } from 'rxjs/operators';

@Component({
  selector: 'app-rentals',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './rentals.component.html',
  styleUrls: ['./rentals.component.scss']
})
export class RentalsComponent implements OnInit {
  items: LibraryItem[] = [];
  loading = false;
  error: string | null = null;

  /** Poster URLs keyed by movieId */
  posters: Record<string, string> = {};
  postersLoading = false;

  constructor(private apiService: ApiService, private tmdbService: TmdbService) {}

  ngOnInit(): void {
    this.loading = true;
    this.apiService.getLibrary().subscribe({
      next: (res) => {
        this.items = res?.data?.items || [];
        this.loading = false;
        this.fetchPosters();
      },
      error: (err) => {
        this.loading = false;
        if (err?.status === 401) {
          this.error = 'Please sign in to view your rentals.';
          return;
        }
        this.error = err?.error?.error?.message || 'Failed to load rentals.';
      }
    });
  }

  isSeries(item: LibraryItem): boolean {
    const t = (item.titleType || '').toLowerCase();
    return t.includes('tv') || t.includes('series');
  }

  typeLabel(item: LibraryItem): string {
    const t = (item.titleType || '').toLowerCase();
    if (t.includes('miniseries') || t.includes('mini')) return 'Mini-Series';
    if (t.includes('tv') || t.includes('series')) return 'TV Show';
    return 'Movie';
  }

  private fetchPosters(): void {
    if (this.items.length === 0) return;
    this.postersLoading = true;

    const fetches: Observable<{ id: string; url: string | null }>[] = this.items.map((item) =>
      this.resolvePosterUrl(item.movieId).pipe(
        map((url) => ({ id: item.movieId, url })),
        catchError(() => of({ id: item.movieId, url: null }))
      )
    );

    forkJoin(fetches).subscribe((results) => {
      results.forEach(({ id, url }) => {
        if (url) this.posters[id] = url;
      });
      this.postersLoading = false;
    });
  }

  private resolvePosterUrl(id: string): Observable<string | null> {
    if (!id) return of(null);

    if (id.startsWith('tmdb-movie-')) {
      const tmdbId = Number.parseInt(id.replace('tmdb-movie-', ''), 10);
      if (!Number.isNaN(tmdbId)) {
        return this.tmdbService.getTmdbMediaDetail(tmdbId, 'movie').pipe(
          map((res) => res.poster),
          catchError(() => of(null))
        );
      }
    }

    if (id.startsWith('tmdb-tv-')) {
      const tmdbId = Number.parseInt(id.replace('tmdb-tv-', ''), 10);
      if (!Number.isNaN(tmdbId)) {
        return this.tmdbService.getTmdbMediaDetail(tmdbId, 'tv').pipe(
          map((res) => res.poster),
          catchError(() => of(null))
        );
      }
    }

    if (id.startsWith('tt')) {
      return this.tmdbService.getPosterByImdbId(id).pipe(
        map((res) => res.poster),
        catchError(() => of(null))
      );
    }

    if (id.startsWith('s') && id.length > 1) {
      const tmdbId = Number.parseInt(id.substring(1), 10);
      if (!Number.isNaN(tmdbId)) {
        return this.tmdbService.getTmdbMediaDetail(tmdbId, 'tv').pipe(
          map((res) => res.poster),
          catchError(() => of(null))
        );
      }
    }

    if (id.startsWith('m') && id.length > 1) {
      const tmdbId = Number.parseInt(id.substring(1), 10);
      if (!Number.isNaN(tmdbId)) {
        return this.tmdbService.getTmdbMediaDetail(tmdbId, 'movie').pipe(
          map((res) => res.poster),
          catchError(() => of(null))
        );
      }
    }

    return of(null);
  }
}
