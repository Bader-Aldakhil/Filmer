import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { ApiService } from './api.service';

describe('ApiService', () => {
  let service: ApiService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [ApiService]
    });

    service = TestBed.inject(ApiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should call GET /health for getHealth', () => {
    const mockResponse = { success: true, data: { status: 'UP' } };

    service.getHealth().subscribe(response => {
      expect(response).toEqual(mockResponse);
    });

    const request = httpMock.expectOne('https://localhost:8443/api/v1/health');
    expect(request.request.method).toBe('GET');
    request.flush(mockResponse);
  });

  it('should call GET /health/db for testDatabaseConnectivity', () => {
    const mockResponse = { success: true, data: { database_status: 'UP' } };

    service.testDatabaseConnectivity().subscribe(response => {
      expect(response).toEqual(mockResponse);
    });

    const request = httpMock.expectOne('https://localhost:8443/api/v1/health/db');
    expect(request.request.method).toBe('GET');
    request.flush(mockResponse);
  });

  it('should surface API errors from getHealth', () => {
    const expectedMessage = 'Service unavailable';

    service.getHealth().subscribe({
      next: () => fail('expected an error response'),
      error: error => {
        expect(error.status).toBe(503);
        expect(error.statusText).toBe(expectedMessage);
      }
    });

    const request = httpMock.expectOne('https://localhost:8443/api/v1/health');
    request.flush({ message: expectedMessage }, { status: 503, statusText: expectedMessage });
  });
});
