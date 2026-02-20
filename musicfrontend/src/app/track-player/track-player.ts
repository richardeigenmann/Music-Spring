import { CommonModule } from '@angular/common';
import { AfterViewInit, Component, ElementRef, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { TrackEntry } from '../apiservice';

/**
 * Component for playing a playlist of tracks.
 */
@Component({
  selector: 'app-track-player',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './track-player.html',
  styleUrls: ['./track-player.css']
})
export class TrackPlayer implements OnInit, OnDestroy, AfterViewInit {
  playlist: TrackEntry[] = [];
  shuffledPlaylist: TrackEntry[] = [];
  currentTrack: TrackEntry | undefined;
  currentIndex = 0;
  isShuffled = false;

  @ViewChild('audioPlayer') audioPlayer!: ElementRef<HTMLAudioElement>;

  constructor(private router: Router, private route: ActivatedRoute) {
    const navigation = this.router.getCurrentNavigation();
    if (navigation?.extras?.state?.['playlist']) {
      this.playlist = navigation.extras.state['playlist'];
    }
  }

  ngOnInit(): void {
    if (this.playlist.length > 0) {
      this.startPlayback();
    }
  }

  ngAfterViewInit(): void {
    this.audioPlayer.nativeElement.addEventListener('ended', this.playNextTrack.bind(this));
  }

  ngOnDestroy(): void {
    this.audioPlayer.nativeElement.removeEventListener('ended', this.playNextTrack.bind(this));
    this.audioPlayer.nativeElement.pause();
  }

  startPlayback(): void {
    if (this.isShuffled) {
      this.shuffledPlaylist = this.shuffleArray([...this.playlist]);
      this.currentTrack = this.shuffledPlaylist[this.currentIndex];
    } else {
      this.currentTrack = this.playlist[this.currentIndex];
    }
    if (this.currentTrack) {
      this.audioPlayer.nativeElement.src = this.getTrackUrl(this.currentTrack.fileId);
      this.audioPlayer.nativeElement.load();
      this.audioPlayer.nativeElement.play();
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

  getTrackUrl(fileId: number): string {
    return `/api/trackFile/${fileId}`;
  }

  getTrackImageUrl(trackId: number): string {
    return `/api/music/db/track/${trackId}/image`;
  }

  /**
   * Shuffles an array using the Fisher-Yates algorithm.
   * @param array The array to shuffle.
   * @returns The shuffled array.
   */
  private shuffleArray<T>(array: T[]): T[] {
    for (let i = array.length - 1; i > 0; i--) {
      const j = Math.floor(Math.random() * (i + 1));
      [array[i], array[j]] = [array[j], array[i]];
    }
    return array;
  }
}

