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

/**
 * player component.
 */
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
  audioPlayer = viewChild<ElementRef<HTMLAudioElement>>('audioPlayer');

  currentTrack = this.playbackService.currentTrack;
  playlistName = this.playbackService.playlistName;
  hasNext = this.playbackService.hasNext;
  hasPrevious = this.playbackService.hasPrevious;

  private requestNextTrack = () => this.playbackService.nextTrack();

  constructor() {
    effect(() => {
      const track = this.currentTrack();
      const audio = this.audioPlayer()?.nativeElement;

      if (track && audio) {
        if (this.preventPlayOnStartup) {
          this.preventPlayOnStartup = false;
        } else {
          this.updateMediaSession(track);
          audio.play().catch((e) => {
            // If play() is blocked, it's often because the async effect
            // lost the "user gesture" context on mobile.
            if (e.name !== 'AbortError') console.error('Playback failed:', e);
          });
        }
      }
    });
  }

  ngAfterViewInit(): void {
    const audio = this.audioPlayer()?.nativeElement;
    if (audio) {
      // attach an event listener to the audio element to detect when a track ends and
      // trigger the next track to play
      audio.addEventListener('ended', this.requestNextTrack);
    }
  }

  ngOnDestroy(): void {
    const audio = this.audioPlayer()?.nativeElement;
    if (audio) {
      // remove the event listener when the component is destroyed
      audio.removeEventListener('ended', this.requestNextTrack);
    }
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
