import { Injectable, signal } from '@angular/core';

export interface FavoriteItem {
    id: string; // IMDb ID
    title: string;
    year?: number;
    poster?: string;
}

@Injectable({
    providedIn: 'root'
})
export class FavoritesService {
    private readonly STORAGE_KEY = 'filmer_favorites';

    // Use Angular Signals for reactive state
    public favorites = signal<FavoriteItem[]>(this.loadFavorites());

    constructor() { }

    private loadFavorites(): FavoriteItem[] {
        try {
            const stored = localStorage.getItem(this.STORAGE_KEY);
            return stored ? JSON.parse(stored) : [];
        } catch {
            return [];
        }
    }

    private saveFavorites(items: FavoriteItem[]): void {
        try {
            localStorage.setItem(this.STORAGE_KEY, JSON.stringify(items));
            this.favorites.set(items);
        } catch {
            console.error('Failed to save to localStorage');
        }
    }

    toggleFavorite(item: FavoriteItem): void {
        const currentList = this.favorites();
        const index = currentList.findIndex(f => f.id === item.id);

        if (index >= 0) {
            // Remove
            this.saveFavorites(currentList.filter(f => f.id !== item.id));
        } else {
            // Add
            this.saveFavorites([...currentList, item]);
        }
    }

    isFavorite(id: string): boolean {
        return this.favorites().some(f => f.id === id);
    }
}
