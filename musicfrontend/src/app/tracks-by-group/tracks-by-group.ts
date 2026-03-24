import { Component, effect, inject, input, computed, signal, OnDestroy } from '@angular/core';
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
export class TracksByGroup implements OnDestroy {
  groupId = input<string>();
  private apiService = inject(ApiService);
  private playbackService = inject(PlaybackService);
  private router = inject(Router);

  showMenu = signal(false);

  playlistEntries = this.apiService.playlistEntries;
  currentTrackId = computed(() => this.playbackService.currentTrack()?.trackId || null);

  currentGroupName = computed(() => {
    const id = Number(this.groupId());
    const groups = this.apiService.groups();
    const group = groups.find(g => g.groupId === id);
    return group?.groupName || 'Unknown';
  });

  currentGroupTitle = computed(() => {
    const id = Number(this.groupId());
    const groups = this.apiService.groups();
    const group = groups.find(g => g.groupId === id);

    const type = group?.groupTypeName || 'Group';
    const name = group?.groupName || 'Unknown';

    return `${type}: ${name}`;
  });

  private clickListener = (event: Event) => {
    if (this.showMenu()) {
      this.showMenu.set(false);
    }
  };

  constructor() {
    effect(() => {
      const id = this.groupId();
      if (id) {
        this.apiService.loadPlaylistEntries(Number(id));
      }
    });

    document.addEventListener('click', this.clickListener);
  }

  ngOnDestroy() {
    document.removeEventListener('click', this.clickListener);
  }

  toggleMenu(event: Event): void {
    event.stopPropagation();
    this.showMenu.update(v => !v);
  }

  onDropdownClick(event: Event): void {
    event.stopPropagation();
  }

  playAll(): void {
    const playlist = this.playlistEntries();
    const title = this.currentGroupTitle();

    console.log('Playing playlist:', title, playlist);
    this.playbackService.playPlaylist(playlist, title);
  }

  deleteCurrentGroup(): void {
    this.showMenu.set(false); // Close menu
    const id = Number(this.groupId());
    const name = this.currentGroupName();
    if (confirm(`Are you sure you want to delete the group "${name}"? This will NOT delete the tracks, only the group itself.`)) {
      this.apiService.deleteGroup(id).subscribe({
        next: () => {
          this.router.navigate(['/groups']);
        },
        error: (err) => {
          console.error(err);
          alert('Failed to delete group.');
        }
      });
    }
  }

  private sanitizeFilename(name: string): string {
    // Replace ":" with a standard separator and then remove any characters that are not letters, numbers, spaces, hyphens, or underscores.
    const sanitized = name.replace(':', ' - ').replace(/[^a-zA-Z0-9 \\-_]/g, '_');
    // Collapse consecutive spaces or underscores into a single underscore and trim leading/trailing underscores.
    return sanitized.replace(/[ _]+/g, '_').replace(/^_+|_+$/g, '');
  }

  downloadM3u(): void {
    console.log('Downloading M3U for group', this.groupId());
    this.showMenu.set(false);
    const id = Number(this.groupId());
    const filename = this.sanitizeFilename(this.currentGroupTitle());

    this.apiService.downloadGroupAsM3u(id).subscribe(blob => {
      console.log('M3U download received');
      const a = document.createElement('a');
      const objectUrl = URL.createObjectURL(blob);
      a.href = objectUrl;
      a.download = `${filename}.m3u`;
      a.click();
      URL.revokeObjectURL(objectUrl);
    });
  }

  downloadZip(): void {
    console.log('Downloading ZIP for group', this.groupId());
    this.showMenu.set(false);
    const id = Number(this.groupId());
    const filename = this.sanitizeFilename(this.currentGroupTitle());

    this.apiService.downloadGroupAsZip(id).subscribe(blob => {
      console.log('ZIP download received');
      const a = document.createElement('a');
      const objectUrl = URL.createObjectURL(blob);
      a.href = objectUrl;
      a.download = `${filename}.zip`;
      a.click();
      URL.revokeObjectURL(objectUrl);
    });
  }
}
