import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { ApiService } from '../../services/api.service';
import { AuthStateService } from '../../services/auth-state.service';

@Component({
  selector: 'app-auth',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './auth.component.html',
  styleUrls: ['./auth.component.scss']
})
export class AuthComponent {
  mode: 'login' | 'register' = 'login';
  loading = false;
  error: string | null = null;

  email = '';
  password = '';
  firstName = '';
  lastName = '';

  constructor(
    private apiService: ApiService,
    private authState: AuthStateService,
    private router: Router
  ) {}

  switchMode(mode: 'login' | 'register') {
    this.mode = mode;
    this.error = null;
  }

  submit() {
    this.loading = true;
    this.error = null;

    const request$ = this.mode === 'login'
      ? this.apiService.login(this.email.trim(), this.password)
      : this.apiService.register(this.firstName.trim(), this.lastName.trim(), this.email.trim(), this.password);

    request$.subscribe({
      next: () => {
        this.authState.refreshSession().subscribe(() => {
          this.loading = false;
          this.router.navigate(['/cart']);
        });
      },
      error: (err) => {
        this.loading = false;
        this.error = err?.error?.error?.message || err?.message || 'Authentication failed.';
      }
    });
  }
}
