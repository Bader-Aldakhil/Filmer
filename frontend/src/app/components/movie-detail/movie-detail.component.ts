import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule, ParamMap } from '@angular/router';
import { ApiService } from '../../services/api.service';
import { MovieDetail } from '../../models/movie.model';
import { ApiResponse } from '../../models/api-response.model';
import { FavoritesService, FavoriteItem } from '../../services/favorites.service';
import { TmdbService } from '../../services/tmdb.service';
import { Router } from '@angular/router';
import { Observable, of, switchMap, map, catchError } from 'rxjs';

@Component({
    selector: 'app-movie-detail',
    standalone: true,
    imports: [CommonModule, RouterModule],
    templateUrl: './movie-detail.component.html',
    styleUrls: ['./movie-detail.component.scss']
})
export class MovieDetailComponent implements OnInit {
    movie: MovieDetail | null = null;
    loading: boolean = false;
    error: string | null = null;
    isTvShow: boolean = false;
    voteCountLabel: string | null = 'IMDb votes';

    // TMDB poster and backdrop
    tmdbPoster: string | null = null;
    tmdbBackdrop: string | null = null;
    tmdbDescription: string | null = null;

    // Cast photos keyed by actor name
    castPhotos: { [name: string]: string } = {};

    constructor(
        private route: ActivatedRoute,
        private apiService: ApiService,
        public favoritesService: FavoritesService,
        private tmdbService: TmdbService,
        private router: Router
    ) { }

    cartLoading = false;
    cartMessage: string | null = null;
    isInCart = false;
    hasWatchAccess = false;
    resolvedCatalogMovieId: string | null = null;

    ngOnInit(): void {
        this.isTvShow = this.route.snapshot.url[0]?.path === 'tvshows';
        this.route.paramMap.subscribe((params: ParamMap) => {
            const id = params.get('id');
            if (id) {
                this.loadMovie(id);
            } else {
                this.error = 'Invalid movie ID';
            }
        });
    }

    loadMovie(id: string): void {
        this.loading = true;
        this.error = null;

        if (id.startsWith('tmdb-movie-') || id.startsWith('tmdb-tv-')) {
            const isTv = id.startsWith('tmdb-tv-');
            const tmdbId = Number.parseInt(id.replace('tmdb-movie-', '').replace('tmdb-tv-', ''), 10);

            if (Number.isNaN(tmdbId)) {
                this.error = 'Invalid TMDB ID';
                this.loading = false;
                return;
            }

            this.tmdbService.getTmdbMediaDetail(tmdbId, isTv ? 'tv' : 'movie').subscribe({
                next: (result) => {
                    this.movie = result.detail;
                    this.tmdbPoster = result.poster;
                    this.tmdbBackdrop = result.backdrop;
                    this.tmdbDescription = result.overview;
                    this.castPhotos = result.castPhotos;

                    this.tmdbService.getImdbRatingForTmdb(tmdbId, isTv ? 'tv' : 'movie').subscribe(imdbRating => {
                        if (imdbRating !== undefined && this.movie) {
                            this.movie.rating = imdbRating;
                        }
                    });

                    this.tmdbService.getImdbVotesForTmdb(tmdbId, isTv ? 'tv' : 'movie').subscribe(imdbVotes => {
                        if (!this.movie) {
                            return;
                        }
                        if (imdbVotes !== undefined) {
                            this.movie.numVotes = imdbVotes;
                            this.voteCountLabel = 'IMDb votes';
                        } else {
                            this.movie.numVotes = undefined;
                            this.voteCountLabel = null;
                        }
                    });
                    this.syncInCartState();
                    this.loading = false;
                },
                error: (err: any) => {
                    this.error = 'Failed to load movie details.';
                    console.error(err);
                    this.loading = false;
                }
            });
            return;
        }

        this.apiService.getMovieById(id).subscribe({
            next: (response: ApiResponse<MovieDetail>) => {
                if (response.success && response.data) {
                    this.movie = response.data;
                    this.voteCountLabel = this.movie?.numVotes ? 'IMDb votes' : null;
                    this.loadTmdbData(this.movie.id);
                    this.loadCastPhotos(this.movie.id);
                    this.syncInCartState();
                } else {
                    this.error = response.message || 'Movie not found';
                }
                this.loading = false;
            },
            error: (err: any) => {
                this.error = 'Failed to load movie details.';
                console.error(err);
                this.loading = false;
            }
        });
    }

    private loadTmdbData(imdbId: string): void {
        this.tmdbService.getPosterByImdbId(imdbId).subscribe({
            next: (result) => {
                this.tmdbPoster = result.poster;
                this.tmdbBackdrop = result.backdrop;
                this.tmdbDescription = result.overview;
            }
        });
    }

