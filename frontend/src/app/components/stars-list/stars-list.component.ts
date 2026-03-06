import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../services/api.service';
import { StarListItem } from '../../models/star.model';
import { ApiResponse, PaginatedResponse } from '../../models/api-response.model';

@Component({
    selector: 'app-stars-list',
    standalone: true,
    imports: [CommonModule, RouterModule, FormsModule],
    templateUrl: './stars-list.component.html',
    styleUrls: ['./stars-list.component.scss']
})
export class StarsListComponent implements OnInit {
    stars: StarListItem[] = [];
    currentPage: number = 1;
    pageSize: number = 24;
    totalPages: number = 0;
    totalElements: number = 0;
    sortBy: string = 'name';
    order: string = 'asc';
    nameFilter: string = '';
    loading: boolean = false;
    error: string | null = null;
    Math = Math;

    constructor(private apiService: ApiService) { }

    ngOnInit(): void {
        this.loadStars();
    }

    loadStars(): void {
        this.loading = true;
        this.error = null;
        this.apiService.getStars(this.currentPage, this.pageSize, this.sortBy, this.order, this.nameFilter)
            .subscribe({
                next: (response: ApiResponse<PaginatedResponse<StarListItem>>) => {
                    if (response.success && response.data) {
                        this.stars = response.data.items;
                        this.currentPage = response.data.page;
                        this.pageSize = response.data.size;
                        this.totalElements = response.data.totalElements;
                        this.totalPages = this.totalElements > 0 ? Math.ceil(this.totalElements / this.pageSize) : 0;
                    } else {
                        this.error = response.message || 'Failed to load stars.';
                    }
                    this.loading = false;
                },
                error: (err: any) => {
                    this.error = 'Error occurred while fetching stars.';
                    console.error(err);
                    this.loading = false;
                }
            });
    }

    onPageChange(page: number): void {
        if (page >= 1 && page <= this.totalPages) {
            this.currentPage = page;
            this.loadStars();
        }
    }

    onSortChange(): void {
        this.currentPage = 1;
        this.loadStars();
    }

    onFilterSubmit(): void {
        this.currentPage = 1;
        this.loadStars();
    }

    clearFilter(): void {
        this.nameFilter = '';
        this.currentPage = 1;
        this.loadStars();
    }
}
