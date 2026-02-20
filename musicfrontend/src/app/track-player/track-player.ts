import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { TrackEntry } from '../apiservice';

@Component({
  selector: 'app-track-player',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './track-player.html',
  styleUrls: ['./track-player.css']
})
export class TrackPlayer implements OnInit, OnDestroy {
  playlist: TrackEntry[] = [];
  shuffledPlaylist: TrackEntry[] = [];
  currentTrack: TrackEntry | undefined;
  currentIndex = 0;
  isShuffled = false;

  private audio: HTMLAudioElement;

  constructor(private router: Router, private route: ActivatedRoute) {
    const navigation = this.router.getCurrentNavigation();
    if (navigation?.extras?.state?.['playlist']) {
      this.playlist = navigation.extras.state['playlist'];
    }
    this.audio = new Audio();
  }

  ngOnInit(): void {
    if (this.playlist.length > 0) {
      this.startPlayback();
    }
    this.audio.addEventListener('ended', this.playNextTrack.bind(this));
  }

  ngOnDestroy(): void {
    this.audio.removeEventListener('ended', this.playNextTrack.bind(this));
    this.audio.pause();
  }

  startPlayback(): void {
    if (this.isShuffled) {
      this.shuffledPlaylist = this.shuffleArray([...this.playlist]);
      this.currentTrack = this.shuffledPlaylist[this.currentIndex];
    } else {
      this.currentTrack = this.playlist[this.currentIndex];
    }
    if (this.currentTrack) {
      this.audio.src = this.getTrackUrl(this.currentTrack.trackId);
      this.audio.load();
      this.audio.play();
    }
  }

  playNextTrack(): void {
    if (this.currentIndex < this.getPlaylist().length - 1) {
      this.currentIndex++;
      this.startPlayback();
    }
  }

  playPreviousTrack(): void {
    if (this.currentIndex > 0) {
      this.currentIndex--;
      this.startPlayback();
    }
  }

  toggleShuffle(): void {
    this.isShuffled = !this.isShuffled;
    this.currentIndex = 0;
    this.startPlayback();
  }

  getPlaylist(): TrackEntry[] {
    return this.isShuffled ? this.shuffledPlaylist : this.playlist;
  }

  getTrackUrl(trackId: number): string {
    return `/api/music/db/track/${trackId}/stream`;
  }

  getTrackImageUrl(trackId: number): string {
    return `/api/music/db/track/${trackId}/image`;
  }

  private shuffleArray<T>(array: T[]): T[] {
    for (let i = array.length - 1; i > 0; i--) {
      const j = Math.floor(Math.random() * (i + 1));
      [array[i], array[j]] = [array[j], array[i]];
    }
    return array;
  }
}

