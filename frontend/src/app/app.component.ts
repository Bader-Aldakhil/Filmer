import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, RouterLink, RouterLinkActive } from '@angular/router';
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
  title = 'Filmer - Movie Rental System';

  constructor(
    public authState: AuthStateService,
    private apiService: ApiService
  ) {
    this.authState.refreshSession().subscribe();
  }

  logout() {
    this.apiService.logout().subscribe({
      next: () => this.authState.clearSession(),
      error: () => this.authState.clearSession()
    });
  }
}
