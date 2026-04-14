import { Component, HostListener, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MovieListItem, GenreInfo } from '../../models/movie.model';
import { TmdbService } from '../../services/tmdb.service';

@Component({
    selector: 'app-movies-list',
    standalone: true,
    imports: [CommonModule, RouterModule, FormsModule],
    templateUrl: './movies-list.component.html',
    styleUrls: ['./movies-list.component.scss']
})
export class MoviesListComponent implements OnInit, OnDestroy {
    allMovies: MovieListItem[] = [];
    movies: MovieListItem[] = [];
    currentPage: number = 1;
    pageSize: number = 20;
    searchQuery: string = '';
    selectedGenre: number | null = null;
    minRating: number = 0;
    genres: GenreInfo[] = [];
    loading: boolean = false;
    loadingMore: boolean = false;
    showLoadingMore: boolean = false;
    error: string | null = null;
    hasMore: boolean = true;
    private loadingMoreIndicatorTimer: ReturnType<typeof setTimeout> | null = null;
    private lastLoadMoreAt: number = 0;
    private scrollRafPending: boolean = false;
    private lastScrollTop: number = 0;

    // Store posters resolved via TMDB
    posters: { [key: string]: string } = {};
    private imdbRatingCache: { [tmdbKey: string]: number | undefined } = {};

    private computeWeightedScore(item: MovieListItem): number {
        const rating = item.rating ?? 0;
        const votes = item.numVotes ?? 0;
        const minVotes = this.selectedGenre ? 200 : 1000;
        const baseline = 6.9;
        if (!rating) {
            return 0;
        }
        return (votes / (votes + minVotes)) * rating + (minVotes / (votes + minVotes)) * baseline;
    }

    private sortByWeightedScore(items: MovieListItem[]): MovieListItem[] {
        return [...items].sort((a, b) => {
            const scoreDiff = this.computeWeightedScore(b) - this.computeWeightedScore(a);
            if (scoreDiff !== 0) {
                return scoreDiff;
            }
            return (b.numVotes ?? 0) - (a.numVotes ?? 0);
        });
    }

    private applyMinRatingFilter(items: MovieListItem[]): MovieListItem[] {
        if (!this.minRating || this.minRating <= 0) {
            return items;
        }

        return items.filter(movie => (movie.rating ?? 0) >= this.minRating);
    }

    private refreshVisibleMovies(): void {
        const ranked = this.sortByWeightedScore(this.allMovies);
        this.movies = this.applyMinRatingFilter(ranked);
    }

    constructor(
        private tmdbService: TmdbService
    ) { }

    ngOnInit(): void {
        this.loadGenres();
        this.loadMovies();
    }

    ngOnDestroy(): void {
        if (this.loadingMoreIndicatorTimer) {
            clearTimeout(this.loadingMoreIndicatorTimer);
            this.loadingMoreIndicatorTimer = null;
        }
    }

    @HostListener('window:scroll')
    onWindowScroll(): void {
        if (this.scrollRafPending) {
            return;
        }

        this.scrollRafPending = true;
        requestAnimationFrame(() => {
            this.scrollRafPending = false;
            this.tryLoadMoreByScroll();
        });
    }

    private tryLoadMoreByScroll(): void {
        if (this.loadingMore || this.loading || !this.hasMore) {
            return;
        }
        if (Date.now() - this.lastLoadMoreAt < 700) {
            return;
        }

        const scrollTop = window.scrollY || document.documentElement.scrollTop || 0;
        if (scrollTop <= this.lastScrollTop) {
            this.lastScrollTop = scrollTop;
            return;
        }
        this.lastScrollTop = scrollTop;

        const maxScroll = document.documentElement.scrollHeight - window.innerHeight;
        if (maxScroll <= 0) {
            return;
        }

        const distanceToBottom = maxScroll - scrollTop;
        if (distanceToBottom <= 650) {
            this.loadMore();
        }
    }

    loadGenres(): void {
        this.tmdbService.getMovieGenres().subscribe({
            next: (genres) => {
                this.genres = genres;
            }
        });
    }

    loadMovies(): void {
        this.loading = true;
        this.error = null;
        this.currentPage = 1;
        this.allMovies = [];
        this.movies = [];
        this.hasMore = true;
        this.lastScrollTop = 0;
        const serverMinRating = undefined;
        this.tmdbService.discoverMovies(
            this.currentPage,
            this.searchQuery,
            this.selectedGenre || undefined,
            serverMinRating,
            'top_rated'
        )
            .subscribe({
                next: (response) => {
                    this.allMovies = response.items;
                    this.posters = { ...this.posters, ...response.posters };
                    this.hasMore = response.page < response.totalPages;
                    this.enrichVisibleRatings(this.allMovies);
                    this.refreshVisibleMovies();
                    this.loading = false;
                },
                error: (err: any) => {
                    this.error = 'Error occurred while fetching movies.';
                    console.error(err);
                    this.loading = false;
                }
            });
    }

    loadMore(): void {
        if (!this.hasMore || this.loadingMore) return;
        this.loadingMore = true;
        this.lastLoadMoreAt = Date.now();
        this.loadingMoreIndicatorTimer = setTimeout(() => {
            if (this.loadingMore) {
                this.showLoadingMore = true;
            }
        }, 180);
        this.currentPage++;
        const serverMinRating = undefined;
        this.tmdbService.discoverMovies(
            this.currentPage,
            this.searchQuery,
            this.selectedGenre || undefined,
            serverMinRating,
            'top_rated'
        )
            .subscribe({
                next: (response) => {
                    const merged = [...this.allMovies, ...response.items];
                    const byId = new Map(merged.map(movie => [movie.id, movie]));
                    this.allMovies = Array.from(byId.values());
                    this.posters = { ...this.posters, ...response.posters };
                    this.hasMore = response.page < response.totalPages;
                    this.enrichVisibleRatings(response.items);
                    this.refreshVisibleMovies();
                    this.loadingMore = false;
                    this.showLoadingMore = false;
                    if (this.loadingMoreIndicatorTimer) {
                        clearTimeout(this.loadingMoreIndicatorTimer);
                        this.loadingMoreIndicatorTimer = null;
                    }
                },
                error: (err: any) => {
                    console.error(err);
                    this.loadingMore = false;
                    this.showLoadingMore = false;
                    if (this.loadingMoreIndicatorTimer) {
                        clearTimeout(this.loadingMoreIndicatorTimer);
                        this.loadingMoreIndicatorTimer = null;
                    }
                }
            });
    }

    trackByMovieId(_index: number, movie: MovieListItem): string {
        return movie.id;
    }

    onFilterSubmit(): void {
        this.loadMovies();
    }

    clearFilter(): void {
        this.searchQuery = '';
        this.selectedGenre = null;
        this.minRating = 0;
        this.loadMovies();
    }

    private enrichVisibleRatings(items: MovieListItem[]): void {
        for (const item of items) {
            if (!item.id.startsWith('tmdb-movie-')) {
                continue;
            }

            const tmdbId = Number.parseInt(item.id.replace('tmdb-movie-', ''), 10);
            if (Number.isNaN(tmdbId)) {
                continue;
            }

            const cacheKey = `movie:${tmdbId}`;
            if (this.imdbRatingCache[cacheKey] !== undefined) {
                item.rating = this.imdbRatingCache[cacheKey];
                continue;
            }

            this.tmdbService.getImdbRatingForTmdb(tmdbId, 'movie').subscribe((imdbRating) => {
                if (imdbRating === undefined) {
                    return;
                }
                this.imdbRatingCache[cacheKey] = imdbRating;
                item.rating = imdbRating;
            });
        }
    }
}
