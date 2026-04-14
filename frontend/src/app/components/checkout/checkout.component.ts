import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { ApiService } from '../../services/api.service';
import { CartData } from '../../models/auth.model';

@Component({
  selector: 'app-checkout',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './checkout.component.html',
  styleUrls: ['./checkout.component.scss']
})
export class CheckoutComponent implements OnInit {
  cart: CartData = { items: [], itemCount: 0, totalPrice: 0 };
  loading = false;
  processing = false;
  error: string | null = null;
  successMessage: string | null = null;

  creditCardId = '';
  firstName = '';
  lastName = '';
  expiration = '';

  constructor(private apiService: ApiService, private router: Router) {}

  ngOnInit(): void {
    this.loading = true;
    this.apiService.getCart().subscribe({
      next: (res) => {
        this.cart = res.data;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.error = 'Unable to load cart for checkout.';
      }
    });
  }

  submit() {
    this.processing = true;
    this.error = null;
    this.successMessage = null;

    this.apiService.checkout(this.creditCardId, this.firstName, this.lastName, this.expiration).subscribe({
      next: (res) => {
        this.processing = false;
        this.successMessage = `Order #${res?.data?.orderId} placed successfully.`;
        setTimeout(() => this.router.navigate(['/orders']), 900);
      },
      error: (err) => {
        this.processing = false;
        const apiError = err?.error?.error;
        const reason = apiError?.details?.reason;
        if (apiError?.message && reason) {
          this.error = `${apiError.message}: ${reason}`;
          return;
        }
        this.error = apiError?.message || err?.message || 'Checkout failed.';
      }
    });
  }
}
