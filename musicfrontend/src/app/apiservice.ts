import { HttpClient } from '@angular/common/http';
import { Injectable, Signal, signal } from '@angular/core';
import { Observable, tap, firstValueFrom } from 'rxjs';

export interface Tag {
  tagTypeName: string;
  tagTypeId: number;
  tagName: string;
  tagId: number;
  tagTypeEdit: string;
}

export interface TrackFile {
  fileId: number;
  fileName: string;
  duration: number;
}

export interface Track {
  trackId: number;
  trackName: string;
  [key: string]: any;
  files: TrackFile[];
}

export interface TrackEntry {
    trackId: number;
    title: string;
    trackName: string;
    artist: string;
    album: string;
    duration: number;
    fileId: number;
    tagDetails: { tagId: number, tagName: string, tagTypeName: string }[];
}


@Injectable({
  providedIn: 'root',
})
export class ApiService {
  //private API_URL = 'http://octan:8011'; // Docker backend container
  private API_URL = 'http://localhost:8002'; // backend bootRunPg or bootRunH2
  private initialized = false;
  private initPromise: Promise<void>;
  private tagsPromise: Promise<void>;
  private resolveTags!: () => void;

  private readonly _playlistEntries = signal<TrackEntry[]>([]);
  private readonly _totalTrackCount = signal<number>(0);
  private readonly _tags = signal<Tag[]>([]);

  readonly playlistEntries: Signal<TrackEntry[]> = this._playlistEntries.asReadonly();
  readonly totalTrackCount: Signal<number> = this._totalTrackCount.asReadonly();
  readonly tags: Signal<Tag[]> = this._tags.asReadonly();

