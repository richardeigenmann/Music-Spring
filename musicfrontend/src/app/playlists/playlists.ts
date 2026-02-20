import { Component, inject } from '@angular/core';
import { ApiService } from '../apiservice';
import { RouterLink } from '@angular/router';

/**
 * Component for displaying a list of playlists.
 */
@Component({
  selector: 'app-playlists',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './playlists.html',
  styleUrl: './playlists.css',
})
export class Playlists {
  apiService = inject(ApiService);
  playlists = this.apiService.playlists;
}
