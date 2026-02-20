import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TracksByGroup } from './tracks-by-group';
import { RouterTestingModule } from '@angular/router/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';

describe('TracksByGroup', () => {
  let component: TracksByGroup;
  let fixture: ComponentFixture<TracksByGroup>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TracksByGroup, RouterTestingModule, HttpClientTestingModule],
    }).compileComponents();

    fixture = TestBed.createComponent(TracksByGroup);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
