import { Component, inject, signal, OnInit } from '@angular/core';
import { ApiService } from '../apiservice';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-status',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './status.html',
  styleUrl: './status.css'
})
export class Status implements OnInit {
  private apiService = inject(ApiService);
  
  frontendUrl = signal<string>('');
  backendUrl = signal<string>('');
  containerName = signal<string>('');
  versionInfo = signal<any>(null);

  ngOnInit() {
    this.frontendUrl.set(this.apiService.getFrontendUrl());
    
    // We might need to wait for ApiService to be initialized for backendUrl and containerName
    this.apiService.getVersion().subscribe(info => {
      this.versionInfo.set(info);
      // Once version is fetched, we know ApiService is initialized
      this.backendUrl.set(this.apiService.getApiUrl());
      this.containerName.set(this.apiService.getContainerName());
    });
  }

  goBack() {
    window.history.back();
  }
}
