import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { ApiService } from '../../services/api.service';

interface ConnectivityResult {
  success: boolean;
  data?: any;
  error?: string;
  timestamp?: string;
}

@Component({
  selector: 'app-connectivity-test',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './connectivity-test.component.html',
  styleUrls: ['./connectivity-test.component.scss']
})
export class ConnectivityTestComponent implements OnInit {
  isLoading = false;
  result: ConnectivityResult | null = null;
  connectionStatus: 'idle' | 'testing' | 'success' | 'error' = 'idle';

  constructor(private apiService: ApiService) {}

  ngOnInit() {
    // Optionally auto-run the test on component load
    // this.runConnectivityTest();
  }

  /**
   * Execute the connectivity test by calling the backend health/db endpoint.
   */
  runConnectivityTest() {
    this.isLoading = true;
    this.connectionStatus = 'testing';
    this.result = null;

    this.apiService.testDatabaseConnectivity().subscribe({
      next: (response: any) => {
        this.result = {
          success: true,
          data: response,
          timestamp: new Date().toLocaleTimeString()
        };
        this.connectionStatus = 'success';
        this.isLoading = false;
      },
      error: (error: HttpErrorResponse) => {
        let errorMessage = 'Unknown error occurred';

        if (error.status === 0) {
          errorMessage = 'Cannot connect to backend server. Is it running on https://localhost:8443?';
        } else if (error.status === 503) {
          errorMessage = 'Database connection failed. Is PostgreSQL running?';
        } else if (error.status >= 500) {
          errorMessage = `Server error (${error.status}): ${error.statusText}`;
        } else if (error.status >= 400) {
          errorMessage = `Client error (${error.status}): ${error.statusText}`;
        }

        this.result = {
          success: false,
          error: errorMessage,
          timestamp: new Date().toLocaleTimeString()
        };
        this.connectionStatus = 'error';
        this.isLoading = false;
      }
    });
  }

  /**
   * Format JSON data for display.
   */
  formatJson(data: any): string {
    return JSON.stringify(data, null, 2);
  }
}
