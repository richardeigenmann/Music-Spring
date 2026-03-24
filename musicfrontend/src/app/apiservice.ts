import { HttpClient } from '@angular/common/http';
import { Injectable, Signal, signal } from '@angular/core';
import { Observable, tap, firstValueFrom } from 'rxjs';

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
  //private API_URL = 'http://octan:8011'; // Docker backend container
  private API_URL = 'http://localhost:8002'; // backend bootRunPg or bootRunH2
  private initialized = false;
  private initPromise: Promise<void>;

  private readonly _playlistEntries = signal<TrackEntry[]>([]);
  private readonly _totalTrackCount = signal<number>(0);

  readonly playlistEntries: Signal<TrackEntry[]> = this._playlistEntries.asReadonly();
  readonly totalTrackCount: Signal<number> = this._totalTrackCount.asReadonly();

  constructor(private http: HttpClient) {
    this.initPromise = this.loadConfig().then(() => {
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
        this.http.get<any[]>(`${this.API_URL}/api/groups`).subscribe({
          next: (data) => {
            const mapped = data.map(g => ({
              groupId: g.groupId ?? g.GroupId,
              groupName: g.groupName ?? g.GroupName,
              groupTypeName: g.groupTypeName ?? g.GroupTypeName,
              groupTypeId: g.groupTypeId ?? g.GroupTypeId
            }));
            observer.next(mapped);
          },
          error: (err) => observer.error(err),
          complete: () => observer.complete()
        });
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

  getActuatorUrl(): string {
    return this.API_URL + '/actuator';
  }

  getHealthUrl(): string {
    return this.API_URL + '/actuator/health';
  }

  getInfoUrl(): string {
    return this.API_URL + '/actuator/info';
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

  getGroupUsageStats(): Observable<{ typeName: string, groupName: string, count: number, groupId: number }[]> {
    return new Observable(observer => {
      this.initPromise.then(() => {
        this.http.get<any[]>(`${this.API_URL}/api/stats/groupUsage`).subscribe({
          next: (data) => {
            observer.next(data.map(d => ({
              typeName: d.typeName ?? d.typename ?? d.TYPENAME,
              groupName: d.groupName ?? d.groupname ?? d.GROUPNAME,
              count: d.count ?? d.COUNT,
              groupId: d.groupId ?? d.groupid ?? d.GROUPID
            })));
          },
          error: (err) => observer.error(err),
          complete: () => observer.complete()
        });
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

  createGroup(groupType: string, groupName: string): Observable<Group> {
    return new Observable(observer => {
      this.initPromise.then(() => {
        this.http.post<any>(`${this.API_URL}/api/group`, { groupType, groupName }).subscribe({
          next: (data) => {
            observer.next({
              groupId: data.groupId,
              groupName: data.groupName,
              groupTypeName: data.groupTypeName,
              groupTypeId: data.groupTypeId
            });
          },
          error: (err) => observer.error(err),
          complete: () => observer.complete()
        });
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

  downloadGroupAsM3u(groupId: number): Observable<Blob> {
    return new Observable(observer => {
      this.initPromise.then(() => {
        this.http.get(`${this.API_URL}/api/group/${groupId}/m3u`, { responseType: 'blob' }).subscribe(observer);
      });
    });
  }

  downloadGroupAsZip(groupId: number): Observable<Blob> {
    return new Observable(observer => {
      this.initPromise.then(() => {
        this.http.get(`${this.API_URL}/api/group/${groupId}/zip`, { responseType: 'blob' }).subscribe(observer);
      });
    });
  }

  getFrontendUrl(): string {
    return window.location.origin;
  }

  getVersion(): Observable<any> {
    return new Observable(observer => {
      this.initPromise.then(() => {
        this.http.get<any>(`${this.API_URL}/api/version`).subscribe(observer);
      });
    });
  }
}
