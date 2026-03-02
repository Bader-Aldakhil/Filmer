import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { of, throwError } from 'rxjs';
import { ConnectivityTestComponent } from './connectivity-test.component';
import { ApiService } from '../../services/api.service';

describe('ConnectivityTestComponent', () => {
  let component: ConnectivityTestComponent;
  let fixture: ComponentFixture<ConnectivityTestComponent>;
  let apiServiceSpy: jasmine.SpyObj<ApiService>;

  beforeEach(async () => {
    apiServiceSpy = jasmine.createSpyObj<ApiService>('ApiService', ['testDatabaseConnectivity']);

    await TestBed.configureTestingModule({
      imports: [ConnectivityTestComponent],
      providers: [{ provide: ApiService, useValue: apiServiceSpy }]
    }).compileComponents();

    fixture = TestBed.createComponent(ConnectivityTestComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should start in idle state', () => {
    expect(component.connectionStatus).toBe('idle');
    expect(component.isLoading).toBeFalse();
    expect(component.result).toBeNull();
  });

  it('should set success result when API responds successfully', () => {
    const mockResponse = {
      success: true,
      data: {
        database_status: 'UP',
        result: 1,
        message: 'Database connection successful'
      }
    };

    apiServiceSpy.testDatabaseConnectivity.and.returnValue(of(mockResponse));

    component.runConnectivityTest();

    expect(apiServiceSpy.testDatabaseConnectivity).toHaveBeenCalled();
    expect(component.connectionStatus).toBe('success');
    expect(component.isLoading).toBeFalse();
    expect(component.result?.success).toBeTrue();
    expect(component.result?.data).toEqual(mockResponse);
    expect(component.result?.timestamp).toBeTruthy();
  });

  it('should map network error status 0 to backend connection message', () => {
    const error = new HttpErrorResponse({ status: 0, statusText: 'Unknown Error' });
    apiServiceSpy.testDatabaseConnectivity.and.returnValue(throwError(() => error));

    component.runConnectivityTest();

    expect(component.connectionStatus).toBe('error');
    expect(component.result?.success).toBeFalse();
    expect(component.result?.error).toContain('Cannot connect to backend server');
  });

  it('should map 503 to database failure message', () => {
    const error = new HttpErrorResponse({ status: 503, statusText: 'Service Unavailable' });
    apiServiceSpy.testDatabaseConnectivity.and.returnValue(throwError(() => error));

    component.runConnectivityTest();

    expect(component.connectionStatus).toBe('error');
    expect(component.result?.error).toContain('Database connection failed');
  });

  it('should map 5xx status to server error message', () => {
    const error = new HttpErrorResponse({ status: 500, statusText: 'Internal Server Error' });
    apiServiceSpy.testDatabaseConnectivity.and.returnValue(throwError(() => error));

    component.runConnectivityTest();

    expect(component.connectionStatus).toBe('error');
    expect(component.result?.error).toBe('Server error (500): Internal Server Error');
  });

  it('should map 4xx status to client error message', () => {
    const error = new HttpErrorResponse({ status: 400, statusText: 'Bad Request' });
    apiServiceSpy.testDatabaseConnectivity.and.returnValue(throwError(() => error));

    component.runConnectivityTest();

    expect(component.connectionStatus).toBe('error');
    expect(component.result?.error).toBe('Client error (400): Bad Request');
  });

  it('should format JSON output with indentation', () => {
    const formatted = component.formatJson({ key: 'value' });
    expect(formatted).toContain('"key": "value"');
    expect(formatted).toContain('\n');
  });
});
