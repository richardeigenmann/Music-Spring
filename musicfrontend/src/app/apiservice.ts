import { HttpClient } from '@angular/common/http';
import { Injectable, Signal, signal, inject } from '@angular/core';
import { Observable, tap, firstValueFrom, from, timer, map } from 'rxjs'; // Added map
import { switchMap, takeWhile } from 'rxjs/operators'; // Added switchMap, takeWhile

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
  [key: string]: string | number | string[] | TrackFile[] | undefined;
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
  tagDetails: { tagId: number; tagName: string; tagTypeName: string }[];
}

export interface ScanProgress {
  checked: number;
  added: number;
  totalEstimated: number;
  isDone: boolean;
  currentFile: string;
}

export interface BackendVersionInfo {
  version: string;
  totalTrackCount: number;
  buildTime: string;
  runtime: string;
  runtimeVersion: string;
  environment: string;
  musicDirectory: string;
  dbConnected: boolean;
  dbUrl?: string;
  dbUser?: string;
  dbError?: string;
}

@Injectable({
  providedIn: 'root',
})
export class ApiService {
  private readonly http = inject(HttpClient);
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

  constructor() {
    this.tagsPromise = new Promise((resolve) => (this.resolveTags = resolve));
    this.initPromise = this.loadConfig().then(() => {
      this.getVersion().subscribe();
      this.loadTags().subscribe();
    });
  }

  private async loadConfig(): Promise<void> {
    try {
      // Fetch from our local Express server
      const config = await firstValueFrom(this.http.get<{ apiUrl: string }>('/config'));
      this.API_URL = config.apiUrl;
      console.log('API URL initialized to:', this.API_URL);
      this.initialized = true;
    } catch {
      console.warn('Could not load dynamic config, falling back to default:', this.API_URL);
      this.initialized = true;
    }
  }

  loadPlaylistEntries(playlistId: number): void {
    if (!this.initialized) {
      this.initPromise.then(() => this.loadPlaylistEntries(playlistId));
      return;
    }
    this.http.get<Track[]>(`${this.API_URL}/api/tracksByTag/${playlistId}`).subscribe({
      next: (data) => {
        this.tagsPromise.then(() => {
          this._playlistEntries.set(data.map((track) => this.mapToTrackEntry(track)));
        });
      },
      error: (error) => console.error('Failed to load playlist entries data', error),
    });
  }

  mapToTrackEntry(track: Track): TrackEntry {
    const artistRaw = track['Artist'] || track['artist'];
    const artist = Array.isArray(artistRaw) ? artistRaw.join(', ') : (artistRaw as string) || '';

    const tagDetails: { tagId: number; tagName: string; tagTypeName: string }[] = [];
    const allTags = this._tags();

    if (allTags.length > 0) {
      // Keys to ignore (not group types)
      const internalKeys = ['trackId', 'trackName', 'files'];

      for (const key in track) {
        if (!internalKeys.includes(key)) {
          const value = track[key];
          if (value) {
            const groupNames = Array.isArray(value) ? value : [value];
            for (const name of groupNames) {
              if (typeof name === 'string') {
                const found = allTags.find(
                  (g) =>
                    g.tagTypeName.toLowerCase() === key.toLowerCase() &&
                    g.tagName.toLowerCase() === name.toLowerCase(),
                );
                if (found) {
                  tagDetails.push({
                    tagId: found.tagId,
                    tagName: found.tagName, // Use the name from DB for consistency
                    tagTypeName: found.tagTypeName,
                  });
                }
              }
            }
          }
        }
      }
    }

    return {
      trackId: track.trackId,
      title: track.trackName,
      trackName: track.trackName,
      artist: artist,
      album: (track['Album'] || track['album'] || '') as string,
      duration: track.files?.[0]?.duration || 0,
      fileId: track.files?.[0]?.fileId || 0,
      tagDetails: tagDetails,
    };
  }

