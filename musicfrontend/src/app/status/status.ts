import { Component, inject, signal, OnInit, ViewEncapsulation, VERSION as ANGULAR_VERSION } from '@angular/core';
import { ApiService } from '../apiservice';
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
  versionInfo = signal<any>(null);
  angularVersion = ANGULAR_VERSION.full;
  projectVersion = PROJECT_VERSION;
  buildDate = BUILD_DATE;

  ngOnInit() {
    this.frontendUrl.set(this.apiService.getFrontendUrl());

    // We might need to wait for ApiService to be initialized for backendUrl and containerName
    this.apiService.getVersion().subscribe(info => {
      this.versionInfo.set(info);
      // Once version is fetched, we know ApiService is initialized
      this.backendUrl.set(this.apiService.getApiUrl());
      this.actuatorUrl.set(this.apiService.getActuatorUrl());
      this.healthUrl.set(this.apiService.getHealthUrl());
      this.infoUrl.set(this.apiService.getInfoUrl());
    });
  }

  goBack() {
    window.history.back();
  }
}
