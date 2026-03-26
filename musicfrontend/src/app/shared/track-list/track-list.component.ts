import { Component, inject, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { ApiService, TrackEntry } from '../../apiservice';
import { PlaybackService } from '../../playback.service';

@Component({
  selector: 'app-track-list',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './track-list.component.html',
  styleUrls: ['./track-list.component.css']
})
export class TrackList {
  tracks = input.required<TrackEntry[]>();
  highlightId = input<number | null>(null);

  private apiService = inject(ApiService);
  private playbackService = inject(PlaybackService);
  private router = inject(Router);

  playSingleTrack(event: Event, track: TrackEntry) {
    event.stopPropagation();
    const title = `Track: ${track.artist} ${track.title}`;
    this.playbackService.playPlaylist([track], title);
  }

  getTrackImageUrl(fileId: number): string {
    return `${this.apiService.getApiUrl()}/api/trackFileImage/${fileId}`;
  }

  goToEdit(trackId: number) {
      this.router.navigate(['/track', trackId]);
  }

  getArtistTagId(track: TrackEntry): number | null {
    const artistTag = track.tagDetails.find(t => t.tagTypeName.toLowerCase() === 'artist');
    return artistTag ? artistTag.tagId : null;
  }
}