  getTrack(id: number): Observable<Track> {
    return new Observable((observer) => {
      this.initPromise.then(() => {
        this.http.get<Track>(`${this.API_URL}/api/track/${id}`).subscribe(observer);
      });
    });
  }

  loadTags(): Observable<Tag[]> {
    if (!this.initialized) {
      return from(this.initPromise).pipe(switchMap(() => this.loadTags()));
    }
    return this.http.get<Tag[]>(`${this.API_URL}/api/tags`).pipe(
      map((data) =>
        data.map((g) => ({
          tagId: g.tagId,
          tagName: g.tagName,
          tagTypeName: g.tagTypeName,
          tagTypeId: g.tagTypeId,
          tagTypeEdit: g.tagTypeEdit,
        })),
      ),
      tap((mapped) => {
        this._tags.set(mapped);
        this.resolveTags();
      }),
    );
  }

  getTags(): Observable<Tag[]> {
    return new Observable((observer) => {
      this.initPromise.then(() => {
        const currentTags = this._tags();
        if (currentTags.length > 0) {
          observer.next(currentTags);
          observer.complete();
        } else {
          this.http.get<Tag[]>(`${this.API_URL}/api/tags`).subscribe({
            next: (data) => {
              const mapped = data.map((g) => ({
                tagId: g.tagId,
                tagName: g.tagName,
                tagTypeName: g.tagTypeName,
                tagTypeId: g.tagTypeId,
                tagTypeEdit: g.tagTypeEdit,
              }));
              this._tags.set(mapped);
              observer.next(mapped);
            },
            error: (err) => observer.error(err),
            complete: () => observer.complete(),
          });
        }
      });
    });
  }

