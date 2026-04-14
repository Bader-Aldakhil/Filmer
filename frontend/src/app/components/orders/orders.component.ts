import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { ApiService } from '../../services/api.service';
import { OrderDetail, OrderListItem } from '../../models/auth.model';

@Component({
  selector: 'app-orders',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './orders.component.html',
  styleUrls: ['./orders.component.scss']
})
export class OrdersComponent implements OnInit {
  orders: OrderListItem[] = [];
  selectedOrder: OrderDetail | null = null;
  tracking: any = null;
  refund: any = null;
  loading = false;
  error: string | null = null;
  cancelReason = 'Customer request';

  constructor(private apiService: ApiService) {}

  ngOnInit(): void {
    this.loadOrders();
  }

  loadOrders() {
    this.loading = true;
    this.error = null;
    this.apiService.listOrders().subscribe({
      next: (res) => {
        this.orders = res?.data?.items || [];
        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        this.error = err?.error?.error?.message || 'Failed to load orders.';
      }
    });
  }

  selectOrder(orderId: number) {
    this.selectedOrder = null;
    this.tracking = null;
    this.refund = null;

    this.apiService.getOrderDetails(orderId).subscribe({
      next: (res) => {
        this.selectedOrder = res.data;
      }
    });

    this.apiService.trackOrder(orderId).subscribe({
      next: (res) => this.tracking = res.data
    });

    this.apiService.getRefundStatus(orderId).subscribe({
      next: (res) => this.refund = res.data,
      error: () => this.refund = null
    });
  }

  cancel(orderId: number) {
    this.apiService.cancelOrder(orderId, this.cancelReason).subscribe({
      next: () => {
        this.selectOrder(orderId);
        this.loadOrders();
      },
      error: (err) => {
        this.error = err?.error?.error?.message || 'Cancel failed.';
      }
    });
  }
}
