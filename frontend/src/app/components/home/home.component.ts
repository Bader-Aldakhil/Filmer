import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.scss']
})
export class HomeComponent {
  title = 'Welcome to Filmer';
  features: { title: string; description: string; route: string }[] = [
    {
      title: 'Browse Movies',
      description: 'Explore a wide catalog of movies and TV shows.',
      route: '/movies'
    },
    {
      title: 'Search & Filter',
      description: 'Find titles quickly with sorting and filter tools.',
      route: '/movies'
    },
    {
      title: 'Rent Movies',
      description: 'Add titles to your cart and complete checkout securely.',
      route: '/cart'
    },
    {
      title: 'Track Orders',
      description: 'Review past orders and monitor rental activity.',
      route: '/orders'
    }
  ];
}
