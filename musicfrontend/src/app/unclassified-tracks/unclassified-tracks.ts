import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { ApiService, TrackEntry } from '../apiservice';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { PlaybackService } from '../playback.service';
import { TrackList } from '../shared/track-list/track-list.component';

@Component({
  selector: 'app-unclassified-tracks',
  standalone: true,
  imports: [CommonModule, TrackList],
  templateUrl: './unclassified-tracks.html',
  styleUrls: ['./unclassified-tracks.css']
})
export class UnclassifiedTracks implements OnInit {
  private apiService = inject(ApiService);
  private playbackService = inject(PlaybackService);
  private router = inject(Router);
  tracks = signal<TrackEntry[]>([]);
  currentTrackId = computed(() => this.playbackService.currentTrack()?.trackId || null);

  ngOnInit(): void {
    this.loadTracks();
  }

  loadTracks(): void {
    console.log('Loading unclassified tracks...');
    this.apiService.getUnclassifiedTracks().subscribe({
      next: (tracks) => {
        console.log('Received tracks from backend (count):', tracks.length);
        this.tracks.set(tracks);
      },
      error: (err) => {
        console.error('Error loading unclassified tracks:', err);
      }
    });
  }

  playAll(): void {
    const playlist = this.tracks();
    this.playbackService.playPlaylist(playlist, 'Status: Unclassified');
  }
}