  saveTrack(track: Track): Observable<Track> {
    return new Observable((observer) => {
      this.initPromise.then(() => {
        this.http
          .post<Track>(`${this.API_URL}/api/track/${track.trackId}`, track)
          .subscribe(observer);
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
    return new Observable((observer) => {
      this.initPromise.then(() => {
        this.http.post<void>(`${this.API_URL}/api/scanTracks`, {}).subscribe(observer);
      });
    });
  }

 getScanProgress(): Observable<ScanProgress> {
  return from(this.initPromise).pipe(
    // timer(0, 1000) fires immediately, then every 1 second
    switchMap(() => timer(0, 1000)),
    switchMap(() => {
      return this.http.get<ScanProgress & { done?: boolean }>(`${this.API_URL}/api/scanProgress`);
    }),
    map(p => ({
      ...p,
      isDone: p.isDone === true || p.done === true
    }) as ScanProgress),
    tap((progress) => console.log('Emitting progress:', progress)),
    // Continue while NOT done, inclusive of the final 'done' emission
    takeWhile((progress) => !progress.isDone, true),
  );
}

  getTagUsageStats(): Observable<
    { typeName: string; tagName: string; count: number; tagId: number }[]
  > {
    return new Observable((observer) => {
      this.initPromise.then(() => {
        this.http.get<Record<string, string | number>[]>(`${this.API_URL}/api/stats/tagUsage`).subscribe({
          next: (data) => {
            observer.next(
              data.map((d) => ({
                typeName: (d['typeName'] ?? d['typename'] ?? d['TYPENAME'] ?? '') as string,
                tagName: (d['tagName'] ?? d['groupname'] ?? d['GROUPNAME'] ?? '') as string,
                count: (d['count'] ?? d['COUNT'] ?? 0) as number,
                tagId: (d['tagId'] ?? d['groupid'] ?? d['GROUPID'] ?? 0) as number,
              })),
            );
          },
          error: (err) => observer.error(err),
          complete: () => observer.complete(),
        });
      });
    });
  }

  getUnclassifiedTracks(): Observable<TrackEntry[]> {
    return new Observable((observer) => {
      this.initPromise.then(() => {
        this.http.get<Track[]>(`${this.API_URL}/api/unclassifiedTracks`).subscribe({
          next: (data) => {
            this.tagsPromise.then(() => {
              observer.next(data.map((track) => this.mapToTrackEntry(track)));
              observer.complete();
            });
          },
          error: (err) => observer.error(err),
        });
      });
    });
  }

  searchTracks(query: string): Observable<TrackEntry[]> {
    return new Observable((observer) => {
      this.initPromise.then(() => {
        this.http
          .get<Track[]>(`${this.API_URL}/api/trackSearch?query=${encodeURIComponent(query)}`)
          .subscribe({
            next: (data) => {
              this.tagsPromise.then(() => {
                observer.next(data.map((track) => this.mapToTrackEntry(track)));
                observer.complete();
              });
            },
            error: (err) => observer.error(err),
          });
      });
    });
  }

  filterTracks(
    mustHaveIds: number[],
    canHaveIds: number[],
    mustNotHaveIds: number[],
  ): Observable<TrackEntry[]> {
    return new Observable((observer) => {
      this.initPromise.then(() => {
        this.http
          .post<
            Track[]
          >(`${this.API_URL}/api/filterTracks`, { mustHaveIds, canHaveIds, mustNotHaveIds })
          .subscribe({
            next: (data) => {
              this.tagsPromise.then(() => {
                observer.next(data.map((track) => this.mapToTrackEntry(track)));
                observer.complete();
              });
            },
            error: (err) => observer.error(err),
          });
      });
    });
  }

  createTag(tagType: string, tagName: string, trackIds: number[] = []): Observable<Tag> {
    return new Observable((observer) => {
      this.initPromise.then(() => {
        this.http.post<Tag>(`${this.API_URL}/api/tag`, { tagType, tagName, trackIds }).subscribe({
          next: (data) => {
            const newTag: Tag = {
              tagId: data.tagId,
              tagName: data.tagName,
              tagTypeName: data.tagTypeName,
              tagTypeId: data.tagTypeId,
              tagTypeEdit: data.tagTypeEdit,
            };
            // Wait for refresh to complete before emitting newTag
            this.loadTags().subscribe({
              next: () => {
                observer.next(newTag);
                observer.complete();
              },
              error: (err) => {
                console.error('Failed to reload tags after creation', err);
                observer.next(newTag);
                observer.complete();
              },
            });
          },
          error: (err) => observer.error(err),
        });
      });
    });
  }

  deleteTag(tagId: number): Observable<void> {
    return new Observable((observer) => {
      this.initPromise.then(() => {
        this.http.delete<void>(`${this.API_URL}/api/tag/${tagId}`).subscribe({
          next: () => {
            // Wait for refresh to complete before emitting
            this.loadTags().subscribe({
              next: () => {
                observer.next();
                observer.complete();
              },
              error: (err) => {
                console.error('Failed to reload tags after deletion', err);
                observer.next();
                observer.complete();
              },
            });
          },
          error: (err) => observer.error(err),
        });
      });
    });
  }

  downloadTagAsM3u(tagId: number): Observable<Blob> {
    return new Observable((observer) => {
      this.initPromise.then(() => {
        this.http
          .get(`${this.API_URL}/api/tag/${tagId}/m3u`, { responseType: 'blob' })
          .subscribe(observer);
      });
    });
  }

  downloadTagAsZip(tagId: number): Observable<Blob> {
    return new Observable((observer) => {
      this.initPromise.then(() => {
        this.http
          .get(`${this.API_URL}/api/tag/${tagId}/zip`, { responseType: 'blob' })
          .subscribe(observer);
      });
    });
  }

  getFrontendUrl(): string {
    return window.location.origin;
  }

  getVersion(): Observable<BackendVersionInfo> {
    return new Observable((observer) => {
      this.initPromise.then(() => {
        this.http
          .get<BackendVersionInfo>(`${this.API_URL}/api/version`)
          .pipe(
            tap((data) => {
              if (data.totalTrackCount !== undefined) {
                this._totalTrackCount.set(data.totalTrackCount);
              }
            }),
          )
          .subscribe(observer);
      });
    });
  }
}
