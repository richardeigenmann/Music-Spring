import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { TrackEdit } from './track-edit';

describe('TrackEditComponent', () => {
  let component: TrackEdit;
  let fixture: ComponentFixture<TrackEdit>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TrackEdit, HttpClientTestingModule],
      providers: [provideRouter([])]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(TrackEdit);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
