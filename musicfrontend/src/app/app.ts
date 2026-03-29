import { Component, OnDestroy, inject, signal, computed } from '@angular/core';
import { Router, RouterOutlet, RouterLink } from '@angular/router';
import { ApiService, ScanProgress, Tag } from './apiservice';
import { CommonModule } from '@angular/common';
import { TrackPlayer } from './track-player/track-player';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, CommonModule, TrackPlayer],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App implements OnDestroy {
  protected readonly title = signal('musicfrontend');
  private apiService = inject(ApiService);
  private router = inject(Router);
  totalTracks = this.apiService.totalTrackCount;

  scanProgress = signal<ScanProgress | null>(null);
  showHamburgerMenu = signal(false);
  private pollInterval: any = null;

  constructor() {}

  ngOnDestroy(): void {
    if (this.pollInterval) {
      clearInterval(this.pollInterval);
    }
  }

  toggleHamburgerMenu(): void {
    this.showHamburgerMenu.update((v) => !v);
  }

  closeHamburgerMenu(): void {
    this.showHamburgerMenu.set(false);
  }

  goHome(): void {
    this.router.navigate(['/tags']);
  }

  goUnclassified(): void {
    this.router.navigate(['/unclassified']);
  }

  performSearch(event: Event): void {
    const input = event.target as HTMLInputElement;
    const q = input.value;
    if (q) {
      this.router.navigate(['/search'], { queryParams: { q } });
      input.value = ''; // Clear input after search
    }
  }

  startScan(): void {
    this.apiService.scanTracks().subscribe(() => {
      this.pollProgress();
    });
  }

  pollProgress(): void {
    console.log('Starting progress polling...');
    this.apiService.getScanProgress().subscribe({
      next: (progress) => {
        console.log('Progress update received:', progress);
        this.scanProgress.set(progress);
        if (progress.isDone) {
          console.log('Scan completion detected in app.ts. Navigating to unclassified...');
          // Update total track count first
          this.apiService.getVersion().subscribe();
          // Navigate
          this.goUnclassified();
        }
      },
      error: (err) => console.error('Error polling progress:', err),
      complete: () => {
        console.log('Progress polling observable completed.');
        // Optional: clear progress after some time or keep it as "done"
      }
    });
  }
}
