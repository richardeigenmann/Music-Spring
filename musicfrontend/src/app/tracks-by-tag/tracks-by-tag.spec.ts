import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TracksByTag } from './tracks-by-tag';
import { RouterTestingModule } from '@angular/router/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';

describe('TracksByTag', () => {
  let component: TracksByTag;
  let fixture: ComponentFixture<TracksByTag>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TracksByTag, RouterTestingModule, HttpClientTestingModule],
    }).compileComponents();

    fixture = TestBed.createComponent(TracksByTag);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
