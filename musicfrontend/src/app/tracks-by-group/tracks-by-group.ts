import { Component, effect, inject, input } from '@angular/core';
import { ApiService } from '../apiservice';
import { Router, RouterLink } from '@angular/router';

/**
 * Component for displaying tracks by a group.
 */
@Component({
  selector: 'app-tracks-by-group',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './tracks-by-group.html',
  styleUrls: ['./tracks-by-group.css']
})
export class TracksByGroup {
  groupId = input<string>();
  private apiService = inject(ApiService);
  private router = inject(Router);

  playlistEntries = this.apiService.playlistEntries;

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
    const id = Number(this.groupId());
    const playlistInfo = this.apiService.playlists().find(p => p.groupId === id);
    const name = playlistInfo ? playlistInfo.groupName : 'Playlist';
    
    console.log('Setting active playlist:', name, playlist);
    this.apiService.setActivePlaylist(playlist, name);
    this.router.navigate(['/player']);
  }
}
