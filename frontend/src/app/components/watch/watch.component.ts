import { AfterViewInit, Component, ElementRef, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { catchError, map, Observable, of, switchMap } from 'rxjs';

import { ApiService, WatchAccessInfo } from '../../services/api.service';
import { TmdbService, TvSeasonEpisodes, TvEpisodeDetail } from '../../services/tmdb.service';
import { TrustUrlPipe } from '../../pipes/trust-url.pipe';

@Component({
  selector: 'app-watch',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, TrustUrlPipe],
  templateUrl: './watch.component.html',
  styleUrls: ['./watch.component.scss']
})
export class WatchComponent implements OnInit, AfterViewInit, OnDestroy {
  private videoPlayer?: ElementRef<HTMLVideoElement>;
  @ViewChild('videoPlayer')
  set videoPlayerRef(ref: ElementRef<HTMLVideoElement> | undefined) {
    this.videoPlayer = ref;
    if (ref && this.streamUrl) {
      setTimeout(() => this.attachPlayer(), 0);
    }
  }

  loading = true;
  grantLoading = false;
  error: string | null = null;
  sourceNotFound = false;
  hasAccess = false;
  movieId = '';
  titleType = 'movie';
  isSeries = false;
  season = 1;
  episode = 1;
  seasons: number[] = [];
  currentEpisodes: number[] = [];
  private seasonEpisodeMeta: TvSeasonEpisodes[] = [];
  grantExpiresAt: string | null = null;

  // Stream / embed
  streamUrl: string | null = null;
  embedUrl: string | null = null;
  streamType = 'video/mp4';
  playerStatus: string | null = null;
  private hls: any = null;
  private loadTimeout: any = null;

