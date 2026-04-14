import { Injectable, signal } from '@angular/core';

export interface RentalItem {
  id: string;
  title: string;
  year?: number;
  poster?: string;
  rentedAt: string;
}

@Injectable({
  providedIn: 'root'
})
export class RentalService {
  private readonly STORAGE_KEY = 'filmer_rentals';
  public rentals = signal<RentalItem[]>(this.loadRentals());

  private loadRentals(): RentalItem[] {
    try {
      const stored = localStorage.getItem(this.STORAGE_KEY);
      return stored ? JSON.parse(stored) : [];
    } catch {
      return [];
    }
  }

  private saveRentals(items: RentalItem[]): void {
    try {
      localStorage.setItem(this.STORAGE_KEY, JSON.stringify(items));
      this.rentals.set(items);
    } catch {
      console.error('Failed to save rentals to localStorage');
    }
  }

  rent(item: Omit<RentalItem, 'rentedAt'>): void {
    const current = this.rentals();
    if (current.some(r => r.id === item.id)) {
      return;
    }

    this.saveRentals([{ ...item, rentedAt: new Date().toISOString() }, ...current]);
  }

  remove(id: string): void {
    this.saveRentals(this.rentals().filter(r => r.id !== id));
  }

  clear(): void {
    this.saveRentals([]);
  }

  isRented(id: string): boolean {
    return this.rentals().some(r => r.id === id);
  }
}
