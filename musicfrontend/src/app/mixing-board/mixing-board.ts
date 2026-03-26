import { Component, OnInit, inject, signal } from '@angular/core';
import { ApiService, Tag, TrackEntry } from '../apiservice';
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

  // Tag lists for drag and drop
  availableTags = signal<Tag[]>([]);
  mustHave = signal<Tag[]>([]);
  canHave = signal<Tag[]>([]);
  mustNotHave = signal<Tag[]>([]);

  // Filtering results
  filteredTracks = signal<TrackEntry[]>([]);
  isFiltering = signal(false);

  // New playlist name
  newPlaylistName = '';

  ngOnInit(): void {
    this.apiService.getTags().subscribe(tags => {
      const selectionTags = tags.filter(t => t.tagTypeEdit === 'S');
      this.availableTags.set(selectionTags.sort((a, b) => a.tagName.localeCompare(b.tagName)));
    });
  }

  drop(event: CdkDragDrop<Tag[]>): void {
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

  private setSignalData(id: string, data: Tag[]): void {
    if (id === 'availableList') this.availableTags.set(data);
    else if (id === 'mustList') this.mustHave.set(data);
    else if (id === 'canList') this.canHave.set(data);
    else if (id === 'notList') this.mustNotHave.set(data);
  }

  updateResults(): void {
    const must = this.mustHave().map(t => t.tagId);
    const can = this.canHave().map(t => t.tagId);
    const not = this.mustNotHave().map(t => t.tagId);

    if (must.length === 0 && can.length === 0 && not.length === 0) {
      this.filteredTracks.set([]);
      return;
    }

    this.isFiltering.set(true);
    this.apiService.filterTracks(must, can, not).subscribe({
      next: (tracks) => {
        this.filteredTracks.set(tracks);
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
        alert(`Playlist '${res.tagName}' created with ${ids.length} tracks!`);
        this.newPlaylistName = '';
        this.router.navigate(['/tags']);
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
