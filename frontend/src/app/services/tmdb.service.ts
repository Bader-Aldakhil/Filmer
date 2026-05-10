import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of, map, catchError, forkJoin, switchMap } from 'rxjs';
import { GenreInfo, MovieListItem, MovieDetail, StarInfo } from '../models/movie.model';

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

interface TmdbGenre {
    id: number;
    name: string;
}

interface TmdbDiscoverItem {
    id: number;
    title?: string;
    name?: string;
    release_date?: string;
    first_air_date?: string;
    vote_average?: number;
    vote_count?: number;
    genre_ids?: number[];
    poster_path?: string | null;
}

interface TmdbDiscoverResponse {
    page: number;
    total_pages: number;
    results: TmdbDiscoverItem[];
}

interface TmdbDetailResponse {
    id: number;
    title?: string;
    name?: string;
    release_date?: string;
    first_air_date?: string;
    vote_average?: number;
    vote_count?: number;
    genres?: TmdbGenre[];
    seasons?: Array<{
        season_number?: number;
        episode_count?: number;
    }>;
    overview?: string;
    poster_path?: string | null;
    backdrop_path?: string | null;
    external_ids?: {
        imdb_id?: string | null;
    };
}

interface TmdbExternalIdsResponse {
    imdb_id?: string | null;
}

interface TmdbFindResponse {
    movie_results?: Array<{ id: number }>;
    tv_results?: Array<{ id: number }>;
}

interface TmdbVideo {
    key?: string;
    site?: string;
    type?: string;
}

interface TmdbVideosResponse {
    results?: TmdbVideo[];
}

export interface TrailerSources {
    embedUrl: string;
    youtubeUrl: string;
}

export interface TvSeasonEpisodes {
    seasonNumber: number;
    episodeCount: number;
}

export interface TvEpisodeDetail {
    name: string | null;
    overview: string | null;
    stillUrl: string | null;
    episodeNumber: number;
    seasonNumber: number;
}

interface CinemetaMetaResponse {
    meta?: {
        imdbRating?: string;
    };
}

interface OmdbTitleResponse {
    imdbVotes?: string;
    Response?: string;
}

interface RuntimeConfig {
    TMDB_API_KEY?: string;
    OMDB_API_KEY?: string;
}

@Injectable({
    providedIn: 'root'
})
export class TmdbService {
    private readonly apiKey = this.getRuntimeConfig().TMDB_API_KEY || '';
    private readonly baseUrl = 'https://api.themoviedb.org/3';
    private readonly imageBaseUrl = 'https://image.tmdb.org/t/p';
    private readonly omdbApiKey = this.getRuntimeConfig().OMDB_API_KEY || '';

    constructor(private http: HttpClient) { }

    private getRuntimeConfig(): RuntimeConfig {
        const runtime = (window as unknown as { RUNTIME_CONFIG?: RuntimeConfig }).RUNTIME_CONFIG;
        return runtime || {};
    }

    private getYear(dateValue?: string): number | undefined {
        if (!dateValue || dateValue.length < 4) {
            return undefined;
        }
        const year = Number.parseInt(dateValue.substring(0, 4), 10);
        return Number.isNaN(year) ? undefined : year;
    }

    private buildPosterUrl(path?: string | null): string | null {
        return path ? `${this.imageBaseUrl}/w500${path}` : null;
    }

    // IMDb-style weighted rating to avoid low-vote titles dominating the list.
    private weightedScore(rawRating?: number, voteCount?: number, baseline = 6.8, minVotes = 1000): number {
        if (!rawRating) {
            return 0;
        }
        const v = voteCount || 0;
        const r = rawRating;
        const m = minVotes;
        const c = baseline;
        return (v / (v + m)) * r + (m / (v + m)) * c;
    }

    private sortByWeightedScore(items: TmdbDiscoverItem[], mediaType: 'movie' | 'tv'): TmdbDiscoverItem[] {
        const minVotes = mediaType === 'movie' ? 1000 : 500;
        const baseline = mediaType === 'movie' ? 6.9 : 7.0;

        return [...items].sort((a, b) => {
            const scoreB = this.weightedScore(b.vote_average, b.vote_count, baseline, minVotes);
            const scoreA = this.weightedScore(a.vote_average, a.vote_count, baseline, minVotes);
            if (scoreB !== scoreA) {
                return scoreB - scoreA;
            }
            return (b.vote_count || 0) - (a.vote_count || 0);
        });
    }

