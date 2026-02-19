import { Component, computed, effect, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ApiService, Group, Track } from '../apiservice';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

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

  track = signal<Track | null>(null);
  allGroups = signal<Group[]>([]);
  trackId = signal<number | null>(null);

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
      trackGroups[groupType] = [];
      for (const groupName of assignedGroupNames[groupType]) {
        const group = allGroups.find(g => g.groupTypeName === groupType && g.groupName === groupName);
        if (group) {
          trackGroups[groupType].push(group);
        }
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

    const allGrouped: { [key: string]: Group[] } = {};
    for (const group of all) {
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
}