    private loadCastPhotos(imdbId: string): void {
        this.tmdbService.getCastByImdbId(imdbId).subscribe({
            next: (cast) => {
                if (cast && cast.length > 0) {
                    cast.forEach(member => {
                        const photoUrl = this.tmdbService.getProfileImageUrl(member.profile_path);
                        if (photoUrl) {
                            this.castPhotos[member.name] = photoUrl;
                        }
                    });
                }
                // Fallback: initials-based avatars for any stars not found in TMDB
                if (this.movie?.stars) {
                    this.movie.stars.forEach(star => {
                        if (!this.castPhotos[star.name]) {
                            this.castPhotos[star.name] = `https://ui-avatars.com/api/?name=${encodeURIComponent(star.name)}&background=333&color=fff&size=185&bold=true&font-size=0.4`;
                        }
                    });
                }
            },
            error: () => {
                if (this.movie?.stars) {
                    this.movie.stars.forEach(star => {
                        this.castPhotos[star.name] = `https://ui-avatars.com/api/?name=${encodeURIComponent(star.name)}&background=333&color=fff&size=185&bold=true&font-size=0.4`;
                    });
                }
            }
        });
    }

    toggleFavorite(): void {
        if (!this.movie) return;

        const item: FavoriteItem = {
            id: this.movie.id,
            title: this.movie.title,
            year: this.movie.year,
            poster: this.tmdbPoster || undefined
        };

        this.favoritesService.toggleFavorite(item);
    }

    rentNow(): void {
        if (!this.movie) {
            return;
        }
        if (this.isInCart) {
            return;
        }

        this.cartLoading = true;
        this.cartMessage = null;

        this.resolveCatalogMovieId(this.movie.id).subscribe({
            next: (catalogId) => {
                if (!catalogId) {
                    this.cartLoading = false;
                    this.cartMessage = 'This title is not available for rental in our catalog yet.';
                    return;
                }
                this.resolvedCatalogMovieId = catalogId;
                this.addToCartByMovieId(catalogId);
            },
            error: () => {
                this.cartLoading = false;
                this.cartMessage = 'Could not add this title to cart.';
            }
        });
    }

    private addToCartByMovieId(movieId: string): void {
        const payload = this.movie
            ? {
                title: this.movie.title,
                year: this.movie.year,
                titleType: this.isTvShow ? 'tvSeries' : 'movie',
                rating: this.movie.rating,
                numVotes: this.movie.numVotes
            }
            : undefined;

        this.apiService.addToCart(movieId, 1, payload).subscribe({
            next: () => {
                this.cartLoading = false;
                this.cartMessage = 'Added to cart.';
                this.isInCart = true;
            },
            error: (err) => {
                this.cartLoading = false;
                if (err?.status === 401) {
                    this.router.navigate(['/auth']);
                    return;
                }

                if (err?.status === 404) {
                    this.cartMessage = 'This title is not available for rental in our catalog yet.';
                    return;
                }

                this.cartMessage = err?.error?.error?.message || 'Could not add to cart.';
            }
        });
    }

    private resolveCatalogMovieId(movieId: string): Observable<string | null> {
        if (movieId.startsWith('tmdb-movie-') || movieId.startsWith('tmdb-tv-')) {
            const mediaType: 'movie' | 'tv' = movieId.startsWith('tmdb-tv-') ? 'tv' : 'movie';
            const tmdbId = Number.parseInt(movieId.replace('tmdb-movie-', '').replace('tmdb-tv-', ''), 10);
            if (Number.isNaN(tmdbId)) {
                return of(null);
            }
            return this.tmdbService.getImdbIdForTmdb(tmdbId, mediaType).pipe(
                map((imdbId) => imdbId || this.buildSyntheticCatalogId(tmdbId, mediaType))
            );
        }
        return of(movieId);
    }

    private buildSyntheticCatalogId(tmdbId: number, mediaType: 'movie' | 'tv'): string {
        const prefix = mediaType === 'tv' ? 's' : 'm';
        const normalized = String(Math.abs(tmdbId)).slice(-9);
        return `${prefix}${normalized}`;
    }

    private syncInCartState(): void {
        if (!this.movie) {
            this.isInCart = false;
            this.hasWatchAccess = false;
            return;
        }

        this.resolveCatalogMovieId(this.movie.id)
            .pipe(
                switchMap((catalogId) => {
                    this.resolvedCatalogMovieId = catalogId;
                    if (!catalogId) {
                        return of(false);
                    }
                    return this.apiService.getCart().pipe(
                        switchMap((res) => {
                            this.isInCart = (res?.data?.items || []).some(item => item.movieId === catalogId);
                            return this.apiService.canWatch(catalogId).pipe(
                                map((watchRes) => !!watchRes?.data?.hasAccess),
                                catchError(() => of(false))
                            );
                        }),
                        catchError(() => of(false))
                    );
                })
            )
            .subscribe((hasAccess) => {
                this.hasWatchAccess = hasAccess;
            });
    }
}
