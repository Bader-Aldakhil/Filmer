import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiResponse, PaginatedResponse } from '../models/api-response.model';
import { MovieListItem, MovieDetail, GenreInfo } from '../models/movie.model';
import { StarListItem, StarDetail } from '../models/star.model';
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
}
