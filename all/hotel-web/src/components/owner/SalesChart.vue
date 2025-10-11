<template>
  <div class="sales-chart-container">
    <Line :data="chartData" :options="chartOptions" />
  </div>
</template>

<script setup>
import { computed } from 'vue';
import { Line } from 'vue-chartjs';
import { Chart as ChartJS, Title, Tooltip, Legend, LineElement, CategoryScale, LinearScale, PointElement } from 'chart.js';

ChartJS.register(Title, Tooltip, Legend, LineElement, CategoryScale, LinearScale, PointElement);

// props를 백엔드 데이터 구조에 맞게 수정합니다.
const props = defineProps({
  graphData: {
    type: Object,
    required: true,
    default: () => ({ labels: [], data: [] })
  },
});

// computed 속성에서 props의 graphData를 직접 사용합니다.
const chartData = computed(() => ({
  labels: props.graphData.labels,
  datasets: [
    {
      label: '매출',
      backgroundColor: 'rgba(59, 130, 246, 0.2)', // 영역 색상
      borderColor: '#3b82f6', // 라인 색상
      data: props.graphData.data,
      tension: 0.1,
      fill: true, // 라인 아래 영역 채우기
    }
  ]
}));

const chartOptions = computed(() => ({
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: {
      display: false,
    },
    title: {
      display: true,
      text: '매출 분석 그래프',
      font: {
        size: 18,
      }
    },
  },
  scales: {
    y: {
      ticks: {
        callback: function(value) {
          return value.toLocaleString() + '원';
        }
      }
    }
  }
}));
</script>

<style scoped>
.sales-chart-container {
  position: relative;
  height: 400px;
  background-color: #fff;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}
</style>