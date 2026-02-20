import { HttpClient } from '@angular/common/http';
import { Injectable, Signal, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';

export interface PlaylistEntry {
  groupName: string,
  tracks: number,
  groupId: number,
}

export interface Group {
  groupTypeName: string;
  groupTypeId: number;
  groupName: string;
  groupId: number;
}

export interface TrackFile {
  FileId: number;
  FileName: string;
  FileLocation: string;
  FileOnline: string;
  Duration: number;
  BackupDate: string;
}

export interface Track {
  TrackId: number;
  TrackName: string;
  [key: string]: any;
  Files: TrackFile[];
}

export interface TrackEntry {
    trackId: number;
    title: string;
    trackName: string;
    artist: string;
    album: string;
    duration: number;
    fileId: number;
}


@Injectable({
  providedIn: 'root',
})
export class ApiService {
  private readonly API_URL = 'http://localhost:8002';
  private readonly _playlists = signal<PlaylistEntry[]>([]);
  private readonly _playlistEntries = signal<TrackEntry[]>([]);
  private readonly _activePlaylist = signal<TrackEntry[]>([]);
  private readonly _activePlaylistName = signal<string>('');
  private readonly _totalTrackCount = signal<number>(0);

  readonly playlists: Signal<PlaylistEntry[]> = this._playlists.asReadonly();
  readonly playlistEntries: Signal<TrackEntry[]> = this._playlistEntries.asReadonly();
  readonly activePlaylist: Signal<TrackEntry[]> = this._activePlaylist.asReadonly();
  readonly activePlaylistName: Signal<string> = this._activePlaylistName.asReadonly();
  readonly totalTrackCount: Signal<number> = this._totalTrackCount.asReadonly();

  constructor(private http: HttpClient) {
    this.loadPlaylists();
    this.loadTotalTrackCount();
  }

  loadTotalTrackCount(): void {
    this.http.get<{ count: number }>(`${this.API_URL}/api/totalTrackCount`)
      .subscribe({
        next: (data) => this._totalTrackCount.set(data.count),
        error: (error) => console.error('Failed to load total track count', error)
      });
  }

  setActivePlaylist(playlist: TrackEntry[], name: string = 'Playlist'): void {
    this._activePlaylist.set(playlist);
    this._activePlaylistName.set(name);
  }

  loadPlaylists(): void {
    this.http.get<PlaylistEntry[]>(this.API_URL+'/api/playlists')
      .pipe(
        tap(data => {
          this._playlists.set(data);
        })
      )
      .subscribe({
        error: (error) => console.error('Failed to load playlists data', error)
      });
  }

  loadPlaylistEntries(playlistId: number): void {
    this.http.get<Track[]>(`${this.API_URL}/api/tracksByGroup/${playlistId}`)
      .pipe(
        tap(data => {
          this._playlistEntries.set(data.map(track => this.mapToTrackEntry(track)));
        })
      )
      .subscribe({
        error: (error) => console.error('Failed to load playlist entries data', error)
      });
  }

  mapToTrackEntry(track: any): TrackEntry {
    const artistRaw = track['Artist'];
    const artist = Array.isArray(artistRaw) ? artistRaw.join(', ') : (artistRaw || '');
    return {
      trackId: track.TrackId,
      title: track.TrackName,
      trackName: track.TrackName,
      artist: artist,
      album: track['Album'],
      duration: track.Files[0]?.Duration || 0,
      fileId: track.Files[0]?.FileId ?? 0
    };
  }

  getTrack(id: number): Observable<Track> {
    return this.http.get<Track>(`${this.API_URL}/api/track/${id}`);
  }

  getGroups(): Observable<Group[]> {
    return this.http.get<Group[]>(`${this.API_URL}/api/groups`);
  }

  saveTrack(track: Track): Observable<Track> {
    return this.http.post<Track>(`${this.API_URL}/api/track/${track.TrackId}`, track);
  }

  getApiUrl(): string {
    return this.API_URL;
  }

  scanTracks(): Observable<void> {
    return this.http.post<void>(`${this.API_URL}/api/scanTracks`, {});
  }

  getScanProgress(): Observable<{ checked: number, added: number, totalEstimated: number, isDone: boolean, currentFile: string }> {
    return this.http.get<{ checked: number, added: number, totalEstimated: number, isDone: boolean, currentFile: string }>(`${this.API_URL}/api/scanProgress`);
  }

  getUnclassifiedTracks(): Observable<Track[]> {
    return this.http.get<Track[]>(`${this.API_URL}/api/unclassifiedTracks`);
  }

  searchTracks(query: string): Observable<Track[]> {
    return this.http.get<Track[]>(`${this.API_URL}/api/trackSearch?query=${encodeURIComponent(query)}`);
  }
}