  // Context metadata
  movieTitle: string | null = null;
  moviePoster: string | null = null;
  episodeDetail: TvEpisodeDetail | null = null;
  private tmdbTvId: number | null = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private apiService: ApiService,
    private tmdbService: TmdbService
  ) {}

  ngAfterViewInit(): void {
    if (this.streamUrl) {
      setTimeout(() => this.attachPlayer(), 0);
    }
  }

  ngOnDestroy(): void {
    this.cleanupPlayer();
  }

  ngOnInit(): void {
    this.movieId = this.route.snapshot.paramMap.get('movieId') || '';
    if (!this.movieId) {
      this.error = 'Invalid title.';
      this.loading = false;
      return;
    }

    const seasonParam = Number.parseInt(this.route.snapshot.queryParamMap.get('season') || '', 10);
    const episodeParam = Number.parseInt(this.route.snapshot.queryParamMap.get('episode') || '', 10);
    if (!Number.isNaN(seasonParam) && seasonParam > 0) {
      this.season = seasonParam;
    }
    if (!Number.isNaN(episodeParam) && episodeParam > 0) {
      this.episode = episodeParam;
    }

    this.apiService.canWatch(this.movieId).subscribe({
      next: (accessRes) => this.handleAccess(accessRes.data),
      error: (err) => {
        this.loading = false;
        this.error = err?.error?.error?.message || 'Unable to verify playback access.';
      }
    });
  }

  goToLibrary(): void {
    this.router.navigate(['/rentals']);
  }

  loadSeriesEpisode(): void {
    if (!this.isSeries) return;
    if (!Number.isFinite(this.season) || this.season < 1 || !Number.isFinite(this.episode) || this.episode < 1) {
      this.error = 'Season and episode must be positive numbers.';
      return;
    }
    this.syncSeriesQuery();
    // Load episode detail from TMDB
    if (this.tmdbTvId) {
      this.tmdbService.getTvEpisodeDetail(this.tmdbTvId, this.season, this.episode).subscribe((detail) => {
        this.episodeDetail = detail;
      });
    }
    this.loadPlaybackGrant();
  }

  onSeasonSelect(season: number): void {
    if (!this.isSeries || season < 1 || this.season === season) return;
    this.season = season;
    this.rebuildEpisodeOptions();
    this.episode = this.currentEpisodes.includes(this.episode) ? this.episode : 1;
    this.loadSeriesEpisode();
  }

  onEpisodeSelect(episode: number): void {
    if (!this.isSeries || episode < 1 || this.episode === episode) return;
    this.episode = episode;
    this.loadSeriesEpisode();
  }

  private handleAccess(access: WatchAccessInfo): void {
    this.hasAccess = !!access?.hasAccess;
    this.titleType = access?.titleType || 'movie';
    this.isSeries = !!access?.isSeries;

    if (!this.hasAccess) {
      this.error = 'This title is not in your purchased library.';
      this.loading = false;
      return;
    }

    // Load the title context (poster + name)
    this.loadTitleContext();

    if (this.isSeries) {
      this.loadSeriesMetadata();
    }

    this.loadPlaybackGrant();
  }

  /**
   * Load title name and poster from TMDB for the context header.
   */
  private loadTitleContext(): void {
    const id = this.movieId;

    if (id.startsWith('tmdb-movie-')) {
      const tmdbId = Number.parseInt(id.replace('tmdb-movie-', ''), 10);
      if (!Number.isNaN(tmdbId)) {
        this.tmdbService.getTmdbMediaDetail(tmdbId, 'movie').subscribe((res) => {
          this.movieTitle = res.detail.title;
          this.moviePoster = res.poster;
        });
      }
    } else if (id.startsWith('tmdb-tv-')) {
      const tmdbId = Number.parseInt(id.replace('tmdb-tv-', ''), 10);
      if (!Number.isNaN(tmdbId)) {
        this.tmdbTvId = tmdbId;
        this.tmdbService.getTmdbMediaDetail(tmdbId, 'tv').subscribe((res) => {
          this.movieTitle = res.detail.title;
          this.moviePoster = res.poster;
        });
      }
    } else if (id.startsWith('tt')) {
      // IMDb ID — use Cinemeta / TMDB find
      this.tmdbService.resolveTmdbFromImdbId(id).subscribe((resolved) => {
        if (resolved) {
          if (this.isSeries && resolved.mediaType === 'tv') this.tmdbTvId = resolved.tmdbId;
          this.tmdbService.getTmdbMediaDetail(resolved.tmdbId, resolved.mediaType).subscribe((res) => {
            this.movieTitle = res.detail.title;
            this.moviePoster = res.poster;
          });
        }
      });
    } else if (id.startsWith('s') && id.length > 1) {
      const tmdbId = Number.parseInt(id.substring(1), 10);
      if (!Number.isNaN(tmdbId)) {
        this.tmdbTvId = tmdbId;
        this.tmdbService.getTmdbMediaDetail(tmdbId, 'tv').subscribe((res) => {
          this.movieTitle = res.detail.title;
          this.moviePoster = res.poster;
        });
      }
    } else if (id.startsWith('m') && id.length > 1) {
      const tmdbId = Number.parseInt(id.substring(1), 10);
      if (!Number.isNaN(tmdbId)) {
        this.tmdbService.getTmdbMediaDetail(tmdbId, 'movie').subscribe((res) => {
          this.movieTitle = res.detail.title;
          this.moviePoster = res.poster;
        });
      }
    }
  }

  private loadPlaybackGrant(): void {
    this.loading = false;
    this.grantLoading = true;
    this.error = null;
    this.sourceNotFound = false;
    this.playerStatus = null;
    this.streamUrl = null;
    this.embedUrl = null;
    this.episodeDetail = null;
    this.cleanupPlayer();

    const season = this.isSeries ? this.season : undefined;
    const episode = this.isSeries ? this.episode : undefined;

    this.apiService.getPlaybackGrant(this.movieId, season, episode).subscribe({
      next: (grantRes) => {
        this.grantLoading = false;
        const grant = grantRes?.data;

        // Prefer embed URL (VidSrc iframe)
        if (grant?.embedUrl) {
          this.embedUrl = grant.embedUrl;
          // Load episode detail from TMDB if we have a series
          if (this.isSeries && this.tmdbTvId) {
            this.tmdbService.getTvEpisodeDetail(this.tmdbTvId, this.season, this.episode).subscribe((detail) => {
              this.episodeDetail = detail;
            });
          }
          return;
        }

        // Fallback: direct video stream (mp4 / hls)
        const selectedSource = grant?.streamUrl || grant?.fallbackUrl || null;
        if (!selectedSource) {
          this.error = null;
          this.sourceNotFound = true;
          return;
        }
        this.streamUrl = selectedSource;
        this.streamType = grant?.contentType || 'video/mp4';
        this.playerStatus = 'Loading stream...';
        setTimeout(() => this.attachPlayer(), 0);
      },
      error: (err) => {
        this.grantLoading = false;
        const code = err?.error?.error?.code;
        if (code === 'NO_SOURCE' || code === 'SOURCE_NOT_FOUND') {
          this.error = null;
          this.sourceNotFound = true;
          return;
        }
        this.error = err?.error?.error?.message || 'Unable to generate playback session.';
      }
    });
  }

  private loadSeriesMetadata(): void {
    this.resolveTmdbTvId(this.movieId)
      .pipe(
        switchMap((tmdbTvId) => {
          if (!tmdbTvId) {
            return of([] as TvSeasonEpisodes[]);
          }
          this.tmdbTvId = tmdbTvId;
          return this.tmdbService.getTvSeasonEpisodes(tmdbTvId);
        })
      )
      .subscribe((meta) => {
        this.seasonEpisodeMeta = meta;
        if (meta.length > 0) {
          this.seasons = meta.map((item) => item.seasonNumber);
          if (!this.seasons.includes(this.season)) {
            this.season = this.seasons[0];
          }
        } else {
          this.seasons = [this.season > 0 ? this.season : 1];
        }
        this.rebuildEpisodeOptions();
      });
  }

  private rebuildEpisodeOptions(): void {
    const meta = this.seasonEpisodeMeta.find((item) => item.seasonNumber === this.season);
    const count = Math.max(meta?.episodeCount || 20, 1);
    this.currentEpisodes = Array.from({ length: count }, (_, i) => i + 1);
    if (!this.currentEpisodes.includes(this.episode)) {
      this.episode = 1;
    }
  }

  private resolveTmdbTvId(id: string): Observable<number | null> {
    if (!id) return of(null);

    if (id.startsWith('tmdb-tv-')) {
      const parsed = Number.parseInt(id.replace('tmdb-tv-', ''), 10);
      return of(Number.isNaN(parsed) ? null : parsed);
    }

    if (id.startsWith('s')) {
      const parsed = Number.parseInt(id.substring(1), 10);
      return of(Number.isNaN(parsed) ? null : parsed);
    }

    if (id.startsWith('tt')) {
      return this.tmdbService.resolveTmdbFromImdbId(id).pipe(
        map((resolved) => (resolved?.mediaType === 'tv' ? resolved.tmdbId : null)),
        catchError(() => of(null))
      );
    }

    return of(null);
  }

  private attachPlayer(): void {
    if (!this.streamUrl || !this.videoPlayer?.nativeElement) return;

    const video = this.videoPlayer.nativeElement;
    const isHlsSource = this.streamType.toLowerCase().includes('mpegurl') || /\.m3u8(\?|$)/i.test(this.streamUrl);

    this.cleanupPlayer();
    this.loadTimeout = setTimeout(() => {
      this.sourceNotFound = true;
      this.error = null;
      this.playerStatus = 'Playback timed out.';
      this.streamUrl = null;
      this.cleanupPlayer();
    }, 12000);

    // Dynamic import of hls.js (only needed for direct HLS streams, not for VidSrc embed)
    if (isHlsSource) {
      // HLS support is currently disabled — real content uses the embed player
      clearTimeout(this.loadTimeout);
      this.sourceNotFound = true;
      this.playerStatus = null;
      return;
    }

    video.onloadedmetadata = () => {
      clearTimeout(this.loadTimeout);
      this.playerStatus = null;
    };
    video.onerror = () => {
      clearTimeout(this.loadTimeout);
      this.sourceNotFound = true;
      this.error = null;
      this.playerStatus = 'Playback failed to load.';
    };
    video.src = this.streamUrl;
    video.load();
    video.play().catch(() => undefined);
  }

  private cleanupPlayer(): void {
    clearTimeout(this.loadTimeout);
    this.loadTimeout = null;

    if (this.hls) {
      this.hls.destroy();
      this.hls = null;
    }

    if (this.videoPlayer?.nativeElement) {
      this.videoPlayer.nativeElement.pause();
      this.videoPlayer.nativeElement.removeAttribute('src');
      this.videoPlayer.nativeElement.load();
    }
  }

  private syncSeriesQuery(): void {
    if (!this.isSeries) return;
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { season: this.season, episode: this.episode },
      queryParamsHandling: 'merge',
      replaceUrl: true
    });
  }
}
