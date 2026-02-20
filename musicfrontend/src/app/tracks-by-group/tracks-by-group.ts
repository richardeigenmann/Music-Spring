import { Component, effect, inject, input, computed } from '@angular/core';
import { ApiService } from '../apiservice';
import { Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';

/**
 * Component for displaying tracks by a group.
 */
@Component({
  selector: 'app-tracks-by-group',
  standalone: true,
  imports: [RouterLink, CommonModule],
  templateUrl: './tracks-by-group.html',
  styleUrls: ['./tracks-by-group.css']
})
export class TracksByGroup {
  groupId = input<string>();
  private apiService = inject(ApiService);
  private router = inject(Router);

  playlistEntries = this.apiService.playlistEntries;

  currentGroupName = computed(() => {
    const id = Number(this.groupId());
    const playlistInfo = this.apiService.playlists().find(p => p.groupId === id);
    return playlistInfo ? playlistInfo.groupName : '';
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
    const name = this.currentGroupName() || 'Playlist';
    
    console.log('Setting active playlist:', name, playlist);
    this.apiService.setActivePlaylist(playlist, name);
    this.router.navigate(['/player']);
  }

  deleteCurrentGroup(): void {
    const id = Number(this.groupId());
    const name = this.currentGroupName();
    if (confirm(`Are you sure you want to delete the group "${name}"? This will NOT delete the tracks, only the group itself.`)) {
      this.apiService.deleteGroup(id).subscribe({
        next: () => {
          this.apiService.loadPlaylists(); // Refresh global list
          this.router.navigate(['/playlists']);
        },
        error: (err) => {
          console.error(err);
          alert('Failed to delete group.');
        }
      });
    }
  }
}
