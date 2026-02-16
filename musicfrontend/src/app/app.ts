import { Component, signal } from '@angular/core';
import { Router, RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  protected readonly title = signal('musicfrontend');

  constructor(private router: Router) {}

  goHome(): void {
    this.router.navigate(['/playlists']);
  }
}
