import { CommonModule } from '@angular/common';
import {
  AfterViewInit,
  Component,
  ElementRef,
  OnDestroy,
  effect,
  inject,
  viewChild,
} from '@angular/core';
import { Router } from '@angular/router';
import { ApiService } from '../apiservice';
import { PlaybackService } from '../playback.service';

@Component({
  selector: 'app-track-player',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './track-player.html',
  styleUrls: ['./track-player.css'],
})
export class TrackPlayer implements OnDestroy, AfterViewInit {
  private apiService = inject(ApiService);
  playbackService = inject(PlaybackService);
  private router = inject(Router);
  private preventPlayOnStartup = true;
  private lastFileId: number | null = null;
  audioPlayer = viewChild<ElementRef<HTMLAudioElement>>('audioPlayer');

  currentTrack = this.playbackService.currentTrack;
  playlistName = this.playbackService.playlistName;
  hasNext = this.playbackService.hasNext;
  hasPrevious = this.playbackService.hasPrevious;

  private trackEndedListener = () => {
    this.requestNextTrack();
  }

  private requestNextTrack = () => {
    this.playbackService.nextTrack();
  }

  constructor() {
    // Effect to handle track changes and playback
    effect(() => {
      const track = this.currentTrack();
      const audio = this.audioPlayer()?.nativeElement;

      if (track && audio) {
        // Only update src and play if the track has actually changed
        if (track.fileId !== this.lastFileId) {
          this.lastFileId = track.fileId;
          const trackUrl = this.getTrackUrl(track.fileId);
          audio.src = trackUrl;
          this.updateMediaSession(track);

          if (this.preventPlayOnStartup) {
            this.preventPlayOnStartup = false;
          } else {
            // We use a small timeout to ensure the browser has processed the src change
            // before we call play(), which helps avoid AbortError.
            setTimeout(() => {
              audio.play().catch((e) => {
                if (e.name !== 'AbortError') {
                  console.error('Playback failed:', e);
                }
              });
            });
          }
        }
      } else if (!track) {
        this.lastFileId = null;
      }
    });

    // Effect to manage the 'ended' event listener on the audio element
    effect(() => {
      const audio = this.audioPlayer()?.nativeElement;
      if (audio) {
        audio.addEventListener('ended', this.trackEndedListener);
        return () => {
          audio.removeEventListener('ended', this.trackEndedListener);
        };
      }
      return;
    });
  }
  ngAfterViewInit(): void {
    // required but not used
  }

  ngOnDestroy(): void {
    if ('mediaSession' in navigator) {
      navigator.mediaSession.metadata = null;
    }
  }

  private updateMediaSession(track: any) {
    if ('mediaSession' in navigator) {
      navigator.mediaSession.metadata = new MediaMetadata({
        title: track.title,
        artist: track.artist || 'Unknown Artist',
        album: this.playlistName() || '',
        artwork: [
          {
            src: this.getTrackImageUrl(track.fileId),
            sizes: '512x512',
            type: 'image/png',
          },
        ],
      });

      // These handlers allow the lock screen buttons to work
      navigator.mediaSession.setActionHandler('play', () => this.audioPlayer()?.nativeElement.play());
      navigator.mediaSession.setActionHandler('pause', () => this.audioPlayer()?.nativeElement.pause());
      navigator.mediaSession.setActionHandler('nexttrack', () => this.playbackService.nextTrack());
      navigator.mediaSession.setActionHandler('previoustrack', () => this.playbackService.previousTrack());
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
