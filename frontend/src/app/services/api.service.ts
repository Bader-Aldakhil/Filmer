import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiResponse, PaginatedResponse } from '../models/api-response.model';
import { MovieListItem, MovieDetail, GenreInfo } from '../models/movie.model';
import { StarListItem, StarDetail } from '../models/star.model';
import { CartData, LibraryItem, OrderDetail, OrderListItem } from '../models/auth.model';

export interface AddToCartPayload {
  title?: string;
  year?: number;
  titleType?: string;
  rating?: number;
  numVotes?: number;
}

export interface WatchAccessInfo {
  movieId: string;
  hasAccess: boolean;
  titleType: string;
  isSeries: boolean;
}

export interface PlaybackGrant {
  movieId: string;
  titleType: string;
  season?: number;
  episode?: number;
  provider: string;
  streamUrl: string;
  contentType?: string;
  fallbackUrl?: string;
  expiresAt: string;
}
/**
 * API Service for communicating with the Filmer backend.
 * All HTTP requests to the backend are routed through this service.
 */
@Injectable({
  providedIn: 'root'
})
export class ApiService {
  // Base URL for the backend API
  // In production, this should be moved to an environment configuration
  private readonly API_BASE_URL = 'http://localhost:8080/api/v1';
  private readonly WITH_CREDENTIALS = { withCredentials: true };

  constructor(private http: HttpClient) { }

  /**
   * Test database connectivity by calling the /api/v1/health/db endpoint.
   * @returns Observable with the health check response
   */
  testDatabaseConnectivity(): Observable<any> {
    return this.http.get(`${this.API_BASE_URL}/health/db`);
  }

  /**
   * Get basic health status of the API.
   * @returns Observable with the health status response
   */
  getHealth(): Observable<any> {
    return this.http.get(`${this.API_BASE_URL}/health`);
  }

  // Movies endpoints
  getMovies(page: number = 1, size: number = 20, sortBy: string = 'popularity', order: string = 'desc', title?: string, type?: string, genreId?: number, minRating?: number, minVotes?: number): Observable<ApiResponse<PaginatedResponse<MovieListItem>>> {
    let params: Record<string, string | number> = { page, size, sortBy, order };
    if (title) {
      params['title'] = title;
    }
    if (type) {
      params['type'] = type;
    }
    if (genreId) {
      params['genreId'] = genreId;
    }
    if (minRating) {
      params['minRating'] = minRating;
    }
    if (minVotes) {
      params['minVotes'] = minVotes;
    }
    return this.http.get<ApiResponse<PaginatedResponse<MovieListItem>>>(`${this.API_BASE_URL}/movies`, { params });
  }

  getMovieById(id: string): Observable<ApiResponse<MovieDetail>> {
    return this.http.get<ApiResponse<MovieDetail>>(`${this.API_BASE_URL}/movies/${id}`);
  }

  // Genres endpoints
  getGenres(): Observable<ApiResponse<{ items: GenreInfo[] }>> {
    return this.http.get<ApiResponse<{ items: GenreInfo[] }>>(`${this.API_BASE_URL}/genres`);
  }

  // Stars endpoints
  getStars(page: number = 1, size: number = 20, sortBy: string = 'name', order: string = 'asc', name?: string): Observable<ApiResponse<PaginatedResponse<StarListItem>>> {
    let params: Record<string, string | number> = { page, size, sortBy, order };
    if (name) {
      params['name'] = name;
    }
    return this.http.get<ApiResponse<PaginatedResponse<StarListItem>>>(`${this.API_BASE_URL}/stars`, { params });
  }

  getStarById(id: string): Observable<ApiResponse<StarDetail>> {
    return this.http.get<ApiResponse<StarDetail>>(`${this.API_BASE_URL}/stars/${id}`);
  }

  // Search endpoints
  searchMovies(params: Record<string, string | number>): Observable<ApiResponse<PaginatedResponse<MovieListItem>>> {
    return this.http.get<ApiResponse<PaginatedResponse<MovieListItem>>>(`${this.API_BASE_URL}/search/movies`, { params });
  }

  login(email: string, password: string): Observable<any> {
    return this.http.post(`${this.API_BASE_URL}/auth/login`, { email, password }, this.WITH_CREDENTIALS);
  }

