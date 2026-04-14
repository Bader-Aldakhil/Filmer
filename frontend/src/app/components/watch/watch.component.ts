import { AfterViewInit, Component, ElementRef, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import Hls from 'hls.js/dist/hls.min.js';
import { catchError, map, Observable, of, switchMap } from 'rxjs';

import { ApiService, WatchAccessInfo } from '../../services/api.service';
import { TmdbService, TvSeasonEpisodes } from '../../services/tmdb.service';

@Component({
  selector: 'app-watch',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './watch.component.html',
  styleUrls: ['./watch.component.scss']
})
export class WatchComponent implements OnInit, AfterViewInit, OnDestroy {
  private videoPlayer?: ElementRef<HTMLVideoElement>;
  @ViewChild('videoPlayer')
  set videoPlayerRef(ref: ElementRef<HTMLVideoElement> | undefined) {
    this.videoPlayer = ref;
    if (ref && this.streamUrl) {
      // Attach only after the DOM has materialized the video node.
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
  streamUrl: string | null = null;
  streamType = 'video/mp4';
  playerStatus: string | null = null;
  private hls: any = null;
  private loadTimeout: any = null;

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
    if (!this.isSeries) {
      return;
    }
    if (!Number.isFinite(this.season) || this.season < 1 || !Number.isFinite(this.episode) || this.episode < 1) {
      this.error = 'Season and episode must be positive numbers.';
      return;
    }
    this.syncSeriesQuery();
    this.loadPlaybackGrant();
  }

  onSeasonSelect(season: number): void {
    if (!this.isSeries || season < 1 || this.season === season) {
      return;
    }

    this.season = season;
    this.rebuildEpisodeOptions();
    this.episode = this.currentEpisodes.includes(this.episode) ? this.episode : 1;
    this.loadSeriesEpisode();
  }

  onEpisodeSelect(episode: number): void {
    if (!this.isSeries || episode < 1 || this.episode === episode) {
      return;
    }
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

    if (this.isSeries) {
      this.loadSeriesMetadata();
    }

    this.loadPlaybackGrant();
  }

  private loadPlaybackGrant(): void {
    this.loading = false;
    this.grantLoading = true;
    this.error = null;
    this.sourceNotFound = false;
    this.playerStatus = null;
    this.streamUrl = null;
    this.cleanupPlayer();

    const season = this.isSeries ? this.season : undefined;
    const episode = this.isSeries ? this.episode : undefined;

    this.apiService.getPlaybackGrant(this.movieId, season, episode).subscribe({
      next: (grantRes) => {
        this.grantLoading = false;
        const grant = grantRes?.data;
        const selectedSource = grant?.streamUrl || grant?.fallbackUrl || null;
        if (!selectedSource) {
          this.error = null;
          this.sourceNotFound = true;
          return;
        }
        this.streamUrl = selectedSource;
        this.streamType = grant.contentType || 'video/mp4';
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
    if (!id) {
      return of(null);
    }

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
    if (!this.streamUrl || !this.videoPlayer?.nativeElement) {
      return;
    }

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

    if (isHlsSource && Hls.isSupported()) {
      this.hls = new Hls({
        enableWorker: true,
        lowLatencyMode: true
      });
      this.hls.loadSource(this.streamUrl);
      this.hls.attachMedia(video);
      this.hls.on(Hls.Events.MANIFEST_PARSED, () => {
        clearTimeout(this.loadTimeout);
        this.playerStatus = null;
        video.play().catch(() => undefined);
      });
      this.hls.on(Hls.Events.ERROR, (_event: any, data: any) => {
        if (!data.fatal) {
          return;
        }
        if (data.type === Hls.ErrorTypes.NETWORK_ERROR) {
          this.hls?.startLoad();
          return;
        }
        if (data.type === Hls.ErrorTypes.MEDIA_ERROR) {
          this.hls?.recoverMediaError();
          return;
        }
        this.sourceNotFound = true;
        this.error = null;
        this.playerStatus = 'Playback failed to load.';
        this.streamUrl = null;
        this.cleanupPlayer();
      });
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
    if (!this.isSeries) {
      return;
    }
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: {
        season: this.season,
        episode: this.episode
      },
      queryParamsHandling: 'merge',
      replaceUrl: true
    });
  }
}
