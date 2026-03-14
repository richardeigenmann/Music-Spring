import { Injectable, computed, effect, inject, signal } from '@angular/core';
import { TrackEntry } from './apiservice';

export interface PlaybackState {
  playlist: TrackEntry[];
  playlistName: string;
  currentIndex: number;
  isShuffled: boolean;
  shuffledPlaylist: TrackEntry[];
}

@Injectable({
  providedIn: 'root'
})
export class PlaybackService {
  private readonly STORAGE_KEY = 'music_playback_state';
  private readonly HISTORY_KEY = 'music_playback_history';
  private readonly HISTORY_INDEX_KEY = 'music_playback_history_index';
  private readonly HISTORY_LIMIT = 10;

  // History is the main source of truth
  private history = signal<PlaybackState[]>([]);
  private historyIndex = signal<number>(-1);

  // Current State Signal (synchronized with history[historyIndex])
  private state = signal<PlaybackState>({
    playlist: [],
    playlistName: '',
    currentIndex: 0,
    isShuffled: true,
    shuffledPlaylist: []
  });

  // Computed Values
  readonly currentTrack = computed(() => {
    const s = this.state();
    const list = s.isShuffled ? s.shuffledPlaylist : s.playlist;
    if (list.length === 0) return null;
    return list[s.currentIndex];
  });

  readonly currentPlaylist = computed(() => {
    const s = this.state();
    return s.isShuffled ? s.shuffledPlaylist : s.playlist;
  });

  readonly isShuffled = computed(() => this.state().isShuffled);
  readonly playlistName = computed(() => this.state().playlistName);
  readonly hasPrevious = computed(() => this.state().currentIndex > 0);
  readonly hasNext = computed(() => this.state().currentIndex < this.currentPlaylist().length - 1);
  
  readonly canGoBack = computed(() => this.historyIndex() > 0);
  readonly canGoForward = computed(() => this.historyIndex() < this.history().length - 1);

  constructor() {
    this.loadState();

    // Auto-save everything on change
    effect(() => {
      localStorage.setItem(this.STORAGE_KEY, JSON.stringify(this.state()));
      localStorage.setItem(this.HISTORY_KEY, JSON.stringify(this.history()));
      localStorage.setItem(this.HISTORY_INDEX_KEY, this.historyIndex().toString());
    });
  }

  playPlaylist(playlist: TrackEntry[], name: string = 'Playlist') {
    // 1. Prepare new state
    const shuffled = this.shuffleArray([...playlist]);
    const newState: PlaybackState = {
      playlist: playlist,
      playlistName: name,
      currentIndex: 0,
      isShuffled: true, // Default to shuffled
      shuffledPlaylist: shuffled
    };

    // 2. Update history: truncate future and append new state
    const currentHist = this.history();
    const truncatedHist = currentHist.slice(0, this.historyIndex() + 1);
    const newHist = [...truncatedHist, JSON.parse(JSON.stringify(newState))];
    
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
    if (index >= 0 && index < this.currentPlaylist().length) {
      this.updateCurrentState(s => ({ ...s, currentIndex: index }));
    }
  }

  toggleShuffle() {
    this.updateCurrentState(s => {
      const newShuffleState = !s.isShuffled;
      let newIndex = s.currentIndex;
      
      if (s.playlist.length > 0) {
          const currentTrack = (s.isShuffled ? s.shuffledPlaylist : s.playlist)[s.currentIndex];
          const targetList = newShuffleState ? s.shuffledPlaylist : s.playlist;
          const foundIndex = targetList.findIndex(t => t.trackId === currentTrack.trackId);
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
      this.state.set(JSON.parse(JSON.stringify(this.history()[newIdx])));
    }
  }

  goForwardHistory() {
      if (this.canGoForward()) {
          const newIdx = this.historyIndex() + 1;
          this.historyIndex.set(newIdx);
          this.state.set(JSON.parse(JSON.stringify(this.history()[newIdx])));
      }
  }

  // Helper to keep state and history in sync
  private updateCurrentState(updater: (s: PlaybackState) => PlaybackState) {
      const newState = updater(this.state());
      this.state.set(newState);
      
      // Also update history at the current index so it's remembered
      const idx = this.historyIndex();
      if (idx >= 0) {
          this.history.update(h => {
              const newH = [...h];
              newH[idx] = JSON.parse(JSON.stringify(newState));
              return newH;
          });
      }
  }

  // --- Internals ---

  private loadState() {
    try {
      const storedHist = localStorage.getItem(this.HISTORY_KEY);
      const storedIdx = localStorage.getItem(this.HISTORY_INDEX_KEY);
      
      if (storedHist && storedIdx) {
          const hist = JSON.parse(storedHist);
          const idx = parseInt(storedIdx, 10);
          this.history.set(hist);
          this.historyIndex.set(idx);
          
          if (idx >= 0 && idx < hist.length) {
              this.state.set(JSON.parse(JSON.stringify(hist[idx])));
          }
      } else {
          // Fallback to old storage key if exists
          const stored = localStorage.getItem(this.STORAGE_KEY);
          if (stored) {
            const s = JSON.parse(stored);
            this.state.set(s);
            this.history.set([s]);
            this.historyIndex.set(0);
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
