import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { HomeComponent } from './home.component';

describe('HomeComponent', () => {
  let component: HomeComponent;
  let fixture: ComponentFixture<HomeComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HomeComponent, RouterTestingModule]
    }).compileComponents();

    fixture = TestBed.createComponent(HomeComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should display welcome title', () => {
    const element = fixture.nativeElement as HTMLElement;
    const heading = element.querySelector('.hero-content h1');

    expect(heading?.textContent).toContain('Welcome to Filmer');
  });

  it('should show expected feature cards', () => {
    const element = fixture.nativeElement as HTMLElement;
    const cards = element.querySelectorAll('.feature-card');

    expect(component.features.length).toBe(4);
    expect(cards.length).toBe(4);
    expect(component.features.map(feature => feature.title)).toEqual([
      'Browse Movies',
      'Search & Filter',
      'Rent Movies',
      'Track Orders'
    ]);
  });

  it('should render disabled coming-soon browse button', () => {
    const element = fixture.nativeElement as HTMLElement;
    const buttons = Array.from(element.querySelectorAll('button'));
    const browseButton = buttons.find(button =>
      (button.textContent ?? '').includes('Browse Movies')
    );

    expect(browseButton).toBeTruthy();
    expect(browseButton?.hasAttribute('disabled')).toBeTrue();
  });
});
