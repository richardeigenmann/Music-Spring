import { Component, inject, signal, OnInit, ViewEncapsulation, VERSION as ANGULAR_VERSION } from '@angular/core';
import { ApiService, BackendVersionInfo } from '../apiservice';
import { CommonModule } from '@angular/common';
import { VERSION as PROJECT_VERSION, BUILD_DATE } from '../version';

@Component({
  selector: 'app-status',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './status.html',
  styleUrl: './status.css',
  encapsulation: ViewEncapsulation.None
})
export class Status implements OnInit {
  private apiService = inject(ApiService);

  frontendUrl = signal<string>('');
  backendUrl = signal<string>('');
  actuatorUrl = signal<string>('');
  healthUrl = signal<string>('');
  infoUrl = signal<string>('');
  versionInfo = signal<BackendVersionInfo | null>(null);
  angularVersion = ANGULAR_VERSION.full;
  projectVersion = PROJECT_VERSION;
  buildDate = BUILD_DATE;

  ngOnInit() {
    this.frontendUrl.set(this.apiService.getFrontendUrl());

    // Immediately set URLs from current config (might be defaults)
    this.updateUrls();

    this.apiService.getVersion().subscribe({
      next: (info) => {
        this.versionInfo.set(info);
        // Refresh URLs in case config loading finished
        this.updateUrls();
      },
      error: () => {
        console.warn('Backend version check failed, but updating URLs anyway.');
        this.updateUrls();
      }
    });
  }

  private updateUrls() {
    this.backendUrl.set(this.apiService.getApiUrl());
    this.actuatorUrl.set(this.apiService.getActuatorUrl());
    this.healthUrl.set(this.apiService.getHealthUrl());
    this.infoUrl.set(this.apiService.getInfoUrl());
  }

  goBack() {
    window.history.back();
  }
}
