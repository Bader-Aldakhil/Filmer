import { Component, OnInit, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../services/api.service';
import { MovieListItem, GenreInfo } from '../../models/movie.model';
import { ApiResponse, PaginatedResponse } from '../../models/api-response.model';
import { TmdbService } from '../../services/tmdb.service';

@Component({
    selector: 'app-movies-list',
    standalone: true,
    imports: [CommonModule, RouterModule, FormsModule],
    templateUrl: './movies-list.component.html',
    styleUrls: ['./movies-list.component.scss']
})
export class MoviesListComponent implements OnInit {
    movies: MovieListItem[] = [];
    currentPage: number = 1;
    pageSize: number = 20;
    sortBy: string = 'popularity';
    searchQuery: string = '';
    selectedGenre: number | null = null;
    minRating: number = 0;
    genres: GenreInfo[] = [];
    loading: boolean = false;
    loadingMore: boolean = false;
    error: string | null = null;
    hasMore: boolean = true;

    // Store posters resolved via TMDB
    posters: { [key: string]: string } = {};

    constructor(
        private apiService: ApiService,
        private tmdbService: TmdbService
    ) { }

    ngOnInit(): void {
        this.loadGenres();
        this.loadMovies();
    }

    @HostListener('window:scroll')
    onScroll(): void {
        if (this.loadingMore || this.loading || !this.hasMore) return;
        const scrollPos = window.innerHeight + window.scrollY;
        const docHeight = document.documentElement.scrollHeight;
        if (scrollPos >= docHeight - 400) {
            this.loadMore();
        }
    }

    loadGenres(): void {
        this.apiService.getGenres().subscribe({
            next: (response) => {
                if (response.success && response.data) {
                    this.genres = response.data.items;
                }
            }
        });
    }

    loadMovies(): void {
        this.loading = true;
        this.error = null;
        this.currentPage = 1;
        this.movies = [];
        this.hasMore = true;
        const minVotes = this.minRating > 0 ? 100 : undefined;

        this.apiService.getMovies(
            this.currentPage,
            this.pageSize,
            this.sortBy,
            'desc',
            this.searchQuery,
            'movie',
            this.selectedGenre || undefined,
            this.minRating || undefined,
            minVotes
        )
            .subscribe({
                next: (response: ApiResponse<PaginatedResponse<MovieListItem>>) => {
                    if (response.success && response.data) {
                        this.movies = response.data.items;
                        this.hasMore = (response.data as any).page < (response.data as any).totalPages;
                        this.fetchPosters(this.movies);
                    } else {
                        this.error = response.message || 'Failed to load movies.';
                    }
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
        this.currentPage++;
        const minVotes = this.minRating > 0 ? 100 : undefined;

        this.apiService.getMovies(
            this.currentPage,
            this.pageSize,
            this.sortBy,
            'desc',
            this.searchQuery,
            'movie',
            this.selectedGenre || undefined,
            this.minRating || undefined,
            minVotes
        )
            .subscribe({
                next: (response: ApiResponse<PaginatedResponse<MovieListItem>>) => {
                    if (response.success && response.data) {
                        this.movies = [...this.movies, ...response.data.items];
                        this.hasMore = (response.data as any).page < (response.data as any).totalPages;
                        this.fetchPosters(response.data.items);
                    }
                    this.loadingMore = false;
                },
                error: (err: any) => {
                    console.error(err);
                    this.loadingMore = false;
                }
            });
    }

    private fetchPosters(moviesList: MovieListItem[]): void {
        moviesList.forEach(movie => {
            if (!this.posters[movie.id]) {
                this.tmdbService.getPosterByImdbId(movie.id).subscribe(result => {
                    if (result.poster) {
                        this.posters[movie.id] = result.poster;
                    }
                });
            }
        });
    }

    onSortChange(): void {
        this.loadMovies();
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
}
