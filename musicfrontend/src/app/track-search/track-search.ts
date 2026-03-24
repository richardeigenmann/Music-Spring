import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { ApiService, TrackEntry } from '../apiservice';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { PlaybackService } from '../playback.service';
import { TrackList } from '../shared/track-list/track-list.component';

@Component({
  selector: 'app-track-search',
  standalone: true,
  imports: [CommonModule, TrackList],
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
  currentTrackId = computed(() => this.playbackService.currentTrack()?.trackId || null);

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
      this.tracks.set(tracks);
    });
  }

  playAll(): void {
    const playlist = this.tracks();
    this.playbackService.playPlaylist(playlist, 'Search: ' + this.query());
  }
}
