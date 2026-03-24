import { Component, computed, effect, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ApiService, Group, Track, TrackEntry } from '../apiservice';
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
  allGroups = signal<Group[]>([]);
  trackId = signal<number | null>(null);

  // For creating new groups
  creatingGroupForType = signal<string | null>(null);
  newGroupName = signal<string>('');

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

    this.apiService.getGroups().subscribe(groups => {
      this.allGroups.set(groups);
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

  trackGroups = computed(() => {
    const track = this.track();
    const allGroups = this.allGroups();
    if (!track || !allGroups.length) {
      return {};
    }

    const assignedGroupNames: { [groupType: string]: string[] } = {};
    for (const key in track) {
      if (key !== 'TrackId' && key !== 'TrackName' && key !== 'Files') {
        const value = track[key];
        if (Array.isArray(value)) {
          assignedGroupNames[key] = value;
        } else if (typeof value === 'string' && value.length > 0) {
          assignedGroupNames[key] = [value];
        }
      }
    }

    const trackGroups: { [groupType: string]: Group[] } = {};
    for (const groupType in assignedGroupNames) {
      const typeGroups: Group[] = [];
      for (const groupName of assignedGroupNames[groupType]) {
        const group = allGroups.find(g => g.groupTypeName === groupType && g.groupName === groupName);
        // Only include if found AND it's an 'S' (Selection) type
        if (group && group.groupTypeEdit === 'S') {
          typeGroups.push(group);
        }
      }
      if (typeGroups.length > 0) {
        trackGroups[groupType] = typeGroups;
      }
    }
    return trackGroups;
  });

  availableGroups = computed(() => {
    const all = this.allGroups();
    const current = this.trackGroups();
    if (!all.length) {
      return {};
    }

    // Only show group types that are marked as 'S' (Selection)
    const allGrouped: { [key: string]: Group[] } = {};
    for (const group of all) {
      if (group.groupTypeEdit !== 'S') {
        continue;
      }
      if (!allGrouped[group.groupTypeName]) {
        allGrouped[group.groupTypeName] = [];
      }
      allGrouped[group.groupTypeName].push(group);
    }

    const available: { [key: string]: Group[] } = {};
    for (const groupType in allGrouped) {
      const allInType = allGrouped[groupType];
      const currentInType = current[groupType] || [];
      available[groupType] = allInType.filter(g => !currentInType.find(c => c.groupId === g.groupId));
    }

    return available;
  });

  addGroup(group: Group) {
    this.track.update(currentTrack => {
      if (!currentTrack) {
        return null;
      }
      const newTrack = {...currentTrack};
      const groupType = group.groupTypeName;
      const groupName = group.groupName;
      const existingValue = newTrack[groupType];

      if (Array.isArray(existingValue)) {
        if (!existingValue.includes(groupName)) {
          newTrack[groupType] = [...existingValue, groupName];
        }
      } else if (typeof existingValue === 'string') {
        if (existingValue !== groupName) {
          newTrack[groupType] = [existingValue, groupName];
        }
      } else {
        newTrack[groupType] = [groupName];
      }
      return newTrack;
    });
  }

  showCreateGroupForm(groupType: string) {
    this.creatingGroupForType.set(groupType);
    this.newGroupName.set('');
  }

  cancelCreateGroup() {
    this.creatingGroupForType.set(null);
    this.newGroupName.set('');
  }

  createGroup() {
    const type = this.creatingGroupForType();
    const name = this.newGroupName();
    if (type && name.trim()) {
      this.apiService.createGroup(type, name.trim()).subscribe({
        next: (newGroup) => {
          this.allGroups.update(groups => [...groups, newGroup]);
          this.addGroup(newGroup);
          this.cancelCreateGroup();
        },
        error: (error) => console.error('Error creating group', error)
      });
    }
  }

  removeGroup(group: Group) {
    this.track.update(currentTrack => {
      if (!currentTrack) {
        return null;
      }
      const newTrack = {...currentTrack};
      const groupType = group.groupTypeName;
      const groupName = group.groupName;
      const existingValue = newTrack[groupType];

      if (Array.isArray(existingValue)) {
        const newValues = existingValue.filter((g: string) => g !== groupName);
        if (newValues.length === 0) {
          delete newTrack[groupType];
        } else if (newValues.length === 1) {
          newTrack[groupType] = newValues[0];
        } else {
          newTrack[groupType] = newValues;
        }
      } else if (existingValue === groupName) {
        delete newTrack[groupType];
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
