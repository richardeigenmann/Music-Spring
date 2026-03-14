import { Injectable, computed, effect, inject, signal } from '@angular/core';
import { TrackEntry } from './apiservice';

export interface PlaybackState {
  playlistIds: number[];
  playlistName: string;
  currentIndex: number;
  isShuffled: boolean;
  shuffledIds: number[];
}

@Injectable({
  providedIn: 'root'
})
export class PlaybackService {
  private readonly TRACKS_KEY = 'music_track_registry';
  private readonly HISTORY_KEY = 'music_playback_history_v2';
  private readonly HISTORY_INDEX_KEY = 'music_playback_history_index_v2';
  private readonly HISTORY_LIMIT = 10;

  // Track Registry (ID -> TrackEntry) to avoid duplication in storage
  private trackRegistry = new Map<number, TrackEntry>();

  // History of states (using IDs)
  private history = signal<PlaybackState[]>([]);
  private historyIndex = signal<number>(-1);

  // Current State Signal (synchronized with history[historyIndex])
  private state = signal<PlaybackState>({
    playlistIds: [],
    playlistName: '',
    currentIndex: 0,
    isShuffled: true,
    shuffledIds: []
  });

  // Computed Values
  readonly currentTrack = computed(() => {
    const s = this.state();
    const ids = s.isShuffled ? s.shuffledIds : s.playlistIds;
    if (ids.length === 0) return null;
    return this.trackRegistry.get(ids[s.currentIndex]) || null;
  });

  readonly currentPlaylist = computed(() => {
    const s = this.state();
    const ids = s.isShuffled ? s.shuffledIds : s.playlistIds;
    return ids.map(id => this.trackRegistry.get(id)).filter(t => !!t) as TrackEntry[];
  });

  readonly isShuffled = computed(() => this.state().isShuffled);
  readonly playlistName = computed(() => this.state().playlistName);
  readonly hasPrevious = computed(() => this.state().currentIndex > 0);
  readonly hasNext = computed(() => {
      const s = this.state();
      const list = s.isShuffled ? s.shuffledIds : s.playlistIds;
      return s.currentIndex < list.length - 1;
  });
  
  readonly canGoBack = computed(() => this.historyIndex() > 0);
  readonly canGoForward = computed(() => this.historyIndex() < this.history().length - 1);

  constructor() {
    this.loadState();

    // Auto-save everything on change
    effect(() => {
      try {
          // 1. Save Registry
          const registryObj = Object.fromEntries(this.trackRegistry);
          localStorage.setItem(this.TRACKS_KEY, JSON.stringify(registryObj));
          
          // 2. Save History
          localStorage.setItem(this.HISTORY_KEY, JSON.stringify(this.history()));
          
          // 3. Save Index
          localStorage.setItem(this.HISTORY_INDEX_KEY, this.historyIndex().toString());
      } catch (e) {
          console.error('Failed to persist playback state. Storage might be full.', e);
          // If quota exceeded, we prune the registry to only what is in history
          this.pruneRegistry();
      }
    });
  }

  playPlaylist(playlist: TrackEntry[], name: string = 'Playlist') {
    // 1. Register tracks
    playlist.forEach(t => this.trackRegistry.set(t.trackId, t));

    // 2. Prepare new state
    const playlistIds = playlist.map(t => t.trackId);
    const shuffledIds = this.shuffleArray([...playlistIds]);
    
    const newState: PlaybackState = {
      playlistIds,
      playlistName: name,
      currentIndex: 0,
      isShuffled: true,
      shuffledIds
    };

    // 3. Update history: truncate future and append new state
    const currentHist = this.history();
    const truncatedHist = currentHist.slice(0, this.historyIndex() + 1);
    const newHist = [...truncatedHist, newState];
    
    if (newHist.length > this.HISTORY_LIMIT) {
        newHist.shift();
    }

    this.history.set(newHist);
    this.historyIndex.set(newHist.length - 1);
    this.state.set(newState);
  }

  nextTrack() {
    if (this.hasNext()) {
      this.updateCurrentState(s => ({ ...s, currentIndex: s.currentIndex + 1 }));
    }
  }

  previousTrack() {
    if (this.hasPrevious()) {
      this.updateCurrentState(s => ({ ...s, currentIndex: s.currentIndex - 1 }));
    }
  }

  jumpToTrack(index: number) {
    const list = this.state().isShuffled ? this.state().shuffledIds : this.state().playlistIds;
    if (index >= 0 && index < list.length) {
      this.updateCurrentState(s => ({ ...s, currentIndex: index }));
    }
  }

  toggleShuffle() {
    this.updateCurrentState(s => {
      const newShuffleState = !s.isShuffled;
      let newIndex = s.currentIndex;
      
      if (s.playlistIds.length > 0) {
          const currentId = (s.isShuffled ? s.shuffledIds : s.playlistIds)[s.currentIndex];
          const targetIds = newShuffleState ? s.shuffledIds : s.playlistIds;
          const foundIndex = targetIds.indexOf(currentId);
          if (foundIndex !== -1) newIndex = foundIndex;
      }

      return { ...s, isShuffled: newShuffleState, currentIndex: newIndex };
    });
  }

  // --- History Navigation ---

  goBackHistory() {
    if (this.canGoBack()) {
      const newIdx = this.historyIndex() - 1;
      this.historyIndex.set(newIdx);
      this.state.set(this.history()[newIdx]);
    }
  }

  goForwardHistory() {
      if (this.canGoForward()) {
          const newIdx = this.historyIndex() + 1;
          this.historyIndex.set(newIdx);
          this.state.set(this.history()[newIdx]);
      }
  }

  clearHistory() {
      const currentState = { ...this.state() };
      this.history.set([currentState]);
      this.historyIndex.set(0);
      this.pruneRegistry();
  }

  private pruneRegistry() {
      const currentIds = new Set<number>();
      this.history().forEach(state => {
          state.playlistIds.forEach(id => currentIds.add(id));
      });
      
      for (const id of this.trackRegistry.keys()) {
          if (!currentIds.has(id)) {
              this.trackRegistry.delete(id);
          }
      }
  }

  private updateCurrentState(updater: (s: PlaybackState) => PlaybackState) {
      const newState = updater(this.state());
      this.state.set(newState);
      
      const idx = this.historyIndex();
      if (idx >= 0) {
          this.history.update(h => {
              const newH = [...h];
              newH[idx] = newState;
              return newH;
          });
      }
  }

  private loadState() {
    try {
      // Load registry
      const storedRegistry = localStorage.getItem(this.TRACKS_KEY);
      if (storedRegistry) {
          const registryObj = JSON.parse(storedRegistry);
          this.trackRegistry = new Map(Object.entries(registryObj).map(([k, v]) => [Number(k), v as TrackEntry]));
      }

      // Load History
      const storedHist = localStorage.getItem(this.HISTORY_KEY);
      const storedIdx = localStorage.getItem(this.HISTORY_INDEX_KEY);
      
      if (storedHist && storedIdx) {
          const hist = JSON.parse(storedHist);
          const idx = parseInt(storedIdx, 10);
          this.history.set(hist);
          this.historyIndex.set(idx);
          
          if (idx >= 0 && idx < hist.length) {
              this.state.set(hist[idx]);
          }
      }
    } catch (e) {
      console.warn('Failed to load playback state', e);
    }
  }

  private shuffleArray<T>(array: T[]): T[] {
    for (let i = array.length - 1; i > 0; i--) {
      const j = Math.floor(Math.random() * (i + 1));
      [array[i], array[j]] = [array[j], array[i]];
    }
    return array;
  }
}
