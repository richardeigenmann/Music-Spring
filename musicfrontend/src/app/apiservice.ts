import { HttpClient } from '@angular/common/http';
import { Injectable, Signal, signal } from '@angular/core';
import { tap } from 'rxjs';

export interface PlaylistEntry {
  groupName: string,
  tracks: number,
  groupId: number,
}

export interface TrackFile {
  FileName: string;
  FileLocation: string;
  FileOnline: string;
  Duration: number;
  BackupDate: string;
}

export interface Track {
  TrackName: string;
  Artist: string;
  Album: string;
  Files: TrackFile[];
}

export interface TrackEntry {
    trackName: string;
    artist: string;
    album: string;
    duration: number;
}


@Injectable({
  providedIn: 'root',
})
export class ApiService {
  private readonly API_URL = 'http://localhost:8002';
  private readonly _playlists = signal<PlaylistEntry[]>([]);
  private readonly _playlistEntries = signal<TrackEntry[]>([]);

  // Expose the signal as read-only for use in components
  readonly playlists: Signal<PlaylistEntry[]> = this._playlists.asReadonly();
  readonly playlistEntries: Signal<TrackEntry[]> = this._playlistEntries.asReadonly();

  constructor(private http: HttpClient) {
    this.loadPlaylists();
  }

    /**
   * Fetches the playlists from the remote URL and updates the signal.
   */
  loadPlaylists(): void {
    console.log('Fetching playlists data from URL...');
    this.http.get<PlaylistEntry[]>(this.API_URL+'/api/playlists')
      .pipe(
        tap(data => {
          console.log('Playlists data loaded successfully.');
          this._playlists.set(data);
        })
      )
      .subscribe({
        error: (error) => console.error('Failed to load playlists data', error)
      });
  }

  loadPlaylistEntries(playlistId: number): void {
    console.log('Fetching playlist entries from URL...');
    this.http.get<Track[]>(`${this.API_URL}/api/tracksByGroup/${playlistId}`)
      .pipe(
        tap(data => {
          console.log('Playlist entries data loaded successfully.');
          this._playlistEntries.set(data.map(track => ({
            trackName: track.TrackName,
            artist: track.Artist,
            album: track.Album,
            duration: track.Files[0]?.Duration || 0
          })));
        })
      )
      .subscribe({
        error: (error) => console.error('Failed to load playlist entries data', error)
      });
  }
}
