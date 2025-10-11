<template>
  <div class="sales-management">
    <div class="page-header">
      <h1>매출·수수료 관리</h1>
      <p class="page-description">지정된 기간의 매출과 수수료를 요약하여 확인합니다.</p>
    </div>

    <!-- 탭 네비게이션 -->
    <div class="tab-navigation">
      <button 
        class="tab-btn" 
        :class="{ active: viewTab === 'settlements' }" 
        @click="viewTab = 'settlements'">
        호텔별 정산
      </button>
      <button 
        class="tab-btn" 
        :class="{ active: viewTab === 'analytics' }" 
        @click="viewTab = 'analytics'">
        매출 분석
      </button>
    </div>

    <!-- 날짜 범위 선택 (호텔별 정산 탭) -->
    <div v-if="viewTab === 'settlements'" class="date-filter">
      <div class="date-inputs">
        <div class="date-group">
          <label>시작일</label>
          <input
            type="date"
            class="date-input"
            v-model="dateRange.from"
            :max="dateRange.to || undefined"
            :disabled="loading"
          />
        </div>
        <div class="date-group">
          <label>종료일</label>
          <input
            type="date"
            class="date-input"
            v-model="dateRange.to"
            :min="dateRange.from || undefined"
            :disabled="loading"
          />
        </div>
        <button class="search-btn" @click="searchSales" :disabled="loading">조회</button>
        <button class="quick-btn" @click="setQuickDate(7)" :disabled="loading">최근 7일</button>
        <button class="quick-btn" @click="setQuickDate(30)" :disabled="loading">최근 30일</button>
        <button class="quick-btn" @click="setQuickDate(90)" :disabled="loading">최근 3개월</button>
        <button class="quick-btn" @click="setQuickDate(365)" :disabled="loading">최근 1년</button>
      </div>
    </div>

    <!-- 매출 요약 카드 (호텔별 정산 탭) -->
    <div v-if="viewTab === 'settlements' && salesData" class="summary-cards">
      <div class="card total-revenue">
        <div class="card-header">
          <h3>총 매출</h3>
        </div>
        <div class="card-content">
          <p class="amount">{{ formatCurrency(salesData.totalRevenue) }}</p>
          <small>{{ dateRange.from }} ~ {{ dateRange.to }}</small>
        </div>
      </div>
      <div class="card platform-fee">
        <div class="card-header">
          <h3>플랫폼 수익</h3>
        </div>
        <div class="card-content">
          <p class="amount">{{ formatCurrency(salesData.platformFeeAmount) }}</p>
          <small>수수료율 10%</small>
        </div>
      </div>
      <div class="card hotels-count">
        <div class="card-header">
          <h3>참여 호텔</h3>
        </div>
        <div class="card-content">
          <p class="amount">{{ salesData.hotelSettlements.length }}</p>
          <small>개 호텔</small>
        </div>
      </div>
      <div class="card avg-settlement">
        <div class="card-header">
          <h3>평균 정산액</h3>
        </div>
        <div class="card-content">
          <p class="amount">{{ formatCurrency(averageSettlement) }}</p>
          <small>호텔당 평균</small>
        </div>
      </div>
    </div>

    <!-- 호텔별 정산 테이블 (호텔별 정산 탭) -->
    <div v-if="viewTab === 'settlements' && salesData && salesData.hotelSettlements.length > 0" class="settlements-section">
      <div class="section-header">
        <h2>호텔별 정산 내역</h2>
        <div class="table-controls">
          <input
            v-model="searchHotel"
            type="text"
            placeholder="호텔명 검색"
            class="search-input"
          />
          <select v-model="sortBy" class="sort-select">
            <option value="revenue">매출액순</option>
            <option value="settlement">정산액순</option>
            <option value="reservations">예약건수순</option>
            <option value="name">호텔명순</option>
          </select>
        </div>
      </div>

      <div class="table-container">
        <table class="settlements-table">
          <thead>
            <tr>
              <th style="width: 80px;">순위</th>
              <th style="min-width: 220px;">호텔명</th>
              <th style="min-width: 140px; text-align:right;">총 매출</th>
              <th style="min-width: 160px; text-align:right;">정산 금액 (90%)</th>
              <th style="min-width: 160px; text-align:right;">플랫폼 수수료 (10%)</th>
              <th style="width: 120px; text-align:center;">예약 건수</th>
              <th style="min-width: 160px; text-align:right;">건당 평균 매출</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(row, index) in filteredSettlements" :key="row.hotelId">
              <td>
                <span class="rank" :class="getRankClass(index)">{{ index + 1 }}</span>
              </td>
              <td>
                <div class="hotel-info">
                  <strong>{{ row.hotelName }}</strong>
                  <small>ID: {{ row.hotelId }}</small>
                </div>
              </td>
              <td class="amount-cell">{{ formatCurrency(row.totalRevenue) }}</td>
              <td class="amount-cell settlement-amount">{{ formatCurrency(row.settlementAmount) }}</td>
              <td class="amount-cell platform-fee">{{ formatCurrency(getPlatformFee(row.totalRevenue)) }}</td>
              <td class="text-center">{{ row.reservationCount }}</td>
              <td class="amount-cell">{{ formatCurrency(getAveragePerReservation(row)) }}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- 정산 요약 -->
      <div class="settlement-summary">
        <div class="summary-item">
          <span>총 정산 호텔:</span>
          <strong>{{ salesData.hotelSettlements.length }}개</strong>
        </div>
        <div class="summary-item">
          <span>총 호텔 정산액:</span>
          <strong>{{ formatCurrency(totalHotelSettlement) }}</strong>
        </div>
        <div class="summary-item">
          <span>총 플랫폼 수수료:</span>
          <strong>{{ formatCurrency(salesData.platformFeeAmount) }}</strong>
        </div>
      </div>
    </div>

    <!-- 매출 분석 섹션 -->
    <div v-if="viewTab === 'analytics'" class="analytics-section">
      <div class="analytics-header">
        <h3>매출 분석</h3>
      </div>

      <div>
        <div class="analytics-filters">
          <div class="filter-row-single">
            <!-- 날짜 입력 그룹 -->
            <div class="date-section">
              <div class="date-input-group">
                <label class="filter-label">기간 FROM</label>
                <input type="date" v-model="analyticsDateRange.from" @change="onDateRangeChange" class="date-input" />
              </div>
              <span class="date-separator">~</span>
              <div class="date-input-group">
                <label class="filter-label">기간 TO</label>
                <input type="date" v-model="analyticsDateRange.to" @change="onDateRangeChange" class="date-input" />
              </div>
            </div>
            
            <!-- 빠른 날짜 버튼들 -->
            <div class="quick-date-buttons">
              <button type="button" class="quick-date-btn" @click="setAnalyticsQuickDate(7)">최근 7일</button>
              <button type="button" class="quick-date-btn active" @click="setAnalyticsQuickDate(30)">최근 30일</button>
              <button type="button" class="quick-date-btn" @click="setAnalyticsQuickDate(90)">최근 3개월</button>
              <button type="button" class="quick-date-btn" @click="setAnalyticsQuickDate(365)">최근 1년</button>
            </div>
            
            <!-- 우측 필터 및 조회 버튼 -->
            <div class="right-section">
              <div class="filter-option">
                <label class="filter-label">호텔</label>
                <select v-model="selectedHotelId" @change="loadAnalytics" class="filter-select">
                  <option value="">전체</option>
                  <option v-for="hotel in hotels" :key="hotel.id" :value="hotel.id">{{ hotel.name }}</option>
                </select>
              </div>
              <div class="filter-option">
                <label class="filter-label">결제 수단</label>
                <select v-model="selectedMethod" @change="loadAnalytics" class="filter-select">
                  <option value="">전체</option>
                  <option value="CREDIT_CARD">신용카드</option>
                  <option value="BANK_TRANSFER">계좌이체</option>
                  <option value="VIRTUAL_ACCOUNT">가상계좌</option>
                  <option value="MOBILE">휴대폰결제</option>
                  <option value="KAKAO_PAY">카카오페이</option>
                  <option value="NAVER_PAY">네이버페이</option>
                </select>
              </div>
              <button class="refresh-btn" :disabled="analyticsLoading" @click="loadAnalytics">
                <span v-if="analyticsLoading">조회중...</span>
                <span v-else">재조회</span>
              </button>
            </div>
          </div>
        </div>

        <div v-if="analyticsError" class="analytics-error">{{ analyticsError }}</div>

        <div v-else>
          <div class="chart-area chart-area-wide">
            <div class="chart-title">기간별 매출 추이</div>
            <div class="chart-body line">
              <canvas v-if="analyticsData.period.length" ref="periodChartRef"></canvas>
              <div v-else class="chart-empty">표시할 데이터가 없습니다.</div>
            </div>
          </div>

          <div class="chart-row">
            <div class="chart-box">
              <div class="chart-title">호텔별 비중</div>
              <div class="chart-body donut">
                <canvas v-if="summaryByHotel.length" ref="hotelChartRef"></canvas>
                <div v-else class="chart-empty">표시할 데이터가 없습니다.</div>
                <div v-if="summaryByHotel.length" class="chart-center-label">
                  <strong>총 매출</strong>
                  <span>{{ formatCurrency(hotelTotalAmount) }}</span>
                </div>
              </div>
              <ul class="chart-legend" v-if="summaryByHotel.length">
                <li v-for="item in summaryByHotel" :key="item.label">
                  <span class="legend-label">{{ item.label }}</span>
                  <span class="legend-value">{{ formatCurrency(item.amount) }} ({{ item.ratio }}%)</span>
                </li>
              </ul>
            </div>
            <div class="chart-box">
              <div class="chart-title">결제 수단별 비중</div>
              <div class="chart-body donut">
                <canvas v-if="summaryByMethod.length" ref="methodChartRef"></canvas>
                <div v-else class="chart-empty">표시할 데이터가 없습니다.</div>
                <div v-if="summaryByMethod.length" class="chart-center-label">
                  <strong>총 매출</strong>
                  <span>{{ formatCurrency(methodTotalAmount) }}</span>
                </div>
              </div>
              <ul class="chart-legend" v-if="summaryByMethod.length">
                <li v-for="item in summaryByMethod" :key="item.label">
                  <span class="legend-label">{{ item.label }}</span>
                  <span class="legend-value">{{ formatCurrency(item.amount) }} ({{ item.ratio }}%)</span>
                </li>
              </ul>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 로딩 상태 -->
    <div v-if="viewTab === 'settlements' && loading" class="loading-message">
      <div class="loading-spinner"></div>
      <p>매출 데이터를 조회하고 있습니다...</p>
    </div>

    <!-- 빈 데이터 메시지 -->
    <div v-else-if="viewTab === 'settlements' && salesData && salesData.hotelSettlements.length === 0" class="empty-message">
      <div class="empty-icon"></div>
      <h3>선택한 기간에 매출 데이터가 없습니다</h3>
      <p>다른 날짜 범위를 선택해보세요.</p>
    </div>

    <!-- 초기 상태 -->
    <div v-else-if="viewTab === 'settlements' && !salesData && !loading" class="initial-message">
      <div class="initial-icon"></div>
      <h3>날짜 범위를 선택하고 조회 버튼을 클릭하세요</h3>
      <p>매출·수수료 관리를 위한 데이터를 조회할 수 있습니다.</p>
    </div>
  </div>
