import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule, ParamMap } from '@angular/router';
import { ApiService } from '../../services/api.service';
import { MovieDetail } from '../../models/movie.model';
import { ApiResponse } from '../../models/api-response.model';
import { FavoritesService, FavoriteItem } from '../../services/favorites.service';
import { TmdbService } from '../../services/tmdb.service';

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
        private tmdbService: TmdbService
    ) { }

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

        this.apiService.getMovieById(id).subscribe({
            next: (response: ApiResponse<MovieDetail>) => {
                if (response.success && response.data) {
                    this.movie = response.data;
                    this.loadTmdbData(this.movie.id);
                    this.loadCastPhotos(this.movie.id);
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
}
