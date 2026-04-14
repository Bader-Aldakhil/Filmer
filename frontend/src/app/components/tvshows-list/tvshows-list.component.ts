import { Component, HostListener, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MovieListItem, GenreInfo } from '../../models/movie.model';
import { TmdbService } from '../../services/tmdb.service';

@Component({
  selector: 'app-tvshows-list',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './tvshows-list.component.html',
  styleUrl: './tvshows-list.component.scss'
})
export class TvshowsListComponent implements OnInit, OnDestroy {
  allShows: MovieListItem[] = [];
  shows: MovieListItem[] = [];
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

  private computeWeightedScore(item: MovieListItem): number {
    const rating = item.rating ?? 0;
    const votes = item.numVotes ?? 0;
    const minVotes = this.selectedGenre ? 100 : 500;
    const baseline = 7.0;
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

    return items.filter(show => (show.rating ?? 0) >= this.minRating);
  }

  private refreshVisibleShows(): void {
    const ranked = this.sortByWeightedScore(this.allShows);
    this.shows = this.applyMinRatingFilter(ranked);
  }

  constructor(
    private tmdbService: TmdbService
  ) { }

  ngOnInit(): void {
    this.loadGenres();
    this.loadShows();
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
    this.tmdbService.getTvGenres().subscribe({
      next: (genres) => {
        this.genres = genres;
      }
    });
  }

  loadShows(): void {
    this.loading = true;
    this.error = null;
    this.currentPage = 1;
    this.allShows = [];
    this.shows = [];
    this.hasMore = true;
    this.lastScrollTop = 0;
    const serverMinRating = undefined;
    this.tmdbService.discoverTvShows(
      this.currentPage,
      this.searchQuery,
      this.selectedGenre || undefined,
      serverMinRating,
      'top_rated'
    )
      .subscribe({
        next: (response) => {
          this.allShows = response.items;
          this.posters = { ...this.posters, ...response.posters };
          this.hasMore = response.page < response.totalPages;
          this.refreshVisibleShows();
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
    this.lastLoadMoreAt = Date.now();
    this.loadingMoreIndicatorTimer = setTimeout(() => {
      if (this.loadingMore) {
        this.showLoadingMore = true;
      }
    }, 180);
    this.currentPage++;
    const serverMinRating = undefined;
    this.tmdbService.discoverTvShows(
      this.currentPage,
      this.searchQuery,
      this.selectedGenre || undefined,
      serverMinRating,
      'top_rated'
    )
      .subscribe({
        next: (response) => {
          const merged = [...this.allShows, ...response.items];
          const byId = new Map(merged.map(show => [show.id, show]));
          this.allShows = Array.from(byId.values());
          this.posters = { ...this.posters, ...response.posters };
          this.hasMore = response.page < response.totalPages;
          this.refreshVisibleShows();
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

  trackByShowId(_index: number, show: MovieListItem): string {
    return show.id;
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
