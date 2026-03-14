import { Component, OnInit, inject, signal } from '@angular/core';
import { ApiService, Group, TrackEntry } from '../apiservice';
import { Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CdkDragDrop, DragDropModule, moveItemInArray, transferArrayItem } from '@angular/cdk/drag-drop';
import { PlaybackService } from '../playback.service';

@Component({
  selector: 'app-mixing-board',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule, DragDropModule],
  templateUrl: './mixing-board.html',
  styleUrls: ['./mixing-board.css']
})
export class MixingBoard implements OnInit {
  private apiService = inject(ApiService);
  private playbackService = inject(PlaybackService);
  private router = inject(Router);

  // Group lists for drag and drop
  availableGroups = signal<Group[]>([]);
  mustHave = signal<Group[]>([]);
  canHave = signal<Group[]>([]);
  mustNotHave = signal<Group[]>([]);

  // Filtering results
  filteredTracks = signal<TrackEntry[]>([]);
  isFiltering = signal(false);

  // New playlist name
  newPlaylistName = '';

  ngOnInit(): void {
    this.apiService.getGroups().subscribe(groups => {
      this.availableGroups.set(groups.sort((a, b) => a.groupName.localeCompare(b.groupName)));
    });
  }

  drop(event: CdkDragDrop<Group[]>): void {
    if (event.previousContainer === event.container) {
      const data = [...event.container.data];
      moveItemInArray(data, event.previousIndex, event.currentIndex);
      this.setSignalData(event.container.id, data);
    } else {
      const prevData = [...event.previousContainer.data];
      const currData = [...event.container.data];
      transferArrayItem(
        prevData,
        currData,
        event.previousIndex,
        event.currentIndex
      );
      this.setSignalData(event.previousContainer.id, prevData);
      this.setSignalData(event.container.id, currData);
    }
    this.updateResults();
  }

  private setSignalData(id: string, data: Group[]): void {
    if (id === 'availableList') this.availableGroups.set(data);
    else if (id === 'mustList') this.mustHave.set(data);
    else if (id === 'canList') this.canHave.set(data);
    else if (id === 'notList') this.mustNotHave.set(data);
  }

  updateResults(): void {
    const must = this.mustHave().map(g => g.groupId);
    const can = this.canHave().map(g => g.groupId);
    const not = this.mustNotHave().map(g => g.groupId);

    if (must.length === 0 && can.length === 0 && not.length === 0) {
      this.filteredTracks.set([]);
      return;
    }

    this.isFiltering.set(true);
    this.apiService.filterTracks(must, can, not).subscribe({
      next: (tracks) => {
        this.filteredTracks.set(tracks.map(t => this.apiService.mapToTrackEntry(t)));
        this.isFiltering.set(false);
      },
      error: () => this.isFiltering.set(false)
    });
  }

  saveAsPlaylist(): void {
    if (!this.newPlaylistName) {
      alert('Please enter a name for the new playlist.');
      return;
    }
    const ids = this.filteredTracks().map(t => t.trackId);
    if (ids.length === 0) {
      alert('No tracks to save.');
      return;
    }

    this.apiService.createPlaylist(this.newPlaylistName, ids).subscribe({
      next: (res) => {
        alert(`Playlist '${res.groupName}' created with ${ids.length} tracks!`);
        this.newPlaylistName = '';
        this.apiService.loadPlaylists();
        this.router.navigate(['/playlists']);
      },
      error: (err) => {
        console.error(err);
        alert('Failed to create playlist.');
      }
    });
  }

  playAll(): void {
    const playlist = this.filteredTracks();
    if (playlist.length > 0) {
      this.playbackService.playPlaylist(playlist, 'Mixer: Result');
    }
  }

  getTrackImageUrl(fileId: number): string {
    return `${this.apiService.getApiUrl()}/api/trackFileImage/${fileId}`;
  }
}
