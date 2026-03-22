import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ApiService } from '../apiservice';
import { BaseChartDirective } from 'ng2-charts';
import { ChartConfiguration, ChartData, ChartType } from 'chart.js';

interface GroupTypeChart {
  typeName: string;
  data: ChartData<'pie', number[], string | string[]>;
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

  public groupTypeCharts = signal<GroupTypeChart[]>([]);
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
    // Load detailed charts for each group type using the single consolidated API
    this.apiService.getGroupUsageStats().subscribe(stats => {
      const grouped: { [key: string]: { labels: string[], data: number[] } } = {};
      
      stats.forEach(s => {
        if (s.count > 0) {
          if (!grouped[s.typeName]) {
            grouped[s.typeName] = { labels: [], data: [] };
          }
          grouped[s.typeName].labels.push(s.groupName);
          grouped[s.typeName].data.push(s.count);
        }
      });

      const charts: GroupTypeChart[] = Object.keys(grouped).map(typeName => ({
        typeName,
        data: {
          labels: grouped[typeName].labels,
          datasets: [{
            data: grouped[typeName].data,
            backgroundColor: this.chartColors,
            borderColor: 'yellow',
            borderWidth: 1
          }]
        }
      }));

      this.groupTypeCharts.set(charts);
    });
  }
}
