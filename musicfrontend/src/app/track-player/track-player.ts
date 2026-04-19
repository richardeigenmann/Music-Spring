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

  layOnStartup = false;

  @ViewChild('audioPlayer') audioPlayer!: ElementRef<HTMLAudioElement>;

  currentTrack = this.playbackService.currentTrack;
  playlistName = this.playbackService.playlistName;
  hasNext = this.playbackService.hasNext;
  hasPrevious = this.playbackService.hasPrevious;

  private requestNextTrack = () => this.playbackService.nextTrack();

  constructor() {
    effect(() => {
      const track = this.currentTrack();
      const audio = this.audioPlayer?.nativeElement;

      if (track && audio) {
        audio.load();
        if (this.preventPlayOnStartup) {
          this.preventPlayOnStartup = false;
        } else {
          audio.play().catch((e) => {
            if (e.name !== 'AbortError') console.warn('Playback error:', e);
          });
        }
      }
    });
  }

  ngAfterViewInit(): void {
    if (this.audioPlayer) {
      // attach an event listener to the audio element to detect when a track ends and
      // trigger the next track to play
      this.audioPlayer.nativeElement.addEventListener('ended', this.requestNextTrack);
    }
  }

  ngOnDestroy(): void {
    if (this.audioPlayer) {
      // remove the event listener when the component is destroyed
      this.audioPlayer.nativeElement.removeEventListener('ended', this.requestNextTrack);
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
