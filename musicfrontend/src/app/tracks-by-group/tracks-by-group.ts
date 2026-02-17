import { Component, effect, inject, input } from '@angular/core';
import { ApiService } from '../apiservice';

@Component({
  selector: 'app-tracks-by-group',
  standalone: true,
  imports: [],
  templateUrl: './tracks-by-group.html',
  styleUrls: ['./tracks-by-group.css']
})
export class TracksByGroup {
  groupId = input<string>();
  private apiService = inject(ApiService);

  playlistEntries = this.apiService.playlistEntries;

  constructor() {
    effect(() => {
      const id = this.groupId();
      console.log("Handling effect: id is " + id )
      if (id) {
        this.apiService.loadPlaylistEntries(Number(id));
      }
    });
  }
}
