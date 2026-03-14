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
      const audio = this.audioPlayer?.nativeElement;
      
      if (track && audio) {
        const newSrc = this.getTrackUrl(track.fileId);
        
        // Check current source to avoid redundant loads
        // We also check if it's the first time or a track skip
        if (audio.src !== newSrc) {
            console.log('Switching track source to:', newSrc);
            audio.src = newSrc;
            audio.load();
            
            // Only autoplay if the audio element is ready and not already playing something else
            // Use a slight timeout to ensure the DOM has updated and let previous requests settle
            setTimeout(() => {
                audio.play().catch(e => {
                    if (e.name !== 'AbortError') {
                        console.warn('Playback error:', e);
                    }
                });
            }, 50);
        }
      }
    });
  }

  ngOnInit(): void {}

  ngAfterViewInit(): void {
    if (this.audioPlayer) {
        this.audioPlayer.nativeElement.addEventListener('ended', () => this.playbackService.nextTrack());
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

