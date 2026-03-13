import { Component, OnInit, inject, signal } from '@angular/core';
import { ApiService, TrackEntry } from '../apiservice';
import { Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { PlaybackService } from '../playback.service';

@Component({
  selector: 'app-unclassified-tracks',
  standalone: true,
  imports: [RouterLink, CommonModule],
  templateUrl: './unclassified-tracks.html',
  styleUrls: ['./unclassified-tracks.css']
})
export class UnclassifiedTracks implements OnInit {
  private apiService = inject(ApiService);
  private playbackService = inject(PlaybackService);
  private router = inject(Router);
  tracks = signal<TrackEntry[]>([]);

  constructor() {}

  ngOnInit(): void {
    this.loadTracks();
  }

  loadTracks(): void {
    console.log('Loading unclassified tracks...');
    this.apiService.getUnclassifiedTracks().subscribe({
      next: (tracks) => {
        console.log('Received tracks from backend (count):', tracks.length);
        console.log('First track raw:', tracks[0]);
        const mapped = tracks.map(t => {
          try {
            return this.apiService.mapToTrackEntry(t);
          } catch (e) {
            console.error('Error mapping track:', t, e);
            return null;
          }
        }).filter(t => t !== null) as TrackEntry[];
        console.log('Mapped tracks (count):', mapped.length);
        this.tracks.set(mapped);
      },
      error: (err) => {
        console.error('Error loading unclassified tracks:', err);
      }
    });
  }

  playAll(): void {
    const playlist = this.tracks();
    this.playbackService.playPlaylist(playlist, 'Unclassified Tracks');
  }
}