</template>

<script>
import { ref, reactive, computed, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'
import Chart from 'chart.js/auto'
import ChartDataLabels from 'chartjs-plugin-datalabels'
import api from '../../api/http'

export default {
  name: 'SalesManagement',
  setup() {
    const viewTab = ref('settlements') // 기본 탭: 호텔별 정산
    const salesData = ref(null)
    const loading = ref(false)
    const searchHotel = ref('')
    const sortBy = ref('revenue')

    // 매출 분석 관련 상태
    const analyticsLoading = ref(false)
    const analyticsError = ref('')
    const granularity = ref('day')
    const selectedHotelId = ref('')
    const selectedMethod = ref('')
    const analyticsDateRange = reactive({
      from: new Date(new Date().setDate(new Date().getDate() - 30)).toISOString().slice(0, 10),
      to: new Date().toISOString().slice(0, 10)
    })
    const analyticsData = reactive({
      period: [],
      byHotel: [],
      byMethod: []
    })
    const hotels = ref([])

    const periodChartRef = ref(null)
    const hotelChartRef = ref(null)
    const methodChartRef = ref(null)
    let periodChartInstance = null
    let hotelChartInstance = null
    let methodChartInstance = null

    // 날짜 범위 상태
    const dateRange = reactive({
      from: '',
      to: ''
    })

    // 초기 날짜 설정 (최근 30일)
    const initializeDates = () => {
      const today = new Date()
      const thirtyDaysAgo = new Date(today)
      thirtyDaysAgo.setDate(today.getDate() - 30)

      dateRange.to = today.toISOString().split('T')[0]
      dateRange.from = thirtyDaysAgo.toISOString().split('T')[0]
    }

    // 빠른 날짜 설정
    const setQuickDate = (days) => {
      const today = new Date()
      const pastDate = new Date(today)
      pastDate.setDate(today.getDate() - days)

      dateRange.to = today.toISOString().split('T')[0]
      dateRange.from = pastDate.toISOString().split('T')[0]
    }

    // 단위 변경 시 날짜 범위 자동 조정
    const onGranularityChange = () => {
      adjustDateRangeForGranularity()
      loadAnalytics()
    }

    // 단위에 따른 날짜 범위 자동 조정
    const adjustDateRangeForGranularity = () => {
      const today = new Date()
      let fromDate = new Date(today)
      
      switch (granularity.value) {
        case 'day':
          // 일별: 최근 30일
          fromDate.setDate(today.getDate() - 30)
          break
        case 'week':
          // 주별: 최근 12주 (약 3개월)
          fromDate.setDate(today.getDate() - (12 * 7))
          break
        case 'month':
          // 월별: 최근 12개월
          fromDate.setMonth(today.getMonth() - 12)
          break
        case 'year':
          // 년별: 최근 5년
          fromDate.setFullYear(today.getFullYear() - 5)
          break
      }
      
      analyticsDateRange.from = fromDate.toISOString().slice(0, 10)
      analyticsDateRange.to = today.toISOString().slice(0, 10)
    }

    // 날짜 변경 시 적절한 단위 추천
    const onDateRangeChange = () => {
      const fromDate = new Date(analyticsDateRange.from)
      const toDate = new Date(analyticsDateRange.to)
      const diffDays = Math.ceil((toDate - fromDate) / (1000 * 60 * 60 * 24))
      
      // 날짜 범위에 따라 적절한 단위 자동 선택
      if (diffDays <= 31) {
        granularity.value = 'day'
      } else if (diffDays <= 365) {
        granularity.value = 'week'
      } else if (diffDays <= 1095) { // 3년
        granularity.value = 'month'
      } else {
        granularity.value = 'year'
      }
      
      loadAnalytics()
    }

    // 분석용 빠른 날짜 설정
    const setAnalyticsQuickDate = (days) => {
      const today = new Date()
      const pastDate = new Date(today)
      pastDate.setDate(today.getDate() - days)
      
      analyticsDateRange.from = pastDate.toISOString().slice(0, 10)
      analyticsDateRange.to = today.toISOString().slice(0, 10)
      
      // 날짜 범위에 따라 적절한 단위 자동 선택
      if (days <= 31) {
        granularity.value = 'day'
      } else if (days <= 90) {
        granularity.value = 'week'
      } else {
        granularity.value = 'month'
      }
      
      loadAnalytics()
    }

    // 매출 데이터 조회
    const searchSales = async () => {
      if (!dateRange.from || !dateRange.to) {
        alert('시작일과 종료일을 모두 선택해주세요.')
        return
      }

      if (new Date(dateRange.from) > new Date(dateRange.to)) {
        alert('시작일은 종료일보다 이전이어야 합니다.')
        return
      }

      loading.value = true
      try {
        console.log('매출 데이터 조회 요청:', { from: dateRange.from, to: dateRange.to })
        
        const response = await api.get('/admin/stats/sales', {
          params: {
            from: dateRange.from,
            to: dateRange.to
          }
        })

        console.log('매출 데이터 조회 응답:', response.data)
        salesData.value = response.data?.data || response.data
        
      } catch (error) {
        console.error('매출 데이터 조회 오류:', error)
        const status = error?.response?.status
        const message = error?.response?.data?.message || error.message
        
        if (status === 401) {
          alert('인증이 필요합니다. 다시 로그인해주세요.')
        } else if (status === 400) {
          alert(`잘못된 요청입니다: ${message}`)
        } else {
          alert(`매출 데이터를 불러오는데 실패했습니다: ${message}`)
        }
      } finally {
        loading.value = false
      }
    }

    // 필터링된 정산 데이터
    const filteredSettlements = computed(() => {
      if (!salesData.value) return []

      let filtered = salesData.value.hotelSettlements

      // 호텔명 검색
      if (searchHotel.value) {
        filtered = filtered.filter(settlement => 
          settlement.hotelName.toLowerCase().includes(searchHotel.value.toLowerCase())
        )
      }

      // 정렬
      return filtered.sort((a, b) => {
        switch (sortBy.value) {
          case 'revenue':
            return b.totalRevenue - a.totalRevenue
          case 'settlement':
            return b.settlementAmount - a.settlementAmount
          case 'reservations':
            return b.reservationCount - a.reservationCount
          case 'name':
            return a.hotelName.localeCompare(b.hotelName, 'ko')
          default:
            return 0
        }
      })
    })

    // 평균 정산액 계산
    const averageSettlement = computed(() => {
      if (!salesData.value || salesData.value.hotelSettlements.length === 0) return 0
      
      const total = salesData.value.hotelSettlements.reduce((sum, settlement) => 
        sum + settlement.settlementAmount, 0)
      
      return total / salesData.value.hotelSettlements.length
    })

    // 총 호텔 정산액 계산
    const totalHotelSettlement = computed(() => {
      if (!salesData.value) return 0
      
      return salesData.value.hotelSettlements.reduce((sum, settlement) => 
        sum + settlement.settlementAmount, 0)
    })

    // 순위 클래스 반환
    const getRankClass = (index) => {
      if (index === 0) return 'rank-gold'
      if (index === 1) return 'rank-silver'
      if (index === 2) return 'rank-bronze'
      return ''
    }

    // 플랫폼 수수료 계산
    const getPlatformFee = (totalRevenue) => {
      return totalRevenue * 0.10
    }

    // 건당 평균 매출 계산
    const getAveragePerReservation = (settlement) => {
      if (settlement.reservationCount === 0) return 0
      return settlement.totalRevenue / settlement.reservationCount
    }

    // 통화 포맷팅
    const formatCurrency = (amount) => {
      if (!amount) return '₩0'
      return new Intl.NumberFormat('ko-KR', {
        style: 'currency',
        currency: 'KRW'
      }).format(amount)
    }

    // 호텔 목록 로드
    const loadHotels = async () => {
      try {
        const res = await api.get('/admin/hotels', { params: { page: 0, size: 200 } })
        hotels.value = res.data?.data?.content?.map(h => ({ id: h.id, name: h.name })) || []
      } catch (e) {
        hotels.value = []
      }
    }

    // 차트 파괴
    const destroyCharts = () => {
      if (periodChartInstance) {
        periodChartInstance.destroy()
        periodChartInstance = null
      }
      if (hotelChartInstance) {
        hotelChartInstance.destroy()
        hotelChartInstance = null
      }
      if (methodChartInstance) {
        methodChartInstance.destroy()
        methodChartInstance = null
      }
    }

    // 라인 차트 생성
    const createLineChart = (ctx, labels, values) => new Chart(ctx, {
      type: 'line',
      data: {
        labels,
        datasets: [
          {
            label: '매출',
            data: values,
            borderColor: '#2c7be5',
            backgroundColor: 'rgba(44, 123, 229, 0.2)',
            borderWidth: 2,
            tension: 0.32,
            fill: true,
            pointRadius: 3,
            pointHoverRadius: 5,
            pointBackgroundColor: '#2c7be5',
            pointBorderWidth: 0
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        animation: false,
        plugins: {
          legend: { display: false },
          tooltip: { enabled: false },
          datalabels: { display: false }
        },
        elements: { point: { radius: 0 } },
        scales: {
          x: {
            grid: { color: 'rgba(0,0,0,0.04)' },
            ticks: { maxTicksLimit: 12 }
          },
          y: {
            grid: { color: 'rgba(0,0,0,0.05)' },
            ticks: { 
              display: true,
              callback: function(value, index, values) {
                // 금액을 천, 만 단위로 포맷팅
                if (value >= 10000) {
                  return (value / 10000).toFixed(0) + '만원';
                } else if (value >= 1000) {
                  return (value / 1000).toFixed(0) + '천원';
                } else {
                  return value + '원';
                }
              },
              color: '#6c757d',
              font: {
                size: 11
              }
            }
          }
        }
      }
    })

    // 비율 가져오기
    const getRatioByLabel = (label) => {
      const hotelItem = summaryByHotel.value.find(item => item.label === label)
      if (hotelItem) return hotelItem.ratio
      const methodItem = summaryByMethod.value.find(item => item.label === label)
      return methodItem ? methodItem.ratio : 0
    }

    // 도넛 차트 생성
    const createDoughnutChart = (ctx, labels, values, palette) => new Chart(ctx, {
      type: 'doughnut',
      data: {
        labels,
        datasets: [
          {
            data: values,
            backgroundColor: palette,
            hoverOffset: 6,
            borderWidth: 0
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        cutout: '70%',
        plugins: {
          legend: { display: false },
          tooltip: {
            callbacks: {
              label: (ctx) => {
                const formatted = new Intl.NumberFormat('ko-KR', {
                  style: 'currency',
                  currency: 'KRW',
                  maximumFractionDigits: 0
                }).format(ctx.parsed)
                return `${ctx.label}: ${formatted} (${getRatioByLabel(ctx.label)}%)`
              }
            }
          },
          datalabels: {
            color: '#1f2933',
            font: { weight: '600', size: 12 },
            formatter: (value, ctx) => {
              const label = ctx.chart.data.labels[ctx.dataIndex]
              const ratio = getRatioByLabel(label)
              if (ratio < 6) return ''
              return `${label}\n${ratio}%`
            },
            align: 'end',
            anchor: 'end',
            offset: 8,
            clip: false,
            clamp: true,
            padding: 4
          }
        },
        layout: { padding: { left: 28, right: 28, top: 8, bottom: 8 } },
        pluginsList: [ChartDataLabels]
      }
    })

    Chart.register(ChartDataLabels)

    // 차트 빌드
    const buildCharts = () => {
      destroyCharts()

      if (periodChartRef.value && analyticsData.period.length) {
        const ctx = periodChartRef.value.getContext('2d')
        if (ctx) {
          periodChartInstance = createLineChart(
            ctx,
            analyticsData.period.map(item => item.period),
            analyticsData.period.map(item => item.amount)
          )
        }
      }

      if (hotelChartRef.value && summaryByHotel.value.length) {
        const ctx = hotelChartRef.value.getContext('2d')
        if (ctx) {
          hotelChartInstance = createDoughnutChart(
            ctx,
            summaryByHotel.value.map(item => item.label),
            summaryByHotel.value.map(item => item.amount),
            ['#2c7be5', '#6f42c1', '#17a2b8', '#20c997', '#ffc107', '#fd7e14', '#e55353']
          )
        }
      }

      if (methodChartRef.value && summaryByMethod.value.length) {
        const ctx = methodChartRef.value.getContext('2d')
        if (ctx) {
          methodChartInstance = createDoughnutChart(
            ctx,
            summaryByMethod.value.map(item => item.label),
            summaryByMethod.value.map(item => item.amount),
            ['#2c7be5', '#6610f2', '#e83e8c', '#20c997', '#ffc107', '#fd7e14', '#6c757d']
          )
        }
      }
    }

    // 매출 분석 로드
    const loadAnalytics = async () => {
      analyticsLoading.value = true
      analyticsError.value = ''
      try {
        const params = {
          granularity: granularity.value,
          from: analyticsDateRange.from,
          to: analyticsDateRange.to
        }
        if (selectedHotelId.value) params.hotelId = selectedHotelId.value
        if (selectedMethod.value) params.paymentMethod = selectedMethod.value

        console.log('매출 통계 요청:', params)
        const res = await api.get('/admin/payments/analytics', { params })
        console.log('매출 통계 응답:', res.data)
        const data = res.data?.data || {}
        analyticsData.period = data.byPeriod || []
        analyticsData.byHotel = data.byHotel || []
        analyticsData.byMethod = data.byMethod || []
        if (viewTab.value === 'analytics') {
          await nextTick()
          buildCharts()
        }
      } catch (e) {
        console.error('매출 통계 로딩 오류:', e)
        const status = e?.response?.status
        const message = e?.response?.data?.message || e.message
        if (status === 401) {
          analyticsError.value = '인증이 필요합니다. 다시 로그인해주세요.'
        } else if (status === 400) {
          analyticsError.value = `잘못된 요청입니다: ${message}`
        } else {
          analyticsError.value = `매출 통계를 불러오는데 실패했습니다: ${message}`
        }
        analyticsData.period = []
        analyticsData.byHotel = []
        analyticsData.byMethod = []
        destroyCharts()
      } finally {
        analyticsLoading.value = false
      }
    }

    // 매출 분석 computed
    const summaryByHotel = computed(() => {
      const total = analyticsData.byHotel.reduce((acc, cur) => acc + cur.amount, 0)
      return analyticsData.byHotel.map(item => ({
        label: item.label,
        amount: item.amount,
        count: item.count,
        ratio: total ? Math.round((item.amount / total) * 100) : 0
      }))
    })

    const summaryByMethod = computed(() => {
      const total = analyticsData.byMethod.reduce((acc, cur) => acc + cur.amount, 0)
      return analyticsData.byMethod.map(item => ({
        label: item.label,
        amount: item.amount,
        count: item.count,
        ratio: total ? Math.round((item.amount / total) * 100) : 0
      }))
    })

    const hotelTotalAmount = computed(() => summaryByHotel.value.reduce((acc, cur) => acc + cur.amount, 0))
    const methodTotalAmount = computed(() => summaryByMethod.value.reduce((acc, cur) => acc + cur.amount, 0))

    // 차트 데이터 변경 감지
    watch(
      () => ({
        period: analyticsData.period,
        hotel: summaryByHotel.value,
        method: summaryByMethod.value
      }),
      async () => {
        if (viewTab.value === 'analytics') {
          await nextTick()
          buildCharts()
        }
      },
      { deep: true }
    )

    // 탭 변경 감지
    watch(viewTab, async (newTab) => {
      if (newTab === 'analytics') {
        await nextTick()
        if (analyticsData.period.length || analyticsData.byHotel.length || analyticsData.byMethod.length) {
          buildCharts()
        } else {
          loadAnalytics()
        }
      }
    })

    // 컴포넌트 마운트 시 초기화
    onMounted(() => {
      initializeDates()
      loadHotels()
    })

    onBeforeUnmount(() => {
      destroyCharts()
    })

    return {
      viewTab,
      salesData,
      loading,
      searchHotel,
      sortBy,
      dateRange,
      filteredSettlements,
      averageSettlement,
      totalHotelSettlement,
      setQuickDate,
      searchSales,
      getRankClass,
      getPlatformFee,
      getAveragePerReservation,
      formatCurrency,
      // 매출 분석
      analyticsLoading,
      analyticsError,
      granularity,
      selectedHotelId,
      selectedMethod,
      analyticsDateRange,
      analyticsData,
      hotels,
      summaryByHotel,
      summaryByMethod,
      hotelTotalAmount,
      methodTotalAmount,
      loadAnalytics,
      periodChartRef,
      hotelChartRef,
      methodChartRef,
      onGranularityChange,
      onDateRangeChange,
      setAnalyticsQuickDate,
      adjustDateRangeForGranularity
    }
  }
}

</script>
<style scoped src="@/assets/css/admin/sales-management.css"></style>