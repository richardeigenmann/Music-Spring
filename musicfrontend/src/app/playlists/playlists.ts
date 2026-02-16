import { Component, inject } from '@angular/core';
import { ApiService } from '../apiservice';
import { RouterLink } from '@angular/router';

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