  register(firstName: string, lastName: string, email: string, password: string): Observable<any> {
    return this.http.post(`${this.API_BASE_URL}/auth/register`, { firstName, lastName, email, password }, this.WITH_CREDENTIALS);
  }

  logout(): Observable<any> {
    return this.http.post(`${this.API_BASE_URL}/auth/logout`, {}, this.WITH_CREDENTIALS);
  }

  checkSession(): Observable<any> {
    return this.http.get(`${this.API_BASE_URL}/auth/session`, this.WITH_CREDENTIALS);
  }

  getCart(): Observable<ApiResponse<CartData>> {
    return this.http.get<ApiResponse<CartData>>(`${this.API_BASE_URL}/cart`, this.WITH_CREDENTIALS);
  }

  addToCart(movieId: string, quantity: number = 1, payload?: AddToCartPayload): Observable<any> {
    return this.http.post(`${this.API_BASE_URL}/cart/items`, { movieId, quantity, ...payload }, this.WITH_CREDENTIALS);
  }

  updateCartItem(movieId: string, quantity: number): Observable<any> {
    return this.http.put(`${this.API_BASE_URL}/cart/items/${movieId}`, { quantity }, this.WITH_CREDENTIALS);
  }

  removeFromCart(movieId: string): Observable<any> {
    return this.http.delete(`${this.API_BASE_URL}/cart/items/${movieId}`, this.WITH_CREDENTIALS);
  }

  clearCart(): Observable<any> {
    return this.http.delete(`${this.API_BASE_URL}/cart`, this.WITH_CREDENTIALS);
  }

  checkout(creditCardId: string, firstName: string, lastName: string, expiration: string): Observable<any> {
    return this.http.post(`${this.API_BASE_URL}/checkout`, { creditCardId, firstName, lastName, expiration }, this.WITH_CREDENTIALS);
  }

  listOrders(page: number = 1, size: number = 20): Observable<ApiResponse<PaginatedResponse<OrderListItem>>> {
    return this.http.get<ApiResponse<PaginatedResponse<OrderListItem>>>(`${this.API_BASE_URL}/orders`, {
      withCredentials: true,
      params: { page, size }
    });
  }

  getOrderDetails(orderId: number): Observable<ApiResponse<OrderDetail>> {
    return this.http.get<ApiResponse<OrderDetail>>(`${this.API_BASE_URL}/orders/${orderId}`, this.WITH_CREDENTIALS);
  }

  cancelOrder(orderId: number, reason: string): Observable<any> {
    return this.http.post(`${this.API_BASE_URL}/orders/cancel`, { orderId: String(orderId), reason }, this.WITH_CREDENTIALS);
  }

  trackOrder(orderId: number): Observable<any> {
    return this.http.get(`${this.API_BASE_URL}/orders/${orderId}/track`, this.WITH_CREDENTIALS);
  }

  getRefundStatus(orderId: number): Observable<any> {
    return this.http.get(`${this.API_BASE_URL}/orders/${orderId}/refund`, this.WITH_CREDENTIALS);
  }

  getLibrary(): Observable<ApiResponse<{ items: LibraryItem[] }>> {
    return this.http.get<ApiResponse<{ items: LibraryItem[] }>>(`${this.API_BASE_URL}/library`, this.WITH_CREDENTIALS);
  }

  canWatch(movieId: string): Observable<ApiResponse<WatchAccessInfo>> {
    return this.http.get<ApiResponse<WatchAccessInfo>>(
      `${this.API_BASE_URL}/library/${encodeURIComponent(movieId)}/access`,
      this.WITH_CREDENTIALS
    );
  }

  getPlaybackGrant(movieId: string, season?: number, episode?: number): Observable<ApiResponse<PlaybackGrant>> {
    const params: Record<string, string> = {};
    if (season != null) {
      params['season'] = String(season);
    }
    if (episode != null) {
      params['episode'] = String(episode);
    }

    return this.http.get<ApiResponse<PlaybackGrant>>(
      `${this.API_BASE_URL}/library/${encodeURIComponent(movieId)}/stream`,
      { ...this.WITH_CREDENTIALS, params }
    );
  }
}
