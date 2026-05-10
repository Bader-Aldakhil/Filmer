import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule, RouterLink, RouterLinkActive } from '@angular/router';
import { ApiService } from './services/api.service';
import { AuthStateService } from './services/auth-state.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterModule, RouterLink, RouterLinkActive],
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent {
  isMobileMenuOpen = false;
  title = 'Filmer - Movie Rental System';

  constructor(
    public authState: AuthStateService,
    private apiService: ApiService,
    private router: Router
  ) {
    this.authState.refreshSession().subscribe();
  }

  toggleMobileMenu() {
    this.isMobileMenuOpen = !this.isMobileMenuOpen;
  }

  closeMobileMenu() {
    this.isMobileMenuOpen = false;
  }

  logout() {
    this.apiService.logout().subscribe({
      next: () => {
        this.authState.clearSession();
        this.closeMobileMenu();
        this.router.navigate(['/']);
      },
      error: () => {
        this.authState.clearSession();
        this.closeMobileMenu();
        this.router.navigate(['/']);
      }
    });
  }
}
