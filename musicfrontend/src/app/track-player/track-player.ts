import { CommonModule } from '@angular/common';
import { AfterViewInit, Component, ElementRef, OnDestroy, OnInit, ViewChild, effect, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { ApiService } from '../apiservice';
import { PlaybackService } from '../playback.service';

/**
 * Always-on bottom player component.
 */
@Component({
  selector: 'app-track-player',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './track-player.html',
  styleUrls: ['./track-player.css']
})
export class TrackPlayer implements OnInit, OnDestroy, AfterViewInit {
  private apiService = inject(ApiService);
  playbackService = inject(PlaybackService);
  private router = inject(Router);

  @ViewChild('audioPlayer') audioPlayer!: ElementRef<HTMLAudioElement>;

  currentTrack = this.playbackService.currentTrack;
  playlistName = this.playbackService.playlistName;
  hasNext = this.playbackService.hasNext;
  hasPrevious = this.playbackService.hasPrevious;

  constructor() {
    // React to track changes to update audio source
    effect(() => {
      const track = this.currentTrack();
      if (track && this.audioPlayer) {
        const audio = this.audioPlayer.nativeElement;
        const newSrc = this.getTrackUrl(track.fileId);
        
        // Only reload if the source actually changed to prevent stutter on other state updates
        if (audio.src !== newSrc) {
            audio.src = newSrc;
            audio.load();
            audio.play().catch(e => console.warn('Autoplay prevented:', e));
        }
      }
    });
  }

  ngOnInit(): void {}

  ngAfterViewInit(): void {
    if (this.audioPlayer) {
        this.audioPlayer.nativeElement.addEventListener('ended', () => this.playbackService.nextTrack());
        // Initialize with current track if exists (e.g. from persistence)
        const track = this.currentTrack();
        if (track) {
            this.audioPlayer.nativeElement.src = this.getTrackUrl(track.fileId);
            // Don't auto-play on page load from persistence, user might not want that
        }
    }
  }

  ngOnDestroy(): void {
    // Component is intended to be always-on, but good practice to clean up
    if (this.audioPlayer) {
      this.audioPlayer.nativeElement.removeEventListener('ended', () => this.playbackService.nextTrack());
    }
  }

  getTrackUrl(fileId: number): string {
    return `${this.apiService.getApiUrl()}/api/trackFile/${fileId}`;
  }

  getTrackImageUrl(fileId: number): string {
    return `${this.apiService.getApiUrl()}/api/trackFileImage/${fileId}`;
  }
  
  openQueue() {
      this.router.navigate(['/queue']);
  }
}

