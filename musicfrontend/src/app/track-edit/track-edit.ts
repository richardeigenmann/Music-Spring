import { Component, computed, effect, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ApiService, Tag, Track } from '../apiservice';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PlaybackService } from '../playback.service';

/**
 * Component for editing a track.
 */
@Component({
  selector: 'app-track-edit',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './track-edit.html',
  styleUrl: './track-edit.css'
})
export class TrackEdit {
  route = inject(ActivatedRoute);
  apiService = inject(ApiService);
  private playbackService = inject(PlaybackService);

  track = signal<Track | null>(null);
  allTags = signal<Tag[]>([]);
  trackId = signal<number | null>(null);

  // For creating new tags
  creatingTagForType = signal<string | null>(null);
  newTagName = signal<string>('');

  objectKeys = Object.keys;

  constructor() {
    this.route.params.subscribe(params => {
      const id = params['id'];
      if (id) {
        this.trackId.set(+id);
      }
    });

    effect(() => {
      const id = this.trackId();
      if (id) {
        this.apiService.getTrack(id).subscribe(track => {
          this.track.set(track);
        });
      }
    });

    this.apiService.getTags().subscribe(tags => {
      this.allTags.set(tags);
    });
  }

  play() {
    const t = this.track();
    if (!t) return;

    // Convert Track to TrackEntry for the playback service
    const trackEntry = this.apiService.mapToTrackEntry(t);
    const title = `Track: ${trackEntry.artist} - ${trackEntry.title}`;

    this.playbackService.playPlaylist([trackEntry], title);
  }

  goBack() {
      window.history.back();
  }

  trackTags = computed(() => {
    const track = this.track();
    const allTags = this.allTags();
    if (!track || !allTags.length) {
      return {};
    }

    const assignedTagNames: { [tagType: string]: string[] } = {};
    for (const key in track) {
      if (key !== 'trackId' && key !== 'trackName' && key !== 'files') {
        const value = track[key];
        if (Array.isArray(value)) {
          assignedTagNames[key] = value;
        } else if (typeof value === 'string' && value.length > 0) {
          assignedTagNames[key] = [value];
        }
      }
    }

    const trackTags: { [tagType: string]: Tag[] } = {};
    for (const tagType in assignedTagNames) {
      const typeTags: Tag[] = [];
      for (const tagName of assignedTagNames[tagType]) {
        const tag = allTags.find(t => t.tagTypeName === tagType && t.tagName === tagName);
        // Only include if found AND it's an 'S' (Selection) type
        if (tag && tag.tagTypeEdit === 'S') {
          typeTags.push(tag);
        }
      }
      if (typeTags.length > 0) {
        trackTags[tagType] = typeTags;
      }
    }
    return trackTags;
  });

  availableTags = computed(() => {
    const all = this.allTags();
    const current = this.trackTags();
    if (!all.length) {
      return {};
    }

    // Only show tag types that are marked as 'S' (Selection)
    const allTagTypesGrouped: { [key: string]: Tag[] } = {};
    for (const tag of all) {
      if (tag.tagTypeEdit !== 'S') {
        continue;
      }
      if (!allTagTypesGrouped[tag.tagTypeName]) {
        allTagTypesGrouped[tag.tagTypeName] = [];
      }
      allTagTypesGrouped[tag.tagTypeName].push(tag);
    }

    const available: { [key: string]: Tag[] } = {};
    for (const tagType in allTagTypesGrouped) {
      const allInType = allTagTypesGrouped[tagType];
      const currentInType = current[tagType] || [];
      available[tagType] = allInType.filter(t => !currentInType.find(c => c.tagId === t.tagId));
    }

    return available;
  });

  addTag(tag: Tag) {
    this.track.update(currentTrack => {
      if (!currentTrack) {
        return null;
      }
      const newTrack = {...currentTrack};
      const tagType = tag.tagTypeName;
      const tagName = tag.tagName;
      const existingValue = newTrack[tagType];

      if (Array.isArray(existingValue)) {
        if (!existingValue.includes(tagName)) {
          newTrack[tagType] = [...existingValue, tagName];
        }
      } else if (typeof existingValue === 'string') {
        if (existingValue !== tagName) {
          newTrack[tagType] = [existingValue, tagName];
        }
      } else {
        newTrack[tagType] = [tagName];
      }
      return newTrack;
    });
  }

