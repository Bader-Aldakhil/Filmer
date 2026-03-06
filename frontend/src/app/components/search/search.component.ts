import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../services/api.service';
import { MovieListItem } from '../../models/movie.model';
import { CinemetaService } from '../../services/cinemeta.service';

@Component({
    selector: 'app-search',
    standalone: true,
    imports: [CommonModule, RouterModule, FormsModule],
    templateUrl: './search.component.html',
    styleUrls: ['./search.component.scss']
})
export class SearchComponent {
    query: string = '';
    titleParams: string = '';
    year: number | null = null;
    yearFrom: number | null = null;
    yearTo: number | null = null;
    director: string = '';
    star: string = '';
    genreId: number | null = null;

    movies: MovieListItem[] = [];
    currentPage: number = 1;
    pageSize: number = 20;
    totalPages: number = 0;
    totalElements: number = 0;
    sortBy: string = 'title';
    order: string = 'asc';

    loading: boolean = false;
    hasSearched: boolean = false;
    error: string | null = null;
    Math = Math;

    // Store localized posters dynamically resolved via Cinemeta
    posters: { [key: string]: string } = {};

    constructor(
        private apiService: ApiService,
        private cinemetaService: CinemetaService
    ) { }

    onSearchSubmit(): void {
        if (!this.query && !this.titleParams && !this.year && !this.yearFrom && !this.yearTo && !this.director && !this.star && !this.genreId) {
            this.error = 'Please enter at least one search criteria.';
            return;
        }
        this.currentPage = 1;
        this.performSearch();
    }

    performSearch(): void {
        this.loading = true;
        this.error = null;
        this.hasSearched = true;

        const params: Record<string, string | number> = {
            page: this.currentPage,
            size: this.pageSize,
            sortBy: this.sortBy,
            order: this.order
        };

        if (this.query) params['query'] = this.query;
        if (this.titleParams) params['title'] = this.titleParams;
        if (this.year) params['year'] = this.year;
        if (this.yearFrom) params['yearFrom'] = this.yearFrom;
        if (this.yearTo) params['yearTo'] = this.yearTo;
        if (this.director) params['director'] = this.director;
        if (this.star) params['star'] = this.star;
        if (this.genreId) params['genreId'] = this.genreId;

        this.apiService.searchMovies(params).subscribe({
            next: (response) => {
                if (response.success && response.data) {
                    this.movies = response.data.items;
                    this.currentPage = response.data.page;
                    this.pageSize = response.data.size;
                    this.totalElements = response.data.totalElements;
                    this.totalPages = this.totalElements > 0 ? Math.ceil(this.totalElements / this.pageSize) : 0;
                    this.fetchPosters();
                } else {
                    this.error = response.message || 'Search failed.';
                }
                this.loading = false;
            },
            error: (err) => {
                this.error = 'An error occurred during search.';
                console.error(err);
                this.loading = false;
            }
        });
    }

    private fetchPosters(): void {
        this.movies.forEach(movie => {
            this.cinemetaService.getMovieMeta(movie.id).subscribe(meta => {
                if (meta?.poster) {
                    this.posters[movie.id] = meta.poster;
                }
            });
        });
    }

    onPageChange(page: number): void {
        if (page >= 1 && page <= this.totalPages) {
            this.currentPage = page;
            this.performSearch();
        }
    }

    onSortChange(): void {
        if (this.hasSearched) {
            this.currentPage = 1;
            this.performSearch();
        }
    }

    resetForm(): void {
        this.query = '';
        this.titleParams = '';
        this.year = null;
        this.yearFrom = null;
        this.yearTo = null;
        this.director = '';
        this.star = '';
        this.genreId = null;
        this.movies = [];
        this.hasSearched = false;
        this.error = null;
        this.currentPage = 1;
        this.totalPages = 0;
    }
}
