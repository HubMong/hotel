<template>
  <div class="admin-dashboard">
    <div class="page-header">
      <h1>관리자 대시보드</h1>
      <p class="page-description">전체 현황을 한눈에 보고 필요한 지표를 설정할 수 있습니다.</p>
      <div class="page-toolbar">
        <span class="last-updated">마지막 업데이트: {{ lastUpdated }}</span>
        <button class="btn" @click="showFilterDrawer = true">설정</button>
        <button class="btn btn-primary" @click="refreshData">새로고침</button>
      </div>
    </div>

    <draggable v-model="widgetList" group="widgets" item-key="key" class="summary-cards mb-16" @end="saveWidgetList" handle=".drag-handle">
      <template #item="{ element }">
        <div :class="['card', element.type]">
          <div class="drag-handle" title="이동" style="position:absolute;top:10px;right:10px;cursor:grab;opacity:0.5;font-size:18px;z-index:2;">
            <svg width="18" height="18" viewBox="0 0 20 20"><circle cx="5" cy="6" r="1.5"/><circle cx="5" cy="10" r="1.5"/><circle cx="5" cy="14" r="1.5"/><circle cx="10" cy="6" r="1.5"/><circle cx="10" cy="10" r="1.5"/><circle cx="10" cy="14" r="1.5"/><circle cx="15" cy="6" r="1.5"/><circle cx="15" cy="10" r="1.5"/><circle cx="15" cy="14" r="1.5"/></svg>
          </div>
          <div class="card-icon">{{ element.icon }}</div>
          <div class="card-content">
            <p class="card-number">{{ element.value() }}</p>
            <h3>{{ element.title }}</h3>
            <div class="card-trend">
              <span :class="['trend-indicator', element.trendClass]">{{ element.trend }}</span>
              <span class="trend-text">지난 달 대비</span>
            </div>
          </div>
        </div>
      </template>
    </draggable>

    <div class="charts-section">
      <div class="chart-container" v-if="chartOptions.showRevenue">
        <div class="chart-header">
          <h3>최근 {{ detailParams.days }}일 매출 추이</h3>
        </div>
        <div class="chart-content">
          <Line
            v-if="revenueChartData"
            :data="revenueChartData"
            :options="revenueChartOptions"
            :height="100"
          />
          <div v-else class="chart-loading">
            차트 로딩 중...
          </div>
        </div>
      </div>

      <div class="chart-container" v-if="chartOptions.showSignups">
        <div class="chart-header">
          <h3>{{ detailParams.year }}년 월별 신규 가입자</h3>
        </div>
        <div class="chart-content">
          <Bar
            v-if="signupChartData"
            :data="signupChartData"
            :options="signupChartOptions"
            :height="100"
          />
          <div v-else class="chart-loading">
            차트 로딩 중...
          </div>
        </div>
      </div>
    </div>

    <div class="top-hotels-section" v-if="chartOptions.showTopHotels">
      <div class="section-header">
        <h3>상위 호텔 Top {{ detailParams.top }} ({{ detailParams.year ? detailParams.year + '년' : '전체 기간' }} 매출)</h3>
        <div class="section-controls">
          <select v-model.number="detailParams.top" @change="refreshData" class="select-input">
            <option value="5">Top 5</option>
            <option value="10">Top 10</option>
            <option value="20">Top 20</option>
          </select>
          <select v-model.number="detailParams.year" @change="refreshData" class="select-input">
            <option :value="currentYear">{{ currentYear }}년</option>
            <option :value="currentYear - 1">{{ currentYear - 1 }}년</option>
            <option :value="currentYear - 2">{{ currentYear - 2 }}년</option>
            <option :value="0">전체 기간</option>
          </select>
        </div>
      </div>
      <div class="top-hotels-list">
        <div v-for="(hotel, index) in dashboardData.topHotels" :key="hotel.hotelId" class="hotel-item" :class="`rank-${index + 1}`">
          <div class="hotel-rank">
            <span class="rank-number">{{ index + 1 }}</span>
            <span class="rank-medal" v-if="index < 3">
              {{ index === 0 ? '🥇' : index === 1 ? '🥈' : '🥉' }}
            </span>
          </div>
          <div class="hotel-info">
            <h4>{{ hotel.hotelName }}</h4>
            <div class="hotel-details">
              <span class="detail-item">
                <i class="icon">🏨</i>
                {{ hotel.roomCount || 0 }}개 객실
              </span>
              <span class="detail-item">
                <i class="icon">📅</i>
                {{ hotel.reservationCount || 0 }}건 예약
              </span>
              <span class="detail-item" v-if="hotel.averageRating > 0">
                <i class="icon">⭐</i>
                {{ hotel.averageRating.toFixed(1) }}점
              </span>
            </div>
          </div>
          <div class="hotel-stats">
            <div class="stat primary">
              <span class="label">{{ detailParams.year ? detailParams.year + '년' : '전체' }} 매출</span>
              <span class="value">{{ formatCurrency(hotel.revenue) }}</span>
            </div>
          </div>
        </div>
        <div v-if="dashboardData.topHotels.length === 0" class="no-data">
          <div class="no-data-icon">🏨</div>
          <h4>상위 호텔 데이터가 없습니다</h4>
          <p>호텔이 승인되고 예약/결제 데이터가 생성되면 표시됩니다.</p>
          <small>다른 연도를 선택해보세요.</small>
        </div>
      </div>
    </div>

    <div class="drawer-overlay" v-if="showFilterDrawer" @click.self="showFilterDrawer = false">
      <div class="drawer" role="dialog" aria-modal="true">
        <div class="drawer-header">
          <strong>대시보드 설정</strong>
          <button class="btn" @click="showFilterDrawer = false">닫기</button>
        </div>
        <div class="drawer-body">
          <label><input type="checkbox" v-model="chartOptions.showRevenue"/> 매출 추이</label>
          <label><input type="checkbox" v-model="chartOptions.showSignups"/> 월별 가입자</label>
          <label><input type="checkbox" v-model="chartOptions.showTopHotels"/> 인기 호텔</label>
        </div>
        <div class="drawer-footer">
          <button class="btn btn-primary" @click="applyDashboardOptions">적용</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, reactive, onMounted, onBeforeUnmount } from 'vue'
