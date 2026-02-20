import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TrackPlayer } from './track-player';
import { RouterTestingModule } from '@angular/router/testing';

describe('TrackPlayer', () => {
  let component: TrackPlayer;
  let fixture: ComponentFixture<TrackPlayer>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TrackPlayer, RouterTestingModule],
    }).compileComponents();

    fixture = TestBed.createComponent(TrackPlayer);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
