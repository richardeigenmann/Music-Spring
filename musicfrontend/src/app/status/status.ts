import { Component, inject, signal, OnInit, ViewEncapsulation, VERSION as ANGULAR_VERSION } from '@angular/core';
import { ApiService, BackendVersionInfo, IntegrityCheckResult } from '../apiservice';
import { CommonModule } from '@angular/common';
import { VERSION as PROJECT_VERSION, BUILD_DATE } from '../version';
import { QRCodeComponent } from 'angularx-qrcode';

@Component({
  selector: 'app-status',
  standalone: true,
  imports: [CommonModule, QRCodeComponent],
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

  integrityResult = signal<IntegrityCheckResult | null>(null);
  checkingIntegrity = signal<boolean>(false);

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

  checkIntegrity() {
    this.checkingIntegrity.set(true);
    this.integrityResult.set(null);
    this.apiService.checkIntegrity().subscribe({
      next: (result) => {
        this.integrityResult.set(result);
        this.checkingIntegrity.set(false);
      },
      error: (err) => {
        console.error('Integrity check failed', err);
        this.checkingIntegrity.set(false);
      }
    });
  }

  deleteBrokenFile(id: number) {
    if (confirm('Are you sure you want to delete this file record? If this is the last file for the track, the track will also be deleted.')) {
      this.apiService.deleteBrokenFile(id).subscribe({
        next: () => {
          // Refresh the integrity check
          this.checkIntegrity();
        },
        error: (err) => {
          console.error('Failed to delete broken file', err);
          alert('Failed to delete record.');
        }
      });
    }
  }

  goBack() {
    window.history.back();
  }
}