import draggable from 'vuedraggable'
import { Line, Bar } from 'vue-chartjs'
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  BarElement,
  Title,
  Tooltip,
  Legend,
  Filler
} from 'chart.js'
import api from '../../api/http'

// Chart.js 등록
ChartJS.register(
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  BarElement,
  Title,
  Tooltip,
  Legend,
  Filler
)

export default {
  name: 'AdminDashboard',
  components: {
    Line,
    Bar,
    draggable
  },
  setup() {
    // 반응형 데이터
    const loading = ref(false)
    // 대시보드 위젯 배열 (순서/크기/타입/값/트렌드 등)
    const widgetList = ref([])

    // 위젯 정의 (초기값)
    const defaultWidgets = [
      {
        key: 'users',
        type: 'users',
        icon: '👥',
        title: '총 사용자',
        value: () => formatNumber(dashboardData.totalUsers),
        trend: '↗ +12%',
        trendClass: 'positive',
      },
      {
        key: 'businesses',
        type: 'businesses',
        icon: '🏢',
        title: '총 사업자',
        value: () => formatNumber(dashboardData.totalBusinesses),
        trend: '↗ +8%',
        trendClass: 'positive',
      },
      {
        key: 'reservations',
        type: 'reservations',
        icon: '📅',
        title: '총 예약',
        value: () => formatNumber(dashboardData.totalReservations),
        trend: '↗ +15%',
        trendClass: 'positive',
      },
      {
        key: 'revenue',
        type: 'revenue',
        icon: '💰',
        title: '총 매출',
        value: () => formatCurrency(dashboardData.totalRevenue),
        trend: '↗ +23%',
        trendClass: 'positive',
      },
      {
        key: 'reviews',
        type: 'reviews',
        icon: '⭐',
        title: '총 리뷰',
        value: () => formatNumber(dashboardData.totalReviews),
        trend: '↗ +18%',
        trendClass: 'positive',
      },
      {
        key: 'coupons',
        type: 'coupons',
        icon: '🎫',
        title: '총 쿠폰',
        value: () => formatNumber(dashboardData.totalCoupons),
        trend: '→ 0%',
        trendClass: 'neutral',
      },
      {
        key: 'Inquiries',            // [ADDED] 총 문의 위젯 (요청된 표기 유지)
        type: 'Inquiries',           // [ADDED]
        icon: '💬',                  // [ADDED]
        title: '총 문의',            // [ADDED]
        value: () => formatNumber(dashboardData.totalInquiries), // [ADDED]
        trend: '→ 0%',               // [ADDED]
        trendClass: 'neutral',       // [ADDED]
      },
    ]

    // 위젯 순서/크기 저장 및 불러오기
    const WIDGET_LIST_KEY = 'dashboardWidgetList'
    const loadWidgetList = () => {
      try {
        const saved = localStorage.getItem(WIDGET_LIST_KEY)
        if (saved) {
          const arr = JSON.parse(saved)
          // 값 동기화
          widgetList.value = arr.map(w => {
            const def = defaultWidgets.find(d => d.key === w.key)
            return def ? { ...def, ...w } : null
          }).filter(Boolean)
        } else {
          widgetList.value = defaultWidgets.map(w => ({ ...w }))
        }
      } catch {
        widgetList.value = defaultWidgets.map(w => ({ ...w }))
      }
    }
    const saveWidgetList = () => {
      localStorage.setItem(WIDGET_LIST_KEY, JSON.stringify(widgetList.value.map(w => ({ key: w.key }))))
    }

    const lastUpdated = ref('')
    const dashboardData = reactive({
      totalUsers: 0,
      totalBusinesses: 0,
      totalReservations: 0,
      totalRevenue: 0,
      totalReviews: 0,
      totalCoupons: 0,
      totalInquiries: 0,     // [ADDED]
      dailyRevenue: [],
      monthlySignups: [],
      topHotels: [],
      dailySignups: []
    })

    const currentYear = new Date().getFullYear()
    const detailParams = reactive({
      days: 14,
      top: 5,
      year: currentYear
    })
    
    const revenueChartData = ref(null)
    const signupChartData = ref(null)

    // Element Plus - 대시보드 설정 드로어 상태 및 옵션
    const showFilterDrawer = ref(false)
    const chartOptions = reactive({
      showRevenue: true,
      showSignups: true,
      showTopHotels: true
    })

    const loadSavedOptions = () => {
      try {
        const saved = localStorage.getItem('dashboardChartOptions')
        if (saved) {
          const parsed = JSON.parse(saved)
          chartOptions.showRevenue = !!parsed.showRevenue
          chartOptions.showSignups = !!parsed.showSignups
          chartOptions.showTopHotels = !!parsed.showTopHotels
        }
      } catch (e) {}
    }
    const saveOptions = () => {
      try {
        localStorage.setItem('dashboardChartOptions', JSON.stringify(chartOptions))
      } catch (e) {}
    }

    // 차트 옵션
    const revenueChartOptions = {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        datalabels: { display: false },
        legend: { display: false },
        tooltip: {
          mode: 'index',
          intersect: false,
          callbacks: {
            label: function(context) {
              return '매출: ' + formatCurrency(context.parsed.y)
            }
          }
        }
      },
      scales: {
        x: { display: true, title: { display: true, text: '날짜' } },
        y: {
          display: true,
          title: { display: true, text: '매출 (원)' },
          ticks: { callback: function(value) { return formatCurrency(value) } }
        }
      },
      elements: { line: { tension: 0.4 } }
    }

    const signupChartOptions = {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        datalabels: { display: false },
        legend: { position: 'top' },
        tooltip: {
          mode: 'index',
          intersect: false,
          callbacks: {
            label: function(context) {
              const label = context.dataset.label || ''
              const value = Math.round(context.parsed.y || 0)
              return `${label}: ${formatNumber(value)}명`
            }
          }
        }
      },
      scales: {
        x: { display: true, title: { display: true, text: '월' } },
        y: {
          display: true,
          title: { display: true, text: '가입자 수' },
          beginAtZero: true,
          ticks: {
            precision: 0,
            callback: function(value) { return formatNumber(Math.round(value)) }
          }
        }
      }
    }

    // 대시보드 데이터 로드
    const loadDashboardData = async () => {
      loading.value = true
      try {
        const [summaryRes, detailRes] = await Promise.all([
          api.get('/admin/dashboard/summary'),
          api.get('/admin/dashboard/details', { params: { days: detailParams.days, top: detailParams.top, year: detailParams.year } })
        ])
        const summaryEnv = summaryRes.data || {}
        const summary = summaryEnv?.data || {}
        const detailEnv = detailRes.data || {}
        const detail = detailEnv?.data || {}

        // 기본 통계 데이터 설정
        dashboardData.totalUsers = summary.totalUsers || 0
        dashboardData.totalBusinesses = summary.totalBusinesses || 0
        dashboardData.totalReservations = summary.totalReservations || 0
        dashboardData.totalRevenue = summary.totalRevenue || summary.recentRevenue || 0
        dashboardData.totalReviews = summary.totalReviews || 0
        dashboardData.totalCoupons = summary.totalCoupons || 0
        dashboardData.totalInquiries = summary.totalInquiries || 0    // [ADDED]

        // 상세 데이터 매핑
        dashboardData.dailyRevenue = Array.isArray(detail.dailyRevenue) ? detail.dailyRevenue : []
        dashboardData.dailySignups = Array.isArray(detail.dailySignups) ? detail.dailySignups : []
        dashboardData.monthlySignups = Array.isArray(detail.monthlySignups) ? detail.monthlySignups : []
        dashboardData.topHotels = Array.isArray(detail.topHotels) ? detail.topHotels : []

        // 매출 차트 데이터 설정
        const daily = dashboardData.dailyRevenue
        revenueChartData.value = {
          labels: daily.map(item =>
            new Date(item.date).toLocaleDateString('ko-KR', { month: 'short', day: 'numeric' })
          ),
          datasets: [
            {
              label: '일별 매출',
              data: daily.map(item => Number(item.value ?? 0)),
              borderColor: 'rgb(75, 192, 192)',
              backgroundColor: 'rgba(75, 192, 192, 0.1)',
              fill: true,
              tension: 0.4
            }
          ]
        }

        // 가입자 차트 데이터 설정
        const monthly = dashboardData.monthlySignups
        signupChartData.value = {
          labels: monthly.map(item => item.month),
          datasets: [
            {
              label: '가입자',
              data: monthly.map(item => Math.round(Number(item.count ?? 0))),
              backgroundColor: 'rgba(54, 162, 235, 0.8)',
              borderColor: 'rgba(54, 162, 235, 1)',
              borderWidth: 1
            }
          ]
        }

        lastUpdated.value = new Date().toLocaleString('ko-KR')

      } catch (error) {
        console.error('Dashboard load error:', error)
        alert(`대시보드 데이터를 불러오는데 실패했습니다. ${error.response?.data?.error || error.message || ''}`)
      } finally {
        loading.value = false
      }
    }

    // USER만 사용자, BUSINESS만 사업자 카운트 재계산
    const recalcCountsFromUsers = async () => {
      try {
        const [usersRes, businessRes] = await Promise.all([
          api.get('/admin/users', { params: { role: 'USER', page: 0, size: 1 } }),
          api.get('/admin/users', { params: { role: 'BUSINESS', page: 0, size: 1 } })
        ])
        const usersPage = usersRes.data?.data
        const businessPage = businessRes.data?.data
        dashboardData.totalUsers = Number(usersPage?.totalElements ?? 0)
        dashboardData.totalBusinesses = Number(businessPage?.totalElements ?? 0)
      } catch (e) {
        // 무시: 서버가 해당 필터를 지원하지 않으면 기존 값을 유지
      }
    }

    // 데이터 새로고침
    const refreshData = async () => {
      await loadDashboardData()
      await recalcCountsFromUsers()
    }

    const applyDashboardOptions = () => {
      saveOptions()
      showFilterDrawer.value = false
    }

    // 유틸리티 함수들
    const formatNumber = (num) => {
      if (!num) return '0'
      return num.toLocaleString('ko-KR')
    }

    const formatCurrency = (amount) => {
      const num = typeof amount === 'number' ? amount : Number(amount || 0)
      if (!num) return '0원'
      return num.toLocaleString('ko-KR') + '원'
    }

    // 컴포넌트 마운트 시 데이터 로드 및 외부 갱신 신호 수신
    let _refreshHandler
    onMounted(async () => {
      loadSavedOptions()
      loadWidgetList()
      await loadDashboardData()
      await recalcCountsFromUsers()
      try {
        if (sessionStorage.getItem('dashboardNeedsRefresh')) {
          await loadDashboardData()
          await recalcCountsFromUsers()
          sessionStorage.removeItem('dashboardNeedsRefresh')
        }
      } catch {}
      _refreshHandler = () => refreshData()
      window.addEventListener('admin:refresh-dashboard', _refreshHandler)
    })

    onBeforeUnmount(() => {
      if (_refreshHandler) window.removeEventListener('admin:refresh-dashboard', _refreshHandler)
    })

    return {
      // 반응형 데이터
      loading,
      lastUpdated,
      detailParams,
      dashboardData,
      revenueChartData,
      signupChartData,
      currentYear,
      // 위젯
      widgetList,
      saveWidgetList,
      // 차트 옵션
      revenueChartOptions,
      signupChartOptions,
      // 함수들
      refreshData,
      formatNumber,
      formatCurrency,
      showFilterDrawer,
      chartOptions,
      applyDashboardOptions,
    }
  }
}
/**/</script>

<style scoped src="@/assets/css/admin/admin-dashboard.css"></style>