    getMovieGenres(): Observable<GenreInfo[]> {
        return this.http
            .get<{ genres: TmdbGenre[] }>(`${this.baseUrl}/genre/movie/list?api_key=${this.apiKey}`)
            .pipe(
                map(response => response.genres.map(g => ({ id: g.id, name: g.name }))),
                catchError(() => of([]))
            );
    }

    getTvGenres(): Observable<GenreInfo[]> {
        return this.http
            .get<{ genres: TmdbGenre[] }>(`${this.baseUrl}/genre/tv/list?api_key=${this.apiKey}`)
            .pipe(
                map(response => response.genres.map(g => ({ id: g.id, name: g.name }))),
                catchError(() => of([]))
            );
    }

    discoverMovies(page: number, query?: string, genreId?: number, minRating?: number, sortBy: 'top_rated' | 'popularity' = 'top_rated'): Observable<{ items: MovieListItem[]; posters: Record<string, string>; page: number; totalPages: number }> {
        const isSearch = !!(query && query.trim().length > 0);
        const isFiltered = !!genreId || !!(minRating && minRating > 0);
        const useMainstreamTopRated = !isSearch && !isFiltered && sortBy === 'top_rated';

        const base = isSearch
            ? `${this.baseUrl}/search/movie`
            : `${this.baseUrl}/discover/movie`;

        let url = `${base}?api_key=${this.apiKey}&page=${page}`;

        if (query && query.trim().length > 0) {
            url += `&query=${encodeURIComponent(query.trim())}`;
        }
        if (genreId) {
            url += `&with_genres=${genreId}`;
        }
        if (minRating && minRating > 0) {
            url += `&vote_average.gte=${minRating}`;
            url += '&vote_count.gte=100';
        }
        if (sortBy === 'top_rated') {
            const voteFloor = useMainstreamTopRated ? 3000 : (isFiltered ? 80 : 700);
            url += `&vote_count.gte=${voteFloor}`;
            url += '&sort_by=vote_average.desc';
            if (useMainstreamTopRated) {
                url += '&with_original_language=en';
                url += '&without_genres=99';
            }
        }
        if (!isSearch && sortBy !== 'top_rated') {
            url += sortBy === 'popularity' ? '&sort_by=popularity.desc' : '&sort_by=vote_average.desc';
        }

        return this.http.get<TmdbDiscoverResponse>(url).pipe(
            map(response => {
                const rankedResults = sortBy === 'top_rated'
                    ? this.sortByWeightedScore(response.results, 'movie')
                    : response.results;
                const posters: Record<string, string> = {};
                const items = rankedResults.map(item => {
                    const id = `tmdb-movie-${item.id}`;
                    const poster = this.buildPosterUrl(item.poster_path);
                    if (poster) {
                        posters[id] = poster;
                    }

                    return {
                        id,
                        title: item.title || 'Untitled',
                        year: this.getYear(item.release_date),
                        rating: item.vote_average ? Number(item.vote_average.toFixed(1)) : undefined,
                        numVotes: item.vote_count,
                        genres: []
                    } as MovieListItem;
                });

                return {
                    items,
                    posters,
                    page: response.page,
                    totalPages: response.total_pages
                };
            }),
            catchError(() => of({ items: [], posters: {}, page: 1, totalPages: 1 }))
        );
    }

