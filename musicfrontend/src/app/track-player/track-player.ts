import { CommonModule } from '@angular/common';
import {
  AfterViewInit,
  Component,
  ElementRef,
  OnDestroy,
  ViewChild,
  effect,
  inject,
} from '@angular/core';
import { Router } from '@angular/router';
import { ApiService } from '../apiservice';
import { PlaybackService } from '../playback.service';

/**
 * Always-on bottom player component.
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

  @ViewChild('audioPlayer') audioPlayer!: ElementRef<HTMLAudioElement>;

  currentTrack = this.playbackService.currentTrack;
  playlistName = this.playbackService.playlistName;
  hasNext = this.playbackService.hasNext;
  hasPrevious = this.playbackService.hasPrevious;

  constructor() {
    effect(() => {
      const track = this.currentTrack();
      // We still grab the reference, but we don't set the .src here anymore
      const audio = this.audioPlayer?.nativeElement;

      if (track && audio) {
        // The template handles audio.src automatically now.
        // We just ensure it plays if the track changes while the player is already open.
        audio.load();
        audio.play().catch((e) => {
          if (e.name !== 'AbortError') console.warn('Playback error:', e);
        });
      }
    });
  }

  ngAfterViewInit(): void {
    if (this.audioPlayer) {
      this.audioPlayer.nativeElement.addEventListener('ended', () =>
        this.playbackService.nextTrack(),
      );
    }
  }

  ngOnDestroy(): void {
    // Component is intended to be always-on, but good practice to clean up
    if (this.audioPlayer) {
      this.audioPlayer.nativeElement.removeEventListener('ended', () =>
        this.playbackService.nextTrack(),
      );
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
