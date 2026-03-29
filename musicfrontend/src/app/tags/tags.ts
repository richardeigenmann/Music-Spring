import { Component, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { ApiService, Tag } from '../apiservice';

@Component({
  selector: 'app-tags',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './tags.html',
  styleUrls: ['./tags.css']
})
export class Tags {
  private apiService = inject(ApiService);
  private router = inject(Router);

  allTags = signal<Tag[]>([]);

  groupedTags = computed(() => {
    const tags = this.allTags();
    const grouped: Record<string, Tag[]> = {};

    console.log('Mapping tags, count:', tags.length);
    if (tags.length > 0) {
        console.log('Sample tag:', tags[0]);
    }

    for (const tag of tags) {
      const type = tag.tagTypeName || 'Other';
      if (!grouped[type]) {
        grouped[type] = [];
      }
      grouped[type].push(tag);
    }

    // Sort tags within each type
    for (const type in grouped) {
      grouped[type].sort((a, b) => a.tagName.localeCompare(b.tagName));
    }

    return grouped;
  });

  sortedTagTypes = computed(() => {
    const keys = Object.keys(this.groupedTags());
    const allTags = this.allTags();

    const priority = (typeName: string) => {
        const lower = typeName.toLowerCase().trim();
        
        // Find the edit type for this category
        const sampleTag = allTags.find(t => t.tagTypeName === typeName);
        const editType = sampleTag?.tagTypeEdit || 'T';

        if (lower === 'playlist') return 0;
        if (lower.includes('playlist')) return 1;
        
        // Selection types ('S') come before Text types ('T')
        if (editType === 'S') return 10;
        
        return 100;
    };

    const sorted = [...keys].sort((a, b) => {
      const pA = priority(a);
      const pB = priority(b);

      if (pA !== pB) return pA - pB;
      return a.localeCompare(b);
    });

    console.log('Final Sorted Tag Types:', sorted);
    return sorted;
  });

  constructor() {
    this.apiService.getTags().subscribe(tags => {
      this.allTags.set(tags);
    });
  }

  selectTag(tagId: number): void {
    this.router.navigate(['/tag', tagId]);
  }
}
