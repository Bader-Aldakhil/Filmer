import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { ApiService } from '../../services/api.service';
import { StarDetail } from '../../models/star.model';

@Component({
    selector: 'app-star-detail',
    standalone: true,
    imports: [CommonModule, RouterModule],
    templateUrl: './star-detail.component.html',
    styleUrls: ['./star-detail.component.scss']
})
export class StarDetailComponent implements OnInit {
    star: StarDetail | null = null;
    loading: boolean = false;
    error: string | null = null;

    constructor(
        private route: ActivatedRoute,
        private apiService: ApiService
    ) { }

    ngOnInit(): void {
        this.route.paramMap.subscribe(params => {
            const id = params.get('id');
            if (id) {
                this.loadStar(id);
            } else {
                this.error = 'Invalid star ID';
            }
        });
    }

    loadStar(id: string): void {
        this.loading = true;
        this.error = null;

        this.apiService.getStarById(id).subscribe({
            next: (response) => {
                if (response.success && response.data) {
                    this.star = response.data;
                } else {
                    this.error = response.message || 'Star not found';
                }
                this.loading = false;
            },
            error: (err) => {
                this.error = 'Failed to load star details.';
                console.error(err);
                this.loading = false;
            }
        });
    }
}
