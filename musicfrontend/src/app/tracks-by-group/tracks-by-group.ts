import { Component, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ApiService, TrackEntry } from '../apiservice';
import { Signal } from '@angular/core';

@Component({
  selector: 'app-tracks-by-group',
  standalone: true,
  imports: [],
  templateUrl: './tracks-by-group.html',
  styleUrls: ['./tracks-by-group.css']
})
export class TracksByGroup {
  private route = inject(ActivatedRoute);
  private apiService = inject(ApiService);

  playlistEntries: Signal<TrackEntry[]>;

  constructor() {
    const groupId = Number(this.route.snapshot.paramMap.get('id'));
    this.apiService.loadPlaylistEntries(groupId);
    this.playlistEntries = this.apiService.playlistEntries;
  }
}