  constructor(private http: HttpClient) {
    this.tagsPromise = new Promise(resolve => this.resolveTags = resolve);
    this.initPromise = this.loadConfig().then(() => {
      this.getVersion().subscribe();
      this.loadTags();
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

  loadPlaylistEntries(playlistId: number): void {
    if (!this.initialized) { this.initPromise.then(() => this.loadPlaylistEntries(playlistId)); return; }
    this.http.get<Track[]>(`${this.API_URL}/api/tracksByTag/${playlistId}`)
      .subscribe({
        next: (data) => {
          this.tagsPromise.then(() => {
            this._playlistEntries.set(data.map(track => this.mapToTrackEntry(track)));
          });
        },
        error: (error) => console.error('Failed to load playlist entries data', error)
      });
  }

  mapToTrackEntry(track: any): TrackEntry {
    const artistRaw = track['Artist'] || track['artist'];
    const artist = Array.isArray(artistRaw) ? artistRaw.join(', ') : (artistRaw || '');

    const tagDetails: { tagId: number, tagName: string, tagTypeName: string }[] = [];
    const allTags = this._tags();
    
    if (allTags.length > 0) {
      // Keys to ignore (not group types)
      const internalKeys = ['trackId', 'trackName', 'files', 'trackId', 'trackName', 'files'];
      
      for (const key in track) {
        if (!internalKeys.includes(key)) {
          const value = track[key];
          if (value) {
            const groupNames = Array.isArray(value) ? value : [value];
            for (const name of groupNames) {
              if (typeof name === 'string') {
                const found = allTags.find(g => 
                  g.tagTypeName.toLowerCase() === key.toLowerCase() && 
                  g.tagName.toLowerCase() === name.toLowerCase()
                );
                if (found) {
                  tagDetails.push({
                    tagId: found.tagId,
                    tagName: found.tagName, // Use the name from DB for consistency
                    tagTypeName: found.tagTypeName
                  });
                }
              }
            }
          }
        }
      }
    }

    return {
      trackId: track.trackId || track.trackId,
      title: track.trackName || track.trackName,
      trackName: track.trackName || track.trackName,
      artist: artist,
      album: track['Album'] || track['album'],
      duration: track.files?.[0]?.duration || track.files?.[0]?.duration || 0,
      fileId: track.files?.[0]?.fileId || track.files?.[0]?.fileId || 0,
      tagDetails: tagDetails
    };
  }

  getTrack(id: number): Observable<Track> {
    return new Observable(observer => {
      this.initPromise.then(() => {
        this.http.get<Track>(`${this.API_URL}/api/track/${id}`).subscribe(observer);
      });
    });
  }

  loadTags(): void {
    if (!this.initialized) { this.initPromise.then(() => this.loadTags()); return; }
    this.http.get<any[]>(`${this.API_URL}/api/tags`)
      .subscribe({
        next: (data) => {
          const mapped = data.map(g => ({
            tagId: g.tagId ?? g.tagId,
            tagName: g.tagName ?? g.tagName,
            tagTypeName: g.tagTypeName ?? g.tagTypeName,
            tagTypeId: g.tagTypeId ?? g.tagTypeId,
            tagTypeEdit: g.tagTypeEdit ?? g.tagTypeEdit
          }));
          this._tags.set(mapped);
          this.resolveTags();
        },
        error: (err) => {
          console.error('Failed to load tags', err);
          this.resolveTags(); // Still resolve to avoid hangs
        }
      });
  }

  getTags(): Observable<Tag[]> {
    return new Observable(observer => {
      this.initPromise.then(() => {
        const currentTags = this._tags();
        if (currentTags.length > 0) {
          observer.next(currentTags);
          observer.complete();
        } else {
          this.http.get<any[]>(`${this.API_URL}/api/tags`).subscribe({
            next: (data) => {
              const mapped = data.map(g => ({
                tagId: g.tagId ?? g.tagId,
                tagName: g.tagName ?? g.tagName,
                tagTypeName: g.tagTypeName ?? g.tagTypeName,
                tagTypeId: g.tagTypeId ?? g.tagTypeId,
                tagTypeEdit: g.tagTypeEdit ?? g.tagTypeEdit
              }));
              this._tags.set(mapped);
              observer.next(mapped);
            },
            error: (err) => observer.error(err),
            complete: () => observer.complete()
          });
        }
      });
    });
  }

  saveTrack(track: Track): Observable<Track> {
    return new Observable(observer => {
      this.initPromise.then(() => {
        this.http.post<Track>(`${this.API_URL}/api/track/${track.trackId}`, track).subscribe(observer);
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

  getTagUsageStats(): Observable<{ typeName: string, tagName: string, count: number, tagId: number }[]> {
    return new Observable(observer => {
      this.initPromise.then(() => {
        this.http.get<any[]>(`${this.API_URL}/api/stats/tagUsage`).subscribe({
          next: (data) => {
            observer.next(data.map(d => ({
              typeName: d.typeName ?? d.typename ?? d.TYPENAME,
              tagName: d.tagName ?? d.groupname ?? d.GROUPNAME,
              count: d.count ?? d.COUNT,
              tagId: d.tagId ?? d.groupid ?? d.GROUPID
            })));
          },
          error: (err) => observer.error(err),
          complete: () => observer.complete()
        });
      });
    });
  }

  getUnclassifiedTracks(): Observable<TrackEntry[]> {
    return new Observable(observer => {
      this.initPromise.then(() => {
        this.http.get<Track[]>(`${this.API_URL}/api/unclassifiedTracks`).subscribe({
          next: (data) => {
            this.tagsPromise.then(() => {
              observer.next(data.map(track => this.mapToTrackEntry(track)));
              observer.complete();
            });
          },
          error: (err) => observer.error(err)
        });
      });
    });
  }

  searchTracks(query: string): Observable<TrackEntry[]> {
    return new Observable(observer => {
      this.initPromise.then(() => {
        this.http.get<Track[]>(`${this.API_URL}/api/trackSearch?query=${encodeURIComponent(query)}`).subscribe({
          next: (data) => {
            this.tagsPromise.then(() => {
              observer.next(data.map(track => this.mapToTrackEntry(track)));
              observer.complete();
            });
          },
          error: (err) => observer.error(err)
        });
      });
    });
  }

  filterTracks(mustHaveIds: number[], canHaveIds: number[], mustNotHaveIds: number[]): Observable<TrackEntry[]> {
    return new Observable(observer => {
      this.initPromise.then(() => {
        this.http.post<Track[]>(`${this.API_URL}/api/filterTracks`, { mustHaveIds, canHaveIds, mustNotHaveIds }).subscribe({
          next: (data) => {
            this.tagsPromise.then(() => {
              observer.next(data.map(track => this.mapToTrackEntry(track)));
              observer.complete();
            });
          },
          error: (err) => observer.error(err)
        });
      });
    });
  }

  createPlaylist(name: string, trackIds: number[]): Observable<{ tagId: number, tagName: string }> {
    return new Observable(observer => {
      this.initPromise.then(() => {
        this.http.post<{ tagId: number, tagName: string }>(`${this.API_URL}/api/createPlaylist`, { name, trackIds }).subscribe(observer);
      });
    });
  }

  createTag(groupType: string, tagName: string): Observable<Tag> {
    return new Observable(observer => {
      this.initPromise.then(() => {
        this.http.post<any>(`${this.API_URL}/api/tag`, { groupType, tagName }).subscribe({
          next: (data) => {
            observer.next({
              tagId: data.tagId,
              tagName: data.tagName,
              tagTypeName: data.tagTypeName,
              tagTypeId: data.tagTypeId,
              tagTypeEdit: data.tagTypeEdit
            });
          },
          error: (err) => observer.error(err),
          complete: () => observer.complete()
        });
      });
    });
  }

  deleteTag(tagId: number): Observable<void> {
    return new Observable(observer => {
      this.initPromise.then(() => {
        this.http.delete<void>(`${this.API_URL}/api/tag/${tagId}`).subscribe(observer);
      });
    });
  }

  downloadTagAsM3u(tagId: number): Observable<Blob> {
    return new Observable(observer => {
      this.initPromise.then(() => {
        this.http.get(`${this.API_URL}/api/tag/${tagId}/m3u`, { responseType: 'blob' }).subscribe(observer);
      });
    });
  }

  downloadTagAsZip(tagId: number): Observable<Blob> {
    return new Observable(observer => {
      this.initPromise.then(() => {
        this.http.get(`${this.API_URL}/api/tag/${tagId}/zip`, { responseType: 'blob' }).subscribe(observer);
      });
    });
  }

  getFrontendUrl(): string {
    return window.location.origin;
  }

  getVersion(): Observable<any> {
    return new Observable(observer => {
      this.initPromise.then(() => {
        this.http.get<any>(`${this.API_URL}/api/version`).pipe(
            tap(data => {
                if (data.totalTrackCount !== undefined) {
                    this._totalTrackCount.set(data.totalTrackCount);
                }
            })
        ).subscribe(observer);
      });
    });
  }
}
