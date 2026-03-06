import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TvshowsListComponent } from './tvshows-list.component';

describe('TvshowsListComponent', () => {
  let component: TvshowsListComponent;
  let fixture: ComponentFixture<TvshowsListComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TvshowsListComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(TvshowsListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
