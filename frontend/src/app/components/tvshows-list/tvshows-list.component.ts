import { Component, OnInit, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../services/api.service';
import { MovieListItem, GenreInfo } from '../../models/movie.model';
import { ApiResponse, PaginatedResponse } from '../../models/api-response.model';
import { TmdbService } from '../../services/tmdb.service';

@Component({
  selector: 'app-tvshows-list',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './tvshows-list.component.html',
  styleUrl: './tvshows-list.component.scss'
})
export class TvshowsListComponent implements OnInit {
  shows: MovieListItem[] = [];
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
    this.loadShows();
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

  loadShows(): void {
    this.loading = true;
    this.error = null;
    this.currentPage = 1;
    this.shows = [];
    this.hasMore = true;
    const minVotes = this.minRating > 0 ? 100 : undefined;

    this.apiService.getMovies(
      this.currentPage,
      this.pageSize,
      this.sortBy,
      'desc',
      this.searchQuery,
      'tvSeries,tvMiniSeries,tvMovie,tvEpisode',
      this.selectedGenre || undefined,
      this.minRating || undefined,
      minVotes
    )
      .subscribe({
        next: (response: ApiResponse<PaginatedResponse<MovieListItem>>) => {
          if (response.success && response.data) {
            this.shows = response.data.items;
            this.hasMore = (response.data as any).page < (response.data as any).totalPages;
            this.fetchPosters(this.shows);
          } else {
            this.error = response.message || 'Failed to load TV shows.';
          }
          this.loading = false;
        },
        error: (err: any) => {
          this.error = 'Error occurred while fetching TV shows.';
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
      'tvSeries,tvMiniSeries,tvMovie,tvEpisode',
      this.selectedGenre || undefined,
      this.minRating || undefined,
      minVotes
    )
      .subscribe({
        next: (response: ApiResponse<PaginatedResponse<MovieListItem>>) => {
          if (response.success && response.data) {
            this.shows = [...this.shows, ...response.data.items];
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

  private fetchPosters(showsList: MovieListItem[]): void {
    showsList.forEach(show => {
      if (!this.posters[show.id]) {
        this.tmdbService.getPosterByImdbId(show.id).subscribe(result => {
          if (result.poster) {
            this.posters[show.id] = result.poster;
          }
        });
      }
    });
  }

  onSortChange(): void {
    this.loadShows();
  }

  onFilterSubmit(): void {
    this.loadShows();
  }

  clearFilter(): void {
    this.searchQuery = '';
    this.selectedGenre = null;
    this.minRating = 0;
    this.loadShows();
  }
}
