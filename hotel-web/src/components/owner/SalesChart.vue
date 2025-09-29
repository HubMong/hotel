<template>
  <Line :data="chartData" :options="chartOptions" />
</template>

<script>
import { Line } from 'vue-chartjs'
import { Chart as ChartJS, Title, Tooltip, Legend, LineElement, CategoryScale, LinearScale, PointElement } from 'chart.js'

ChartJS.register(Title, Tooltip, Legend, LineElement, CategoryScale, LinearScale, PointElement)

export default {
  name: 'SalesChart',
  components: { Line },
  props: {
    salesData: {
      type: Array,
      required: true
    }
  },
  computed: {
    chartData() {
      const labels = this.salesData.map(d => d.date);
      const data = this.salesData.map(d => d.totalSales);
      
      return {
        labels: labels,
        datasets: [
          {
            label: '일일 매출',
            backgroundColor: '#3b82f6',
            borderColor: '#3b82f6',
            data: data,
            tension: 0.1
          }
        ]
      }
    },
    chartOptions() {
      return {
        responsive: true,
        maintainAspectRatio: false,
        scales: {
          y: {
            ticks: {
              callback: function(value) {
                return '₩ ' + value.toLocaleString('ko-KR');
              }
            }
          }
        }
      }
    }
  }
}
</script>