    discoverTvShows(page: number, query?: string, genreId?: number, minRating?: number, sortBy: 'top_rated' | 'popularity' = 'top_rated'): Observable<{ items: MovieListItem[]; posters: Record<string, string>; page: number; totalPages: number }> {
        const isSearch = !!(query && query.trim().length > 0);
        const isFiltered = !!genreId || !!(minRating && minRating > 0);
        const useMainstreamTopRated = !isSearch && !isFiltered && sortBy === 'top_rated';

        const base = isSearch
            ? `${this.baseUrl}/search/tv`
            : `${this.baseUrl}/discover/tv`;

        let url = `${base}?api_key=${this.apiKey}&page=${page}`;

        if (query && query.trim().length > 0) {
            url += `&query=${encodeURIComponent(query.trim())}`;
        }
        if (genreId) {
            url += `&with_genres=${genreId}`;
        }
        if (minRating && minRating > 0) {
            url += `&vote_average.gte=${minRating}`;
            url += '&vote_count.gte=100';
        }
        if (sortBy === 'top_rated') {
            const voteFloor = useMainstreamTopRated ? 1800 : (isFiltered ? 50 : 450);
            url += `&vote_count.gte=${voteFloor}`;
            url += '&sort_by=vote_average.desc';
            if (useMainstreamTopRated) {
                url += '&with_original_language=en';
            }
        }
        if (!isSearch && sortBy !== 'top_rated') {
            url += sortBy === 'popularity' ? '&sort_by=popularity.desc' : '&sort_by=vote_average.desc';
        }

        return this.http.get<TmdbDiscoverResponse>(url).pipe(
            map(response => {
                const rankedResults = sortBy === 'top_rated'
                    ? this.sortByWeightedScore(response.results, 'tv')
                    : response.results;
                const posters: Record<string, string> = {};
                const items = rankedResults.map(item => {
                    const id = `tmdb-tv-${item.id}`;
                    const poster = this.buildPosterUrl(item.poster_path);
                    if (poster) {
                        posters[id] = poster;
                    }

                    return {
                        id,
                        title: item.name || 'Untitled',
                        year: this.getYear(item.first_air_date),
                        rating: item.vote_average ? Number(item.vote_average.toFixed(1)) : undefined,
                        numVotes: item.vote_count,
                        genres: []
                    } as MovieListItem;
                });

                return {
                    items,
                    posters,
                    page: response.page,
                    totalPages: response.total_pages
                };
            }),
            catchError(() => of({ items: [], posters: {}, page: 1, totalPages: 1 }))
        );
    }

    getTmdbMediaDetail(id: number, mediaType: 'movie' | 'tv'): Observable<{ detail: MovieDetail; poster: string | null; backdrop: string | null; overview: string | null; castPhotos: Record<string, string> }> {
        const detailUrl = `${this.baseUrl}/${mediaType}/${id}?api_key=${this.apiKey}&append_to_response=external_ids`;
        const creditsUrl = `${this.baseUrl}/${mediaType}/${id}/credits?api_key=${this.apiKey}`;

        return forkJoin({
            detail: this.http.get<TmdbDetailResponse>(detailUrl),
            credits: this.http.get<TmdbCreditsResponse>(creditsUrl).pipe(catchError(() => of({ id, cast: [] })))
        }).pipe(
            map(({ detail, credits }) => {
                const stars: StarInfo[] = [];
                const photos: Record<string, string> = {};

                credits.cast.slice(0, 20).forEach(member => {
                    stars.push({ id: String(member.id), name: member.name });
                    const photoUrl = this.getProfileImageUrl(member.profile_path);
                    if (photoUrl) {
                        photos[member.name] = photoUrl;
                    }
                });

                return {
                    detail: {
                        id: `${mediaType === 'movie' ? 'tmdb-movie-' : 'tmdb-tv-'}${id}`,
                        imdbId: detail.imdb_id || detail.external_ids?.imdb_id || undefined,
                        title: detail.title || detail.name || 'Untitled',
                        year: this.getYear(detail.release_date || detail.first_air_date),
                        rating: detail.vote_average ? Number(detail.vote_average.toFixed(1)) : undefined,
                        numVotes: detail.vote_count,
                        genres: (detail.genres || []).map(g => ({ id: g.id, name: g.name })),
                        stars
                    },
                    poster: this.buildPosterUrl(detail.poster_path),
                    backdrop: detail.backdrop_path ? `${this.imageBaseUrl}/w1280${detail.backdrop_path}` : null,
                    overview: detail.overview || null,
                    castPhotos: photos
                };
            }),
            catchError(() => of({
                detail: {
                    id: `${mediaType === 'movie' ? 'tmdb-movie-' : 'tmdb-tv-'}${id}`,
                    title: 'Unknown title',
                    genres: [],
                    stars: []
                },
                poster: null,
                backdrop: null,
                overview: null,
                castPhotos: {}
            }))
        );
    }

