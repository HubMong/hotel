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

    <div class="summary-cards mb-16">
      <div class="card users">
        <div class="card-icon"></div>
        <div class="card-content">
          <p class="card-number">{{ formatNumber(dashboardData.totalUsers) }}</p>
          <h3>총 사용자</h3>
        </div>
      </div>
      <div class="card businesses">
        <div class="card-icon"></div>
        <div class="card-content">
          <p class="card-number">{{ formatNumber(dashboardData.totalBusinesses) }}</p>
          <h3>총 사업자</h3>
        </div>
      </div>
      <div class="card reservations">
        <div class="card-icon"></div>
        <div class="card-content">
          <p class="card-number">{{ formatNumber(dashboardData.totalReservations) }}</p>
          <h3>총 예약</h3>
        </div>
      </div>
      <div class="card revenue">
        <div class="card-icon"></div>
        <div class="card-content">
          <p class="card-number">{{ formatCurrency(dashboardData.totalRevenue) }}</p>
          <h3>총 매출</h3>
        </div>
      </div>
      <div class="card reviews">
        <div class="card-icon"></div>
        <div class="card-content">
          <p class="card-number">{{ formatNumber(dashboardData.totalReviews) }}</p>
          <h3>총 리뷰</h3>
        </div>
      </div>
      <div class="card coupons">
        <div class="card-icon"></div>
        <div class="card-content">
          <p class="card-number">{{ formatNumber(dashboardData.totalCoupons) }}</p>
          <h3>총 쿠폰</h3>
        </div>
      </div>
      <!-- <div class="card inquires">
        <div class="card-icon"></div>
        <div class="card-content">
          <p class="card-number">{{ formatNumber(dashboardData.totalInquires) }}</p>
          <h3>총 문의</h3>
        </div>
      </div> -->
    </div>

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
  <h3>상위 호텔 Top {{ detailParams.top }} (연간 매출)</h3>
      </div>
      <div class="top-hotels-list">
        <div v-for="(hotel, index) in dashboardData.topHotels" :key="hotel.hotelId" class="hotel-item" :class="`rank-${index + 1}`">
          <div class="hotel-rank"><span class="rank-number">{{ index + 1 }}</span></div>
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
              <span class="label">연간 매출</span>
              <span class="value">{{ formatCurrency(hotel.revenue) }}</span>
            </div>
          </div>
        </div>
        <div v-if="dashboardData.topHotels.length === 0" class="no-data">
          상위 호텔 데이터가 없습니다. 
          <br><small>호텔이 승인되고 예약/결제 데이터가 생성되면 표시됩니다.</small>
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
    Bar
  },
  setup() {
    // 반응형 데이터
  const loading = ref(false)
    const lastUpdated = ref('')
    const dashboardData = reactive({
      totalUsers: 0,
      totalBusinesses: 0,
      totalReservations: 0,
      totalRevenue: 0,
      totalReviews: 0,
      totalCoupons: 0,
      totalInquires: 0,
      dailyRevenue: [],
      monthlySignups: [],
      topHotels: [],
      dailySignups: []
    })

    const detailParams = reactive({
      days: 14,
      top: 5,
      year: new Date().getFullYear()
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
      } catch (e) {
      }
    }
    const saveOptions = () => {
      try {
        localStorage.setItem('dashboardChartOptions', JSON.stringify(chartOptions))
      } catch (e) {
      }
    }

    // 차트 옵션
    const revenueChartOptions = {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: {
          display: false
        },
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
        x: {
          display: true,
          title: {
            display: true,
            text: '날짜'
          }
        },
        y: {
          display: true,
          title: {
            display: true,
            text: '매출 (원)'
          },
          ticks: {
            callback: function(value) {
              return formatCurrency(value)
            }
          }
        }
      },
      elements: {
        line: {
          tension: 0.4
        }
      }
    }

    const signupChartOptions = {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: {
          position: 'top'
        },
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
        x: {
          display: true,
          title: {
            display: true,
            text: '월'
          }
        },
        y: {
          display: true,
          title: {
            display: true,
            text: '가입자 수'
          },
          beginAtZero: true,
          ticks: {
            precision: 0,
            callback: function(value) {
              return formatNumber(Math.round(value))
            }
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
        dashboardData.pendingInquiries = summary.pendingInquiries || 0

        // 상세 데이터 매핑
        dashboardData.dailyRevenue = Array.isArray(detail.dailyRevenue) ? detail.dailyRevenue : []
        dashboardData.dailySignups = Array.isArray(detail.dailySignups) ? detail.dailySignups : []
        dashboardData.monthlySignups = Array.isArray(detail.monthlySignups) ? detail.monthlySignups : []
        dashboardData.topHotels = Array.isArray(detail.topHotels) ? detail.topHotels : []
        
        // 상위호텔 데이터 검증
        if (!Array.isArray(detail.topHotels)) {
          console.warn('Top hotels data is not an array:', detail.topHotels)
        }

        // 매출 차트 데이터 설정
        const daily = dashboardData.dailyRevenue
        revenueChartData.value = {
          labels: daily.map(item => 
            new Date(item.date).toLocaleDateString('ko-KR', { 
              month: 'short', 
              day: 'numeric' 
            })
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
      await loadDashboardData()
      await recalcCountsFromUsers()
      // 세션 플래그가 있으면 갱신
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
      
      // 차트 옵션
      revenueChartOptions,
      signupChartOptions,
      
      // 함수들
      refreshData,
      formatNumber,
      formatCurrency,
      showFilterDrawer,
      chartOptions,
      applyDashboardOptions
    }
  }
}
/**/</script>

<style scoped src="@/assets/css/admin/admin-dashboard.css"></style>