import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';

export interface CinemetaResponse {
    meta: {
        id: string;
        type: string;
        name: string;
        poster?: string;
        background?: string;
        logo?: string;
        description?: string;
        releaseInfo?: string;
        imdbRating?: string;
        genres?: string[];
        cast?: string[];
        director?: string[];
        runtime?: string;
    };
}

@Injectable({
    providedIn: 'root'
})
export class CinemetaService {
    private readonly baseUrl = 'https://v3-cinemeta.strem.io/meta';

    constructor(private http: HttpClient) { }

    getMovieMeta(imdbId: string): Observable<CinemetaResponse['meta'] | null> {
        if (!imdbId) return of(null);
        return this.http.get<CinemetaResponse>(`${this.baseUrl}/movie/${imdbId}.json`).pipe(
            map(response => response.meta),
            catchError(() => of(null)) // fallback to null if not found
        );
    }

    getSeriesMeta(imdbId: string): Observable<CinemetaResponse['meta'] | null> {
        if (!imdbId) return of(null);
        return this.http.get<CinemetaResponse>(`${this.baseUrl}/series/${imdbId}.json`).pipe(
            map(response => response.meta),
            catchError(() => of(null)) // fallback to null if not found
        );
    }
}