    getImdbRatingForTmdb(tmdbId: number, mediaType: 'movie' | 'tv'): Observable<number | undefined> {
        const externalIdsUrl = `${this.baseUrl}/${mediaType}/${tmdbId}/external_ids?api_key=${this.apiKey}`;

        return this.http.get<TmdbExternalIdsResponse>(externalIdsUrl).pipe(
            map(response => response.imdb_id || null),
            catchError(() => of(null)),
            map(imdbId => imdbId?.trim() || null),
            switchMap(imdbId => {
                if (!imdbId) {
                    return of(undefined);
                }
                const mediaPath = mediaType === 'movie' ? 'movie' : 'series';
                const url = `https://v3-cinemeta.strem.io/meta/${mediaPath}/${imdbId}.json`;
                return this.http.get<CinemetaMetaResponse>(url).pipe(
                    map(metaResponse => {
                        const ratingText = metaResponse.meta?.imdbRating;
                        if (!ratingText) {
                            return undefined;
                        }
                        const parsed = Number.parseFloat(ratingText);
                        return Number.isNaN(parsed) ? undefined : Number(parsed.toFixed(1));
                    }),
                    catchError(() => of(undefined))
                );
            })
        );
    }

    getImdbVotesForTmdb(tmdbId: number, mediaType: 'movie' | 'tv'): Observable<number | undefined> {
        if (!this.omdbApiKey) {
            return of(undefined);
        }

        const externalIdsUrl = `${this.baseUrl}/${mediaType}/${tmdbId}/external_ids?api_key=${this.apiKey}`;

        return this.http.get<TmdbExternalIdsResponse>(externalIdsUrl).pipe(
            map(response => response.imdb_id || null),
            catchError(() => of(null)),
            map(imdbId => imdbId?.trim() || null),
            switchMap(imdbId => {
                if (!imdbId) {
                    return of(undefined);
                }

                const url = `https://www.omdbapi.com/?i=${imdbId}&apikey=${this.omdbApiKey}`;
                return this.http.get<OmdbTitleResponse>(url).pipe(
                    map(omdb => {
                        const text = omdb.imdbVotes;
                        if (!text) {
                            return undefined;
                        }
                        const normalized = text.replace(/,/g, '');
                        const parsed = Number.parseInt(normalized, 10);
                        return Number.isNaN(parsed) ? undefined : parsed;
                    }),
                    catchError(() => of(undefined))
                );
            })
        );
    }

    getImdbIdForTmdb(tmdbId: number, mediaType: 'movie' | 'tv'): Observable<string | null> {
        const externalIdsUrl = `${this.baseUrl}/${mediaType}/${tmdbId}/external_ids?api_key=${this.apiKey}`;

        return this.http.get<TmdbExternalIdsResponse>(externalIdsUrl).pipe(
            map(response => {
                const imdbId = response.imdb_id?.trim();
                return imdbId || null;
            }),
            catchError(() => of(null))
        );
    }

