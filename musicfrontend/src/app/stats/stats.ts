import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ApiService } from '../apiservice';
import { BaseChartDirective } from 'ng2-charts';
import { ChartConfiguration, ChartData, ChartEvent, ChartType } from 'chart.js';
import { Router } from '@angular/router';

interface TagTypeChart {
  typeName: string;
  data: ChartData<'pie', number[], string | string[]>;
  tagIds: number[];
}

@Component({
  selector: 'app-stats',
  standalone: true,
  imports: [CommonModule, BaseChartDirective],
  templateUrl: './stats.html',
  styleUrl: './stats.css'
})
export class Stats implements OnInit {
  private apiService = inject(ApiService);
  private router = inject(Router);

  // Common Chart Configuration
  public pieChartOptions: ChartConfiguration['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        display: true,
        position: 'top',
        labels: {
          color: 'yellow',
          font: {
            size: 11
          }
        }
      },
      tooltip: {
        enabled: true
      }
    }
  };

  public tagTypeCharts = signal<TagTypeChart[]>([]);
  public pieChartType: ChartType = 'pie';

  private chartColors = [
    'rgba(255, 99, 132, 0.8)',
    'rgba(54, 162, 235, 0.8)',
    'rgba(255, 206, 86, 0.8)',
    'rgba(75, 192, 192, 0.8)',
    'rgba(153, 102, 255, 0.8)',
    'rgba(255, 159, 64, 0.8)',
    'rgba(199, 199, 199, 0.8)',
    'rgba(83, 102, 255, 0.8)',
    'rgba(40, 159, 64, 0.8)',
    'rgba(210, 199, 199, 0.8)',
    'rgba(120, 100, 200, 0.8)',
    'rgba(80, 200, 120, 0.8)',
  ];

  ngOnInit(): void {
    // Load detailed charts for each tag type using the single consolidated API
    this.apiService.getTagUsageStats().subscribe(stats => {
      const grouped: Record<string, { labels: string[], data: number[], tagIds: number[] }> = {};

      stats.forEach(s => {
        if (s.count > 0) {
          if (!grouped[s.typeName]) {
            grouped[s.typeName] = { labels: [], data: [], tagIds: [] };
          }
          grouped[s.typeName].labels.push(s.tagName);
          grouped[s.typeName].data.push(s.count);
          grouped[s.typeName].tagIds.push(s.tagId);
        }
      });

      const charts: TagTypeChart[] = Object.keys(grouped).map(typeName => ({
        typeName,
        data: {
          labels: grouped[typeName].labels,
          datasets: [{
            data: grouped[typeName].data,
            backgroundColor: this.chartColors,
            borderColor: 'yellow',
            borderWidth: 1
          }]
        },
        tagIds: grouped[typeName].tagIds
      }));

      this.tagTypeCharts.set(charts);
    });
  }

  public chartClicked({ event, active }: { event?: ChartEvent, active?: object[] }): void {
    if (active && active.length > 0 && event && event.native) {
      const clickedElement = active[0] as { index: number };
      const chartIndex = clickedElement.index;

      const canvas = event.native.target as HTMLElement;
      const chartContainer = canvas.closest('.chart-section');
      if (chartContainer) {
          const chartNumber = (chartContainer as HTMLElement).dataset['chart'];
          if (chartNumber) {
            const chart = this.tagTypeCharts()[parseInt(chartNumber, 10)];
            const tagId = chart.tagIds[chartIndex];
            this.router.navigate(['/tag', tagId]);
          }
      }
    }
  }
}
