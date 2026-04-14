import { Routes } from '@angular/router';
import { HomeComponent } from './components/home/home.component';
import { ConnectivityTestComponent } from './components/connectivity-test/connectivity-test.component';
import { MoviesListComponent } from './components/movies-list/movies-list.component';
import { MovieDetailComponent } from './components/movie-detail/movie-detail.component';
import { TvshowsListComponent } from './components/tvshows-list/tvshows-list.component';
import { FavoritesComponent } from './components/favorites/favorites.component';
import { RentalsComponent } from './components/rentals/rentals.component';
import { AuthComponent } from './components/auth/auth.component';
import { CartComponent } from './components/cart/cart.component';
import { CheckoutComponent } from './components/checkout/checkout.component';
import { OrdersComponent } from './components/orders/orders.component';
import { WatchComponent } from './components/watch/watch.component';
export const APP_ROUTES: Routes = [
  {
    path: '',
    component: HomeComponent
  },
  {
    path: 'connection-test',
    component: ConnectivityTestComponent
  },
  {
    path: 'movies',
    component: MoviesListComponent,
    title: 'Filmer - Movies'
  },
  {
    path: 'tvshows',
    component: TvshowsListComponent,
    title: 'Filmer - TV Shows'
  },
  {
    path: 'movies/:id',
    component: MovieDetailComponent,
    title: 'Filmer - Movie Details'
  },
  {
    path: 'tvshows/:id',
    component: MovieDetailComponent,
    title: 'Filmer - TV Show Details'
  },
  {
    path: 'favorites',
    component: FavoritesComponent
  },
  {
    path: 'rentals',
    component: RentalsComponent,
    title: 'Filmer - My Rentals'
  },
  {
    path: 'auth',
    component: AuthComponent,
    title: 'Filmer - Sign In'
  },
  {
    path: 'cart',
    component: CartComponent,
    title: 'Filmer - Cart'
  },
  {
    path: 'checkout',
    component: CheckoutComponent,
    title: 'Filmer - Checkout'
  },
  {
    path: 'orders',
    component: OrdersComponent,
    title: 'Filmer - Orders'
  },
  {
    path: 'watch/:movieId',
    component: WatchComponent,
    title: 'Filmer - Watch'
  },
  {
    path: 'search',
    redirectTo: 'movies'
  },
  {
    path: '**',
    redirectTo: ''
  }
];
