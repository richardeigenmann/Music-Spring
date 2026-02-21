import { HttpClient } from '@angular/common/http';
import { Injectable, Signal, signal } from '@angular/core';
import { Observable, tap, firstValueFrom } from 'rxjs';

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
    groupDetails: { groupId: number, groupName: string, groupTypeName: string }[];
}


@Injectable({
  providedIn: 'root',
})
export class ApiService {
  private API_URL = 'http://localhost:8002'; // Fallback
  private initialized = false;
  private initPromise: Promise<void>;

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
    this.initPromise = this.loadConfig().then(() => {
      this.loadPlaylists();
      this.loadTotalTrackCount();
    });
  }

  private async loadConfig(): Promise<void> {
    try {
      // Fetch from our local Express server
      const config = await firstValueFrom(this.http.get<{ apiUrl: string }>('/config'));
      this.API_URL = config.apiUrl;
      console.log('API URL initialized to:', this.API_URL);
      this.initialized = true;
    } catch (e) {
      console.warn('Could not load dynamic config, falling back to default:', this.API_URL);
      this.initialized = true;
    }
  }

  loadTotalTrackCount(): void {
    if (!this.initialized) { this.initPromise.then(() => this.loadTotalTrackCount()); return; }
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
    if (!this.initialized) { this.initPromise.then(() => this.loadPlaylists()); return; }
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
    if (!this.initialized) { this.initPromise.then(() => this.loadPlaylistEntries(playlistId)); return; }
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
    
    const groupDetails = (track.GroupDetails || []).map((g: any) => ({
      groupId: g.GroupId,
      groupName: g.GroupName,
      groupTypeName: g.GroupTypeName
    }));

    return {
      trackId: track.TrackId,
      title: track.TrackName,
      trackName: track.TrackName,
      artist: artist,
      album: track['Album'],
      duration: track.Files?.[0]?.Duration || 0,
      fileId: track.Files?.[0]?.FileId ?? 0,
      groupDetails: groupDetails
    };
  }

  getTrack(id: number): Observable<Track> {
    return new Observable(observer => {
      this.initPromise.then(() => {
        this.http.get<Track>(`${this.API_URL}/api/track/${id}`).subscribe(observer);
      });
    });
  }

  getGroups(): Observable<Group[]> {
    return new Observable(observer => {
      this.initPromise.then(() => {
        this.http.get<Group[]>(`${this.API_URL}/api/groups`).subscribe(observer);
      });
    });
  }

  saveTrack(track: Track): Observable<Track> {
    return new Observable(observer => {
      this.initPromise.then(() => {
        this.http.post<Track>(`${this.API_URL}/api/track/${track.TrackId}`, track).subscribe(observer);
      });
    });
  }

  getApiUrl(): string {
    return this.API_URL;
  }

  scanTracks(): Observable<void> {
    return new Observable(observer => {
      this.initPromise.then(() => {
        this.http.post<void>(`${this.API_URL}/api/scanTracks`, {}).subscribe(observer);
      });
    });
  }

  getScanProgress(): Observable<{ checked: number, added: number, totalEstimated: number, isDone: boolean, currentFile: string }> {
    return new Observable(observer => {
      this.initPromise.then(() => {
        this.http.get<{ checked: number, added: number, totalEstimated: number, isDone: boolean, currentFile: string }>(`${this.API_URL}/api/scanProgress`).subscribe(observer);
      });
    });
  }

  getUnclassifiedTracks(): Observable<Track[]> {
    return new Observable(observer => {
      this.initPromise.then(() => {
        this.http.get<Track[]>(`${this.API_URL}/api/unclassifiedTracks`).subscribe(observer);
      });
    });
  }

  searchTracks(query: string): Observable<Track[]> {
    return new Observable(observer => {
      this.initPromise.then(() => {
        this.http.get<Track[]>(`${this.API_URL}/api/trackSearch?query=${encodeURIComponent(query)}`).subscribe(observer);
      });
    });
  }

  filterTracks(mustHaveIds: number[], canHaveIds: number[], mustNotHaveIds: number[]): Observable<Track[]> {
    return new Observable(observer => {
      this.initPromise.then(() => {
        this.http.post<Track[]>(`${this.API_URL}/api/filterTracks`, { mustHaveIds, canHaveIds, mustNotHaveIds }).subscribe(observer);
      });
    });
  }

  createPlaylist(name: string, trackIds: number[]): Observable<{ groupId: number, groupName: string }> {
    return new Observable(observer => {
      this.initPromise.then(() => {
        this.http.post<{ groupId: number, groupName: string }>(`${this.API_URL}/api/createPlaylist`, { name, trackIds }).subscribe(observer);
      });
    });
  }

  deleteGroup(groupId: number): Observable<void> {
    return new Observable(observer => {
      this.initPromise.then(() => {
        this.http.delete<void>(`${this.API_URL}/api/group/${groupId}`).subscribe(observer);
      });
    });
  }
}
