import { Component, inject } from '@angular/core';
import { ApiService } from '../apiservice';

@Component({
  selector: 'app-playlists',
  imports: [],
  templateUrl: './playlists.html',
  styleUrl: './playlists.css',
})
export class Playlists {
  apiService = inject(ApiService)
}
