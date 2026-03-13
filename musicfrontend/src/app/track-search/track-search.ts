import { Component, OnInit, inject, signal } from '@angular/core';
import { ApiService, TrackEntry } from '../apiservice';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { PlaybackService } from '../playback.service';

@Component({
  selector: 'app-track-search',
  standalone: true,
  imports: [RouterLink, CommonModule],
  templateUrl: './track-search.html',
  styleUrls: ['./track-search.css']
})
export class TrackSearch implements OnInit {
  private apiService = inject(ApiService);
  private playbackService = inject(PlaybackService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  tracks = signal<TrackEntry[]>([]);
  query = signal<string>('');

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      const q = params['q'] || '';
      this.query.set(q);
      if (q) {
        this.loadTracks(q);
      } else {
        this.tracks.set([]);
      }
    });
  }

  loadTracks(q: string): void {
    this.apiService.searchTracks(q).subscribe(tracks => {
      this.tracks.set(tracks.map(t => this.apiService.mapToTrackEntry(t)));
    });
  }

  playAll(): void {
    const playlist = this.tracks();
    this.playbackService.playPlaylist(playlist, 'Search Results: ' + this.query());
  }
}
