import { Component, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { ApiService, Group } from '../apiservice';

@Component({
  selector: 'app-groups',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './groups.html',
  styleUrls: ['./groups.css']
})
export class Groups {
  private apiService = inject(ApiService);
  private router = inject(Router);

  allGroups = signal<Group[]>([]);

  groupedGroups = computed(() => {
    const groups = this.allGroups();
    const grouped: { [key: string]: Group[] } = {};

    console.log('Mapping groups, count:', groups.length);
    if (groups.length > 0) {
        console.log('Sample group:', groups[0]);
    }

    for (const group of groups) {
      const type = group.groupTypeName || 'Other';
      if (!grouped[type]) {
        grouped[type] = [];
      }
      grouped[type].push(group);
    }

    // Sort groups within each type
    for (const type in grouped) {
      grouped[type].sort((a, b) => a.groupName.localeCompare(b.groupName));
    }

    return grouped;
  });

  sortedGroupTypes = computed(() => {
    const keys = Object.keys(this.groupedGroups());

    const priority = (s: string) => {
        const lower = (s || '').toLowerCase().trim();
        if (lower === 'playlist') return 0;
        if (lower.includes('playlist')) return 1;
        return 100;
    };

    const sorted = [...keys].sort((a, b) => {
      const pA = priority(a);
      const pB = priority(b);

      if (pA !== pB) return pA - pB;
      return a.localeCompare(b);
    });

    console.log('Final Sorted Group Types:', sorted);
    return sorted;
  });

  constructor() {
    this.apiService.getGroups().subscribe(groups => {
      this.allGroups.set(groups);
    });
  }

  selectGroup(groupId: number): void {
    this.router.navigate(['/group', groupId]);
  }
}
