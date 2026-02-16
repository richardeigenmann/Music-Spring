import { Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { Playlists } from "./playlists/playlists";

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, Playlists],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  protected readonly title = signal('musicfrontend');
}
