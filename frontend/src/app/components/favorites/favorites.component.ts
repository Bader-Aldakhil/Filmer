import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FavoritesService, FavoriteItem } from '../../services/favorites.service';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-favorites',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './favorites.component.html',
  styleUrl: './favorites.component.scss'
})
export class FavoritesComponent {
  favoritesService = inject(FavoritesService);
  private router = inject(Router);

  // Read signal
  favorites = this.favoritesService.favorites;

  goToDetail(id: string): void {
    this.router.navigate(['/movies', id]);
  }

  removeFavorite(item: FavoriteItem, event: Event): void {
    event.stopPropagation();
    this.favoritesService.toggleFavorite(item);
  }
}
