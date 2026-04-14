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
  features = [
    {
      icon: '🎬',
      title: 'Browse Movies',
      description: 'Discover top-rated movies with smart filtering and infinite browsing'
    },
    {
      icon: '📺',
      title: 'Browse TV Shows',
      description: 'Explore highly rated series with the same smooth browsing experience'
    },
    {
      icon: '🛒',
      title: 'Rentals',
      description: 'Keep track of movies you have rented in one place'
    },
    {
      icon: '⭐',
      title: 'My List',
      description: 'Save favorite titles and quickly return to what you love'
    }
  ];
}
