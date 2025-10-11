<template>
  <div class="owner-dashboard">
    <h2>호텔 대시보드</h2>

    <div class="summary-cards">
      <div class="card">
        <h4>오늘 매출</h4>
        <p>{{ formatCurrency(summary.todaySales) }}</p>
      </div>
      <div class="card">
        <h4>이번 주 매출</h4>
        <p>{{ formatCurrency(summary.weeklySales) }}</p>
      </div>
      <div class="card">
        <h4>이번 달 매출</h4>
        <p>{{ formatCurrency(summary.monthlySales) }}</p>
      </div>
    </div>

    <div class="chart-section">
      <h3>매출 분석</h3>
      <div class="filters">
        <div class="filter-group">
          <label for="room-type">객실 타입:</label>
          <select id="room-type" v-model="filters.roomType" @change="fetchGraphData">
            <option value="">전체</option>
            <option v-for="rt in roomTypes" :key="rt" :value="rt">{{ rt }}</option>
          </select>
        </div>
        <div class="filter-group period-buttons">
          <button @click="setPeriod('daily', '7d')" :class="{ active: filters.period === '7d' }">최근 7일</button>
          <button @click="setPeriod('daily', '30d')" :class="{ active: filters.period === '30d' }">최근 30일</button>
          <button @click="setPeriod('monthly', '1y')" :class="{ active: filters.period === '1y' }">최근 1년</button>
        </div>
        <div class="filter-group date-range">
           <label>기간 설정:</label>
           <input type="date" v-model="filters.startDate" @change="fetchGraphDataWithDateRange"/>
           <span>~</span>
           <input type="date" v-model="filters.endDate" @change="fetchGraphDataWithDateRange"/>
        </div>
      </div>
      
      <SalesChart :graph-data="graphData" />
    </div>
  </div>
</template>

<script>
import SalesChart from './SalesChart.vue';
import axios from 'axios';

export default {
  name: 'OwnerDashboard',
  components: {
    SalesChart
  },
  data() {
    return {
      api: null,
      hotelId: null,
      summary: {
        todaySales: 0,
        weeklySales: 0,
        monthlySales: 0,
        todayCheckIns: 0,
        todayCheckOuts: 0,
      },
      roomTypes: [],
      filters: {
        roomType: '',
        analysisType: 'daily',
        period: '7d',
        startDate: '',
        endDate: '',
      },
      rawGraphData: [],
    };
  },
  computed: {
    graphData() {
      return {
        labels: this.rawGraphData.map(d => d.label),
        data: this.rawGraphData.map(d => d.value)
      };
    }
  },
  methods: {
    // --- API 호출 함수들 ---
    getSummaryCards(hotelId) {
        const token = localStorage.getItem('token');
        return this.api.get(`/owner/dashboard/summary-cards/${hotelId}`, {
            headers: { 'Authorization': `Bearer ${token}` }
        });
    },
    getSalesGraphData(hotelId, params) {
        const token = localStorage.getItem('token');
        return this.api.get(`/owner/dashboard/sales-graph/${hotelId}`, {
            params,
            headers: { 'Authorization': `Bearer ${token}` }
        });
    },
    getRoomTypes(hotelId) {
        const token = localStorage.getItem('token');
        return Promise.resolve({
            data: {
                data: ['Standard', 'Deluxe', 'Suite']
            }
        });
    },

    // --- 데이터 처리 및 이벤트 핸들러 ---
    formatCurrency(value) {
      if (typeof value !== 'number') return '0원';
      return `${value.toLocaleString()}원`;
    },
    async fetchSummary() {
      try {
        const response = await this.getSummaryCards(this.hotelId);
        if (response.data.success) {
          this.summary = response.data.data;
        }
      } catch (error) {
        console.error('요약 데이터 로딩 실패:', error);
      }
    },
    async fetchGraphData() {
      try {
        const useDateRange = this.filters.startDate && this.filters.endDate;
        const params = {
          analysisType: this.filters.analysisType,
          period: useDateRange ? null : this.filters.period,
          roomType: this.filters.roomType || null,
          startDate: this.filters.startDate || null,
          endDate: this.filters.endDate || null,
        };
        const response = await this.getSalesGraphData(this.hotelId, params);
        if (response.data.success) {
          this.rawGraphData = response.data.data.dataPoints;
        }
      } catch (error) {
        console.error('그래프 데이터 로딩 실패:', error);
      }
    },
    setPeriod(analysisType, period) {
      this.filters.analysisType = analysisType;
      this.filters.period = period;
      this.filters.startDate = '';
      this.filters.endDate = '';
      this.fetchGraphData();
    },
    fetchGraphDataWithDateRange() {
        this.filters.period = ''; 
        this.fetchGraphData();
    },
    async fetchRoomTypes() {
        try {
            const response = await this.getRoomTypes(this.hotelId);
            if (response.data.success) {
                this.roomTypes = response.data.data;
            }
        } catch (error) {
            console.error('객실 타입 로딩 실패:', error);
        }
    }
  },
  created() {
    // http.js의 localAxios 함수 로직을 여기에 직접 구현합니다.
    const baseURL = import.meta.env.VITE_VUE_API_URL;
    this.api = axios.create({
      baseURL: baseURL,
      headers: {
        "Content-Type": "application/json;charset=utf-8",
      },
    });

    this.hotelId = this.$route.params.hotelId || 1;
  },
  mounted() {
    this.fetchSummary();
    this.fetchGraphData();
    this.fetchRoomTypes();
  }
};
</script>

<style scoped>
/* style 부분은 변경 없이 그대로 사용합니다. */
.owner-dashboard {
  padding: 2rem;
  font-family: 'Helvetica Neue', Arial, sans-serif;
  background-color: #f4f6f9;
}

h2, h3 {
  color: #333;
}

.summary-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 1.5rem;
  margin-bottom: 2rem;
}

.card {
  background-color: white;
  padding: 1.5rem;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.05);
  text-align: center;
}

.card h4 {
  margin: 0 0 0.5rem 0;
  color: #666;
  font-size: 1rem;
}

.card p {
  margin: 0;
  color: #333;
  font-size: 1.75rem;
  font-weight: bold;
}

.chart-section {
  background-color: #ffffff;
  padding: 2rem;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.05);
}

.filters {
  display: flex;
  flex-wrap: wrap;
  gap: 1.5rem;
  margin-bottom: 1.5rem;
  align-items: center;
}

.filter-group {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.filter-group label {
  font-weight: bold;
  color: #555;
}

.filter-group select,
.filter-group input[type="date"] {
  padding: 0.5rem;
  border-radius: 5px;
  border: 1px solid #ddd;
}

.period-buttons button {
  padding: 0.5rem 1rem;
  border: 1px solid #ddd;
  background-color: #fff;
  cursor: pointer;
  border-radius: 5px;
  transition: background-color 0.2s, color 0.2s;
}

.period-buttons button.active {
  background-color: #007bff;
  color: white;
  border-color: #007bff;
}

.period-buttons button:hover:not(.active) {
  background-color: #f0f0f0;
}
</style>