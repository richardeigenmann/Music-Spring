import { Component, effect, inject, input, computed } from '@angular/core';
import { ApiService } from '../apiservice';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { PlaybackService } from '../playback.service';
import { TrackList } from '../shared/track-list/track-list.component';

/**
 * Component for displaying tracks by a group.
 */
@Component({
  selector: 'app-tracks-by-group',
  standalone: true,
  imports: [CommonModule, TrackList],
  templateUrl: './tracks-by-group.html',
  styleUrls: ['./tracks-by-group.css']
})
export class TracksByGroup {
  groupId = input<string>();
  private apiService = inject(ApiService);
  private playbackService = inject(PlaybackService);
  private router = inject(Router);

  playlistEntries = this.apiService.playlistEntries;
  currentTrackId = computed(() => this.playbackService.currentTrack()?.trackId || null);

  currentGroupName = computed(() => {
    const id = Number(this.groupId());
    const playlistInfo = this.apiService.playlists().find(p => p.groupId === id);
    return playlistInfo ? playlistInfo.groupName : '';
  });

  currentGroupTitle = computed(() => {
    const id = Number(this.groupId());
    const playlist = this.playlistEntries();

    // Find info from global playlists (works for actual playlists)
    const playlistInfo = this.apiService.playlists().find(p => p.groupId === id);

    // If it's a non-playlist group, we need to find its type from the track details
    const sampleTrack = playlist[0];
    const detail = sampleTrack?.groupDetails.find(d => d.groupId === id);

    const type = detail?.groupTypeName || (playlistInfo ? 'Playlist' : 'Group');
    const name = playlistInfo?.groupName || detail?.groupName || 'Unknown';

    return `${type}: ${name}`;
  });

  constructor() {
    effect(() => {
      const id = this.groupId();
      if (id) {
        this.apiService.loadPlaylistEntries(Number(id));
      }
    });
  }

  playAll(): void {
    const playlist = this.playlistEntries();
    const title = this.currentGroupTitle();

    console.log('Playing playlist:', title, playlist);
    this.playbackService.playPlaylist(playlist, title);
  }

  deleteCurrentGroup(): void {
    const id = Number(this.groupId());
    const name = this.currentGroupName();
    if (confirm(`Are you sure you want to delete the group "${name}"? This will NOT delete the tracks, only the group itself.`)) {
      this.apiService.deleteGroup(id).subscribe({
        next: () => {
          this.apiService.loadPlaylists(); // Refresh global list
          this.router.navigate(['/groups']);
        },
        error: (err) => {
          console.error(err);
          alert('Failed to delete group.');
        }
      });
    }
  }
}
