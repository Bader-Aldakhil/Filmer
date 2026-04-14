import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { RentalService } from '../../services/rental.service';

@Component({
  selector: 'app-rentals',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './rentals.component.html',
  styleUrls: ['./rentals.component.scss']
})
export class RentalsComponent {
  constructor(public rentalService: RentalService) {}

  remove(id: string): void {
    this.rentalService.remove(id);
  }
}
