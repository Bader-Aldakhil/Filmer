import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { ApiService } from '../../services/api.service';
import { CartData } from '../../models/auth.model';

@Component({
  selector: 'app-cart',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './cart.component.html',
  styleUrls: ['./cart.component.scss']
})
export class CartComponent implements OnInit {
  cart: CartData = { items: [], itemCount: 0, totalPrice: 0 };
  loading = false;
  error: string | null = null;
  unauthorized = false;

  constructor(private apiService: ApiService, private router: Router) {}

  ngOnInit(): void {
    this.loadCart();
  }

  loadCart() {
    this.loading = true;
    this.error = null;
    this.unauthorized = false;

    this.apiService.getCart().subscribe({
      next: (res) => {
        this.cart = res.data;
        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        if (err?.status === 401) {
          this.unauthorized = true;
          return;
        }
        this.error = err?.error?.error?.message || 'Failed to load cart.';
      }
    });
  }

  remove(movieId: string) {
    this.apiService.removeFromCart(movieId).subscribe({ next: () => this.loadCart() });
  }

  clearAll() {
    this.apiService.clearCart().subscribe({ next: () => this.loadCart() });
  }

  goCheckout() {
    this.router.navigate(['/checkout']);
  }
}
