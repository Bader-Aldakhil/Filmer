import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { ApiService } from '../../services/api.service';
import { LibraryItem } from '../../models/auth.model';

@Component({
  selector: 'app-rentals',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './rentals.component.html',
  styleUrls: ['./rentals.component.scss']
})
export class RentalsComponent implements OnInit {
  items: LibraryItem[] = [];
  loading = false;
  error: string | null = null;

  constructor(private apiService: ApiService) {}

  ngOnInit(): void {
    this.loading = true;
    this.apiService.getLibrary().subscribe({
      next: (res) => {
        this.items = res?.data?.items || [];
        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        if (err?.status === 401) {
          this.error = 'Please sign in to view your rentals.';
          return;
        }
        this.error = err?.error?.error?.message || 'Failed to load rentals.';
      }
    });
  }
}
