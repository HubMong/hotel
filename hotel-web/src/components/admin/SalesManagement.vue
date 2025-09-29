<template>
  <div class="sales-management">
    <div class="page-header">
      <h1>매출·수수료 관리</h1>
      <p class="page-description">지정된 기간의 매출과 수수료를 요약하여 확인합니다.</p>
    </div>

    <!-- 날짜 범위 선택 -->
    <div class="date-filter">
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
      </div>
    </div>

    <!-- 매출 요약 카드 -->
    <div class="summary-cards" v-if="salesData">
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

    <!-- 호텔별 정산 테이블 -->
    <div class="settlements-section" v-if="salesData && salesData.hotelSettlements.length > 0">
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

    <!-- 빈 데이터 메시지 -->
    <div v-else-if="salesData && salesData.hotelSettlements.length === 0" class="empty-message">
      <div class="empty-icon"></div>
      <h3>선택한 기간에 매출 데이터가 없습니다</h3>
      <p>다른 날짜 범위를 선택해보세요.</p>
    </div>

    <!-- 로딩 상태 -->
    <div v-else-if="loading" class="loading-message">
      <div class="loading-spinner"></div>
      <p>매출 데이터를 조회하고 있습니다...</p>
    </div>

    <!-- 초기 상태 -->
    <div v-else class="initial-message">
      <div class="initial-icon"></div>
      <h3>날짜 범위를 선택하고 조회 버튼을 클릭하세요</h3>
      <p>매출·수수료 관리를 위한 데이터를 조회할 수 있습니다.</p>
    </div>
  </div>
</template>

<script>
import { ref, reactive, computed, onMounted } from 'vue'

export default {
  name: 'SalesManagement',
  setup() {
    const salesData = ref(null)
    const loading = ref(false)
    const searchHotel = ref('')
    const sortBy = ref('revenue')

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
        const params = new URLSearchParams({
          from: dateRange.from,
          to: dateRange.to
        })

        const response = await fetch(`/api/admin/stats/sales?${params}`, {
          headers: {
            'Authorization': `Bearer ${localStorage.getItem('token')}`
          }
        })

        if (!response.ok) throw new Error('매출 데이터 조회 실패')

        salesData.value = await response.json()
        
      } catch (error) {
        alert('매출 데이터를 불러오는데 실패했습니다.')
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

    // 컴포넌트 마운트 시 초기화
    onMounted(() => {
      initializeDates()
    })

    return {
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
      formatCurrency
    }
  }
}

</script>
<style scoped src="@/assets/css/admin/sales-management.css"></style>