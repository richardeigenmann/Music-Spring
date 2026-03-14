import { Component, inject, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PlaybackService } from '../playback.service';
import { ApiService } from '../apiservice';
import { RouterLink } from '@angular/router';
import { TrackList } from '../shared/track-list/track-list.component';

@Component({
  selector: 'app-queue',
  standalone: true,
  imports: [CommonModule, RouterLink, TrackList],
  templateUrl: './queue.html',
  styleUrls: ['./queue.css']
})
export class Queue {
  playbackService = inject(PlaybackService);
  private apiService = inject(ApiService);

  currentPlaylist = this.playbackService.currentPlaylist;
  currentTrack = this.playbackService.currentTrack;
  playlistName = this.playbackService.playlistName;
  isShuffled = this.playbackService.isShuffled;
  
  canGoBack = this.playbackService.canGoBack;
  canGoForward = this.playbackService.canGoForward;

  currentTrackId = computed(() => this.currentTrack()?.trackId || null);

  getTrackImageUrl(fileId: number): string {
    return `${this.apiService.getApiUrl()}/api/trackFileImage/${fileId}`;
  }
}
