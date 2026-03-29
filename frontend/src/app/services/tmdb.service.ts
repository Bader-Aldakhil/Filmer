import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { environment } from '../../environments/environment';

export interface TmdbCastMember {
    id: number;
    name: string;
    character: string;
    profile_path: string | null;
    order: number;
}

export interface TmdbCreditsResponse {
    id: number;
    cast: TmdbCastMember[];
}

@Injectable({
    providedIn: 'root'
})
export class TmdbService {
    private readonly apiKey = environment.tmdbApiKey;
    private readonly baseUrl = 'https://api.themoviedb.org/3';
    private readonly imageBaseUrl = 'https://image.tmdb.org/t/p';

    constructor(private http: HttpClient) { }

    /**
     * Find a movie/TV show by IMDB ID and return the full cast with photos
     */
    getCastByImdbId(imdbId: string): Observable<TmdbCastMember[]> {
        if (!imdbId || !this.apiKey) return of([]);

        return new Observable<TmdbCastMember[]>(subscriber => {
            this.http.get<any>(
                `${this.baseUrl}/find/${imdbId}?api_key=${this.apiKey}&external_source=imdb_id`
            ).subscribe({
                next: (findResponse) => {
                    const movieResults = findResponse.movie_results || [];
                    const tvResults = findResponse.tv_results || [];
                    const results = movieResults.length > 0 ? movieResults : tvResults;
                    const mediaType = movieResults.length > 0 ? 'movie' : 'tv';

                    if (results.length > 0) {
                        const tmdbId = results[0].id;
                        this.http.get<TmdbCreditsResponse>(
                            `${this.baseUrl}/${mediaType}/${tmdbId}/credits?api_key=${this.apiKey}`
                        ).subscribe({
                            next: (creditsResponse) => {
                                subscriber.next(creditsResponse.cast || []);
                                subscriber.complete();
                            },
                            error: () => {
                                subscriber.next([]);
                                subscriber.complete();
                            }
                        });
                    } else {
                        subscriber.next([]);
                        subscriber.complete();
                    }
                },
                error: () => {
                    subscriber.next([]);
                    subscriber.complete();
                }
            });
        });
    }

    /**
     * Get the full profile image URL for a given TMDB profile path
     */
    getProfileImageUrl(profilePath: string | null, size: 'w45' | 'w185' | 'h632' | 'original' = 'w185'): string | null {
        if (!profilePath) return null;
        return `${this.imageBaseUrl}/${size}${profilePath}`;
    }

    /**
     * Get poster and backdrop URLs for a movie/TV show by IMDB ID
     */
    getPosterByImdbId(imdbId: string): Observable<{ poster: string | null; backdrop: string | null; overview: string | null }> {
        if (!imdbId || !this.apiKey) return of({ poster: null, backdrop: null, overview: null });

        return new Observable(subscriber => {
            this.http.get<any>(
                `${this.baseUrl}/find/${imdbId}?api_key=${this.apiKey}&external_source=imdb_id`
            ).subscribe({
                next: (findResponse) => {
                    const movieResults = findResponse.movie_results || [];
                    const tvResults = findResponse.tv_results || [];
                    const results = movieResults.length > 0 ? movieResults : tvResults;

                    if (results.length > 0) {
                        const result = results[0];
                        subscriber.next({
                            poster: result.poster_path ? `${this.imageBaseUrl}/w500${result.poster_path}` : null,
                            backdrop: result.backdrop_path ? `${this.imageBaseUrl}/w1280${result.backdrop_path}` : null,
                            overview: result.overview || null
                        });
                    } else {
                        subscriber.next({ poster: null, backdrop: null, overview: null });
                    }
                    subscriber.complete();
                },
                error: () => {
                    subscriber.next({ poster: null, backdrop: null, overview: null });
                    subscriber.complete();
                }
            });
        });
    }
}
