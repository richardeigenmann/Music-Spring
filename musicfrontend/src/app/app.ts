import { Component, OnDestroy, inject, signal, computed } from '@angular/core';
import { Router, RouterOutlet, RouterLink } from '@angular/router';
import { ApiService, Group } from './apiservice';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, CommonModule],
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

  // Groups Modal logic
  showGroupsModal = signal(false);
  showHamburgerMenu = signal(false);
  allGroups = signal<Group[]>([]);
  objectKeys = Object.keys;

  groupedGroups = computed(() => {
    const groups = this.allGroups();
    const grouped: { [key: string]: Group[] } = {};
    for (const group of groups) {
      if (!grouped[group.groupTypeName]) {
        grouped[group.groupTypeName] = [];
      }
      grouped[group.groupTypeName].push(group);
    }
    return grouped;
  });

  constructor() {
    this.apiService.getGroups().subscribe(groups => {
      this.allGroups.set(groups);
    });
  }

  ngOnDestroy(): void {
    if (this.pollInterval) {
      clearInterval(this.pollInterval);
    }
  }

  toggleHamburgerMenu(): void {
    this.showHamburgerMenu.update(v => !v);
  }

  closeHamburgerMenu(): void {
    this.showHamburgerMenu.set(false);
  }

  goHome(): void {
    this.router.navigate(['/playlists']);
  }

  goUnclassified(): void {
    this.router.navigate(['/unclassified']);
  }

  toggleGroupsModal(): void {
    this.showGroupsModal.update(v => !v);
  }

  selectGroup(groupId: number): void {
    this.showGroupsModal.set(false);
    this.router.navigate(['/group', groupId]);
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
