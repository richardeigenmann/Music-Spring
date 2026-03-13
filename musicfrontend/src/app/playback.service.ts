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
  private readonly HISTORY_LIMIT = 10;

  // Current State Signals
  private state = signal<PlaybackState>({
    playlist: [],
    playlistName: '',
    currentIndex: 0,
    isShuffled: true,
    shuffledPlaylist: []
  });

  // History Management
  private history = signal<PlaybackState[]>([]);
  private historyIndex = signal<number>(-1);

  // Computed Values
  readonly currentTrack = computed(() => {
    const s = this.state();
    const list = s.isShuffled ? s.shuffledPlaylist : s.playlist;
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

    // Auto-save state on change
    effect(() => {
      localStorage.setItem(this.STORAGE_KEY, JSON.stringify(this.state()));
    });
    
    // Auto-save history on change
    effect(() => {
       localStorage.setItem(this.HISTORY_KEY, JSON.stringify(this.history()));
    });
  }

  playPlaylist(playlist: TrackEntry[], name: string = 'Playlist') {
    // 1. Save current state to history before switching (if not empty)
    if (this.state().playlist.length > 0) {
        this.pushToHistory(this.state());
    }

    // 2. Prepare new state
    const shuffled = this.shuffleArray([...playlist]);
    const newState: PlaybackState = {
      playlist: playlist,
      playlistName: name,
      currentIndex: 0,
      isShuffled: true, // Default to shuffled as requested
      shuffledPlaylist: shuffled
    };

    // 3. Set new state
    this.state.set(newState);
    
    // 4. Reset history pointer to the end (new "future")
    // Actually, simple history implies a linear timeline.
    // If we play a new playlist, we might want to clear "forward" history 
    // or just append this as a new entry.
    // For simplicity, let's treat "Back" as "Go to previous playlist context"
  }

  nextTrack() {
    if (this.hasNext()) {
      this.state.update(s => ({ ...s, currentIndex: s.currentIndex + 1 }));
    }
  }

  previousTrack() {
    if (this.hasPrevious()) {
      this.state.update(s => ({ ...s, currentIndex: s.currentIndex - 1 }));
    }
  }

  jumpToTrack(index: number) {
    if (index >= 0 && index < this.currentPlaylist().length) {
      this.state.update(s => ({ ...s, currentIndex: index }));
    }
  }

  toggleShuffle() {
    this.state.update(s => {
      const newShuffleState = !s.isShuffled;
      let newIndex = s.currentIndex;
      
      // If switching modes, try to keep the current track playing
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
      this.historyIndex.update(i => i - 1);
      this.state.set(this.history()[this.historyIndex()]);
    }
  }

  goForwardHistory() {
      if (this.canGoForward()) {
          this.historyIndex.update(i => i + 1);
          this.state.set(this.history()[this.historyIndex()]);
      }
  }

  private pushToHistory(state: PlaybackState) {
      const currentHist = this.history();
      // If we are in the middle of history, discard the "future"
      const truncatedHist = currentHist.slice(0, this.historyIndex() + 1);
      
      const newHist = [...truncatedHist, JSON.parse(JSON.stringify(state))]; // Deep copy
      if (newHist.length > this.HISTORY_LIMIT) {
          newHist.shift(); // Remove oldest
      }
      
      this.history.set(newHist);
      this.historyIndex.set(newHist.length - 1);
  }

  // --- Internals ---

  private loadState() {
    try {
      const stored = localStorage.getItem(this.STORAGE_KEY);
      if (stored) {
        this.state.set(JSON.parse(stored));
      }
      
      const storedHist = localStorage.getItem(this.HISTORY_KEY);
      if (storedHist) {
          const hist = JSON.parse(storedHist);
          this.history.set(hist);
          // If we loaded a state, try to find where we are in history
          // Or just append the loaded state as the "latest"
          this.historyIndex.set(hist.length - 1);
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
