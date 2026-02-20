import { Component, OnDestroy, inject, signal } from '@angular/core';
import { Router, RouterOutlet } from '@angular/router';
import { ApiService } from './apiservice';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, CommonModule],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App implements OnDestroy {
  protected readonly title = signal('musicfrontend');
  private apiService = inject(ApiService);
  private router = inject(Router);
  totalTracks = this.apiService.totalTrackCount;
  
  scanProgress = signal<{ checked: number, added: number, totalEstimated: number, isDone: boolean, currentFile: string } | null>(null);
  private pollInterval: any;

  constructor() {}

  ngOnDestroy(): void {
    if (this.pollInterval) {
      clearInterval(this.pollInterval);
    }
  }

  goHome(): void {
    this.router.navigate(['/playlists']);
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
    if (this.pollInterval) {
      clearInterval(this.pollInterval);
    }
    this.pollInterval = setInterval(() => {
      this.apiService.getScanProgress().subscribe(progress => {
        this.scanProgress.set(progress);
        if (progress.isDone) {
          clearInterval(this.pollInterval);
          this.pollInterval = null;
          // After scan is done, show unclassified tracks
          this.goUnclassified();
          // Update total track count
          this.apiService.loadTotalTrackCount();
        }
      });
    }, 300);
  }
}