  showCreateTagForm(tagType: string) {
    this.creatingTagForType.set(tagType);
    this.newTagName.set('');
  }

  cancelCreateTag() {
    this.creatingTagForType.set(null);
    this.newTagName.set('');
  }

  createTag() {
    const type = this.creatingTagForType();
    const name = this.newTagName();
    if (type && name.trim()) {
      this.apiService.createTag(type, name.trim()).subscribe({
        next: (newTag) => {
          this.allTags.update(tags => [...tags, newTag]);
          this.addTag(newTag);
          this.cancelCreateTag();
        },
        error: (error) => console.error('Error creating tag', error)
      });
    }
  }

  removeTag(tag: Tag) {
    this.track.update(currentTrack => {
      if (!currentTrack) {
        return null;
      }
      const newTrack = {...currentTrack};
      const tagType = tag.tagTypeName;
      const tagName = tag.tagName;
      const existingValue = newTrack[tagType];

      if (Array.isArray(existingValue)) {
        const newValues = existingValue.filter((t: string) => t !== tagName);
        if (newValues.length === 0) {
          delete newTrack[tagType];
        } else if (newValues.length === 1) {
          newTrack[tagType] = newValues[0];
        } else {
          newTrack[tagType] = newValues;
        }
      } else if (existingValue === tagName) {
        delete newTrack[tagType];
      }
      return newTrack;
    });
  }

  save() {
    const track = this.track();
    if (track) {
      this.apiService.saveTrack(track).subscribe({
        next: (response) => console.log('Track saved successfully', response),
        error: (error) => console.error('Error saving track', error)
      });
    }
  }

  getImageUrl(fileId: number): string {
    return `${this.apiService.getApiUrl()}/api/trackFileImage/${fileId}`;
  }

  getAudioUrl(fileId: number): string {
    return `${this.apiService.getApiUrl()}/api/trackFile/${fileId}`;
  }

  formatDuration(seconds: number): string {
    if (isNaN(seconds) || seconds < 0) {
      return '00:00';
    }
    const minutes = Math.floor(seconds / 60);
    const remainingSeconds = Math.floor(seconds % 60);
    return `${minutes.toString().padStart(2, '0')}:${remainingSeconds.toString().padStart(2, '0')}`;
  }

  // New features for editable arrays
  editableArrayFields = ['Artist', 'Composer', 'Media Name', 'Original Artist'];

  getTrackFieldAsArray(field: string): string[] {
    const track = this.track();
    if (!track) {
      return [];
    }
    const value = track[field];
    if (Array.isArray(value)) {
      return value;
    }
    if (typeof value === 'string') {
      return [value];
    }
    return [];
  }

  addArrayItem(field: string) {
    this.track.update(currentTrack => {
      if (!currentTrack) {
        return null;
      }
      const newTrack = { ...currentTrack };
      const currentValue = newTrack[field];
      if (Array.isArray(currentValue)) {
        newTrack[field] = [...currentValue, ''];
      } else if (typeof currentValue === 'string') {
        newTrack[field] = [currentValue, ''];
      } else {
        newTrack[field] = [''];
      }
      return newTrack;
    });
  }

  removeArrayItem(field: string, index: number) {
    this.track.update(currentTrack => {
      if (!currentTrack) {
        return null;
      }
      const newTrack = { ...currentTrack };
      const currentValue = newTrack[field];
      if (Array.isArray(currentValue)) {
        const newValues = currentValue.filter((_, i) => i !== index);
        if (newValues.length === 0) {
          delete newTrack[field];
        } else if (newValues.length === 1) {
          newTrack[field] = newValues[0];
        } else {
          newTrack[field] = newValues;
        }
      } else if (index === 0) {
        // It was a single string, so just delete it
        delete newTrack[field];
      }
      return newTrack;
    });
  }

  updateArrayItem(field: string, index: number, event: Event) {
    const target = event.target as HTMLInputElement;
    const newValue = target.value;
    this.track.update(currentTrack => {
      if (!currentTrack) {
        return null;
      }
      const newTrack = { ...currentTrack };
      const currentValue = newTrack[field];
      if (Array.isArray(currentValue)) {
        const newValues = [...currentValue];
        newValues[index] = newValue;
        newTrack[field] = newValues;
      } else if (index === 0) {
        newTrack[field] = newValue;
      }
      return newTrack;
    });
  }
}