    /**
     * Find a movie/TV show by IMDB ID and return the full cast with photos
     */
    getCastByImdbId(imdbId: string): Observable<TmdbCastMember[]> {
        if (!imdbId) return of([]);

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
        if (!imdbId) return of({ poster: null, backdrop: null, overview: null });

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

    resolveTmdbFromImdbId(imdbId: string): Observable<{ tmdbId: number; mediaType: 'movie' | 'tv' } | null> {
        if (!imdbId) {
            return of(null);
        }

        return this.http.get<TmdbFindResponse>(
            `${this.baseUrl}/find/${imdbId}?api_key=${this.apiKey}&external_source=imdb_id`
        ).pipe(
            map((findResponse) => {
                const movieResults = findResponse.movie_results || [];
                const tvResults = findResponse.tv_results || [];
                if (movieResults.length > 0) {
                    return { tmdbId: movieResults[0].id, mediaType: 'movie' as const };
                }
                if (tvResults.length > 0) {
                    return { tmdbId: tvResults[0].id, mediaType: 'tv' as const };
                }
                return null;
            }),
            catchError(() => of(null))
        );
    }

    getTrailerEmbedUrl(tmdbId: number, mediaType: 'movie' | 'tv'): Observable<string | null> {
        return this.getTrailerSources(tmdbId, mediaType).pipe(
            map((sources) => sources?.embedUrl || null)
        );
    }

    getTrailerSources(tmdbId: number, mediaType: 'movie' | 'tv'): Observable<TrailerSources | null> {
        return this.http.get<TmdbVideosResponse>(
            `${this.baseUrl}/${mediaType}/${tmdbId}/videos?api_key=${this.apiKey}`
        ).pipe(
            map((response) => {
                const videos = response.results || [];
                const trailer = videos.find(v =>
                    (v.site || '').toLowerCase() === 'youtube' &&
                    ((v.type || '').toLowerCase() === 'trailer' || (v.type || '').toLowerCase() === 'teaser')
                );
                if (!trailer?.key) {
                    return null;
                }
                return {
                    embedUrl: `https://www.youtube-nocookie.com/embed/${trailer.key}`,
                    youtubeUrl: `https://www.youtube.com/watch?v=${trailer.key}`
                };
            }),
            catchError(() => of(null))
        );
    }

    /**
     * Get detail for a specific episode (name, overview, still image)
     */
    getTvEpisodeDetail(tmdbId: number, season: number, episode: number): Observable<TvEpisodeDetail | null> {
        const url = `${this.baseUrl}/tv/${tmdbId}/season/${season}/episode/${episode}?api_key=${this.apiKey}`;
        return this.http.get<any>(url).pipe(
            map((res) => ({
                name: res.name || null,
                overview: res.overview || null,
                stillUrl: res.still_path ? `${this.imageBaseUrl}/w780${res.still_path}` : null,
                episodeNumber: res.episode_number ?? episode,
                seasonNumber: res.season_number ?? season
            })),
            catchError(() => of(null))
        );
    }

    /**
     * Resolve an IMDb ID from any supported movieId format (tt..., tmdb-movie-X, tmdb-tv-X).
     * For IMDb IDs already, returns them directly. For TMDB IDs, calls external_ids.
     * Returns null if resolution fails or the ID format is unrecognised.
     */
    resolveImdbIdFromMovieId(movieId: string, isSeries: boolean): Observable<string | null> {
        if (!movieId) return of(null);

        if (movieId.startsWith('tt')) {
            return of(movieId);
        }

        let mediaType: 'movie' | 'tv' | null = null;
        let tmdbId: number | null = null;

        if (movieId.startsWith('tmdb-movie-')) {
            mediaType = 'movie';
            tmdbId = Number.parseInt(movieId.replace('tmdb-movie-', ''), 10);
        } else if (movieId.startsWith('tmdb-tv-')) {
            mediaType = 'tv';
            tmdbId = Number.parseInt(movieId.replace('tmdb-tv-', ''), 10);
        } else if (movieId.startsWith('s') && movieId.length > 1) {
            mediaType = 'tv';
            tmdbId = Number.parseInt(movieId.substring(1), 10);
        } else if (movieId.startsWith('m') && movieId.length > 1) {
            mediaType = 'movie';
            tmdbId = Number.parseInt(movieId.substring(1), 10);
        }

        if (mediaType && tmdbId && !Number.isNaN(tmdbId)) {
            return this.getImdbIdForTmdb(tmdbId, mediaType);
        }

        // Fallback: use title type hint
        const fallbackMedia: 'movie' | 'tv' = isSeries ? 'tv' : 'movie';
        const fallbackId = Number.parseInt(movieId, 10);
        if (!Number.isNaN(fallbackId)) {
            return this.getImdbIdForTmdb(fallbackId, fallbackMedia);
        }

        return of(null);
    }

    getTvSeasonEpisodes(tmdbId: number): Observable<TvSeasonEpisodes[]> {
        return this.http.get<TmdbDetailResponse>(
            `${this.baseUrl}/tv/${tmdbId}?api_key=${this.apiKey}`
        ).pipe(
            map((response) => {
                const seasons = response.seasons || [];
                return seasons
                    .filter((season) => (season.season_number || 0) > 0 && (season.episode_count || 0) > 0)
                    .map((season) => ({
                        seasonNumber: season.season_number as number,
                        episodeCount: season.episode_count as number
                    }))
                    .sort((a, b) => a.seasonNumber - b.seasonNumber);
            }),
            catchError(() => of([]))
        );
    }
}
