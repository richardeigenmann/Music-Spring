import { Component, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { ApiService, Group } from '../apiservice';

@Component({
  selector: 'app-groups',
  standalone: true,
  imports: [CommonModule, RouterLink],
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
    return Object.keys(this.groupedGroups()).sort();
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
