<template>
  <div class="reservation-monitoring">
    <div class="page-header">
      <h1>예약 관리</h1>
      <p class="page-description">호텔 예약 및 결제 상태를 모니터링하고 관리합니다.</p>
    </div>

    <div class="search-section mb-16">
      <form class="search-form" @submit.prevent="searchReservations">
        <div class="search-group">
          <label>호텔명</label>
          <input v-model="filters.hotelName" placeholder="호텔명 검색" @keyup.enter="searchReservations" class="search-input" />
        </div>
        <div class="search-group">
          <label>예약자명</label>
          <input v-model="filters.userName" placeholder="예약자명 검색" @keyup.enter="searchReservations" class="search-input" />
        </div>
        <div class="search-group">
          <label>예약 상태</label>
          <select v-model="filters.reservationStatus" class="search-select">
            <option value="">전체</option>
            <option value="PENDING">예약 대기</option>
            <option value="COMPLETED">예약 확정</option>
            <option value="CANCELLED">예약 취소</option>
          </select>
        </div>
        <div class="search-group">
          <label>결제 상태</label>
          <select v-model="filters.paymentStatus" class="search-select">
            <option value="">전체</option>
            <option value="COMPLETED">결제완료</option>
            <option value="CANCELLED">결제취소</option>
            <option value="PENDING">결제대기</option>
            <option value="FAILED">결제실패</option>
          </select>
        </div>
        <div class="search-buttons">
          <button class="btn btn-primary" type="submit">검색</button>
          <button class="btn btn-secondary" type="button" @click="resetFilters">초기화</button>
        </div>
      </form>
    </div>

    <div class="stats-row mb-16">
      <div class="stat-card"><div>전체 예약</div><div class="stat-number">{{ totalReservations }}</div></div>
      <div class="stat-card"><div>확정 예약</div><div class="stat-number confirmed">{{ confirmedReservations }}</div></div>
      <div class="stat-card"><div>대기 예약</div><div class="stat-number pending">{{ pendingReservations }}</div></div>
      <div class="stat-card"><div>취소 예약</div><div class="stat-number cancelled">{{ cancelledReservations }}</div></div>
      <div class="stat-card"><div>결제 완료</div><div class="stat-number paid">{{ paidReservations }}</div></div>
    </div>

    <div class="view-toggle">
      <button :class="['toggle-btn', { active: viewMode === 'calendar' }]" @click="viewMode = 'calendar'">캘린더</button>
      <button :class="['toggle-btn', { active: viewMode === 'table' }]" @click="viewMode = 'table'">목록</button>
    </div>

    <div v-if="viewMode === 'calendar'" class="calendar-section">
      <div class="calendar-controls">
        <div class="left">
          <button class="btn btn-sm" :disabled="calendarLoading" @click="changeMonth(-1)">이전 달</button>
          <strong class="month-label">{{ formatMonth(calendarMonth) }}</strong>
          <button class="btn btn-sm" :disabled="calendarLoading" @click="changeMonth(1)">다음 달</button>
        </div>
        <div class="right">
          <select v-model="calendarStatus" class="calendar-select" @change="loadCalendar">
            <option value="">상태 전체</option>
            <option value="COMPLETED">예약 확정</option>
            <option value="PENDING">변경 요청/대기</option>
            <option value="CANCELLED">취소</option>
          </select>
          <select v-model="calendarHotelId" class="calendar-select" @change="loadCalendar">
            <option value="">호텔 전체</option>
            <option v-for="hotel in calendarHotels" :key="hotel.id" :value="hotel.id">{{ hotel.name }}</option>
          </select>
          <button class="btn btn-sm btn-outline" :disabled="calendarLoading" @click="loadCalendar">새로고침</button>
          <div class="calendar-legend" aria-label="객실 상태 설명">
            <span class="legend-item"><span class="dot dot-confirmed" aria-hidden="true"></span> 확정</span>
            <span class="legend-item"><span class="dot dot-pending" aria-hidden="true"></span> 대기</span>
            <span class="legend-item"><span class="dot dot-cancelled" aria-hidden="true"></span> 취소</span>
          </div>
        </div>
      </div>

      <div v-if="calendarLoading" class="calendar-loading">달력을 불러오는 중...</div>
      <div v-else class="calendar-grid">
        <div class="calendar-header">
          <div class="cell">일</div>
          <div class="cell">월</div>
          <div class="cell">화</div>
          <div class="cell">수</div>
          <div class="cell">목</div>
          <div class="cell">금</div>
          <div class="cell">토</div>
        </div>
        <div class="calendar-body">
          <div
            v-for="day in calendarDays"
            :key="day.key"
            class="day-cell"
            :class="[{ outside: day.outside }, day.statusClass]"
            @click="openDayModal(day)"
          >
            <div class="day-header">
              <span class="day-number">{{ day.date.getDate() }}</span>
              <span class="badge" v-if="day.total">{{ day.total }}</span>
              <div class="status-dots" role="img" :aria-label="`확정 ${day.confirmed||0}건, 대기 ${day.pending||0}건, 취소 ${day.cancelled||0}건`">
                <span v-if="day.confirmed > 0" class="dot dot-confirmed" title="확정"></span>
                <span v-if="day.pending > 0" class="dot dot-pending" title="대기"></span>
                <span v-if="day.cancelled > 0" class="dot dot-cancelled" title="취소"></span>
              </div>
            </div>
            <div class="day-content" v-if="day.summary">
              <div class="summary-line" :class="day.summary.statusClass">
                {{ day.summary.text }}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-else class="table-section">
      <table class="reservations-table">
        <thead>
          <tr>
            <th :class="['sortable', { active: sort.prop==='reservationId', asc: sort.prop==='reservationId' && sort.dir==='asc', desc: sort.prop==='reservationId' && sort.dir==='desc' }]" @click="sortBy('reservationId')">예약번호
              <span class="sort-group" :class="sortGroupClass('reservationId')">
                <span class="sort-caret up"></span>
                <span class="sort-caret down"></span>
              </span>
            </th>
            <th>호텔명</th>
            <th>객실</th>
            <th>예약자</th>
            <th>체크인</th>
            <th>체크아웃</th>
            <th :class="['text-right','sortable', { active: sort.prop==='totalPrice', asc: sort.prop==='totalPrice' && sort.dir==='asc', desc: sort.prop==='totalPrice' && sort.dir==='desc' }]" @click="sortBy('totalPrice')">예약금액
              <span class="sort-group" :class="sortGroupClass('totalPrice')">
                <span class="sort-caret up"></span>
                <span class="sort-caret down"></span>
              </span>
            </th>
            <th :class="['sortable', { active: sort.prop==='reservationStatus', asc: sort.prop==='reservationStatus' && sort.dir==='asc', desc: sort.prop==='reservationStatus' && sort.dir==='desc' }]" @click="sortBy('reservationStatus')">예약상태
              <span class="sort-group" :class="sortGroupClass('reservationStatus')">
                <span class="sort-caret up"></span>
                <span class="sort-caret down"></span>
              </span>
            </th>
            <th>결제상태</th>
            <th :class="['sortable', { active: sort.prop==='createdAt', asc: sort.prop==='createdAt' && sort.dir==='asc', desc: sort.prop==='createdAt' && sort.dir==='desc' }]" @click="sortBy('createdAt')">예약일시
              <span class="sort-group" :class="sortGroupClass('createdAt')">
                <span class="sort-caret up"></span>
                <span class="sort-caret down"></span>
              </span>
            </th>
            <th>액션</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in reservations" :key="row.reservationId">
            <td>{{ row.reservationId }}</td>
            <td>{{ row.hotelName }}</td>
            <td>{{ row.roomName }} ({{ row.roomType }})</td>
            <td>
              <div>{{ row.userName }}</div>
              <small class="muted">{{ row.userEmail }}</small>
            </td>
            <td>{{ formatDate(row.checkInDate) }}</td>
            <td>{{ formatDate(row.checkOutDate) }}</td>
            <td class="text-right">{{ formatCurrency(row.totalAmount) }}</td>
            <td>{{ getReservationStatusText(row.reservationStatus) }}</td>
            <td>{{ getPaymentStatusText(row.paymentStatus) }}</td>
            <td>{{ formatDateTime(row.reservationCreatedAt) }}</td>
            <td class="actions-cell">
              <div class="action-buttons">
                <div class="primary-actions">
                  <button class="btn btn-secondary btn-sm" @click="viewReservationDetail(row)">
                    상세
                  </button>
                </div>
                <div class="status-actions" v-if="row.reservationStatus !== 'CANCELLED'">
                  <button 
                    v-if="row.reservationStatus === 'PENDING'"
                    class="btn btn-success btn-sm" 
                    @click="updateReservationStatus(row.reservationId, 'COMPLETED')"
                    :disabled="updatingStatus"
                  >
                    <span class="btn-icon">✓</span> 확정
                  </button>
                  <button 
                    class="btn btn-danger btn-sm" 
                    @click="updateReservationStatus(row.reservationId, 'CANCELLED')"
                    :disabled="updatingStatus"
                  >
                    <span class="btn-icon">✕</span> 취소
                  </button>
                </div>
                <div class="status-display" v-else>
                  <span class="status-badge cancelled">
                    <span class="badge-icon">🚫</span> 취소됨
                  </span>
                </div>
              </div>
            </td>
          </tr>
          <tr v-if="reservations.length === 0">
            <td colspan="11" class="muted center">조건에 맞는 예약이 없습니다.</td>
          </tr>
        </tbody>
      </table>
    </div>

    <div class="pagination" v-if="pagination.totalPages > 1">
      <button
        @click="changePage(pagination.currentPage - 1)"
        :disabled="pagination.currentPage === 0"
        class="page-btn"
      >
        이전
      </button>

      <span class="page-info">
        {{ pagination.currentPage + 1 }} / {{ pagination.totalPages }}
      </span>

      <button
        @click="changePage(pagination.currentPage + 1)"
        :disabled="pagination.currentPage >= pagination.totalPages - 1"
        class="page-btn"
      >
        다음
      </button>
    </div>

    <div v-if="showDayModal" class="modal-overlay" @click="closeDayModal">
      <div class="modal day-modal" @click.stop>
        <div class="modal-header">
          <h3>{{ selectedDay?.date }}</h3>
          <button class="close" @click="closeDayModal">×</button>
        </div>
        <div class="modal-body">
          <div v-if="selectedDay?.reservations?.length">
            <div class="reservation-chip" v-for="item in selectedDay.reservations" :key="item.reservationId" :class="['status-' + (item.status || '').toLowerCase()]">
              <div class="chip-header">
                <strong>#{{ item.reservationId }}</strong>
                <span class="status-label">{{ getReservationStatusText(item.status) }}</span>
              </div>
              <div class="chip-body">
                <div class="chip-line">{{ item.guestName }} ({{ item.guestCount }}명)</div>
                <div class="chip-line muted">{{ item.roomType }}</div>
              </div>
              <div class="chip-actions">
                <button class="chip-action" @click="viewReservationDetail({ reservationId: item.reservationId })">
                  상세
                </button>
                <div class="status-actions" v-if="item.status !== 'CANCELLED'">
                  <button 
                    v-if="item.status === 'PENDING'"
                    class="chip-action success" 
                    @click="updateReservationStatus(item.reservationId, 'COMPLETED')"
                    :disabled="updatingStatus"
                  >
                    <span class="btn-icon">✓</span> 확정
                  </button>
                  <button 
                    class="chip-action danger" 
                    @click="updateReservationStatus(item.reservationId, 'CANCELLED')"
                    :disabled="updatingStatus"
                  >
                    <span class="btn-icon">✕</span> 취소
                  </button>
                </div>
              </div>
            </div>
          </div>
          <div v-else class="empty">예약이 없습니다.</div>
        </div>
      </div>
    </div>

    <div v-if="showDetailModal" class="modal-overlay" @click="closeDetailModal">
      <div class="modal" @click.stop>
        <div class="modal-header">
          <h3>예약 상세 정보</h3>
          <button class="close" @click="closeDetailModal">×</button>
        </div>
        <div class="modal-body" v-if="selectedReservation">
          <div class="detail-grid">
            <section class="detail-card">
              <h4>예약 정보</h4>
              <div class="desc-grid">
                <div><label>예약번호</label><span>{{ selectedReservation.reservationId }}</span></div>
                <div><label>예약상태</label><span>{{ getReservationStatusText(selectedReservation.reservationStatus) }}</span></div>
                <div><label>체크인</label><span>{{ formatDate(selectedReservation.checkInDate) }}</span></div>
                <div><label>체크아웃</label><span>{{ formatDate(selectedReservation.checkOutDate) }}</span></div>
                <div><label>투숙인원</label><span>{{ selectedReservation.guestCount }}명</span></div>
                <div><label>총 금액</label><span>{{ formatCurrency(selectedReservation.totalAmount) }}</span></div>
                <div v-if="selectedReservation.specialRequests" class="span-2"><label>특별요청</label><span>{{ selectedReservation.specialRequests }}</span></div>
                <div class="span-2"><label>예약일시</label><span>{{ formatDateTime(selectedReservation.reservationCreatedAt) }}</span></div>
              </div>
            </section>

            <section class="detail-card">
              <h4>결제 정보</h4>
              <div class="desc-grid">
                <div v-if="selectedReservation.paymentId"><label>결제번호</label><span>{{ selectedReservation.paymentId }}</span></div>
                <div><label>결제상태</label><span>{{ getPaymentStatusText(selectedReservation.paymentStatus) }}</span></div>
                <div v-if="selectedReservation.paymentMethod"><label>결제수단</label><span>{{ getPaymentMethodText(selectedReservation.paymentMethod) }}</span></div>
                <div v-if="selectedReservation.paidAmount"><label>결제금액</label><span>{{ formatCurrency(selectedReservation.paidAmount) }}</span></div>
                <div v-if="selectedReservation.paymentCreatedAt" class="span-2"><label>결제일시</label><span>{{ formatDateTime(selectedReservation.paymentCreatedAt) }}</span></div>
              </div>
            </section>

            <section class="detail-card">
              <h4>고객 정보</h4>
              <div class="desc-grid">
                <!-- 고객번호 항목 제거됨 -->
                <div><label>고객명</label><span>{{ selectedReservation.userName }}</span></div>
                <div><label>이메일</label><span>{{ selectedReservation.userEmail }}</span></div>
                <div><label>전화번호</label><span>{{ selectedReservation.userPhone }}</span></div>
              </div>
            </section>

            <section class="detail-card">
              <h4>호텔/객실 정보</h4>
              <div class="desc-grid">
                <div><label>호텔번호</label><span>{{ selectedReservation.hotelId }}</span></div>
                <div><label>호텔명</label><span>{{ selectedReservation.hotelName }}</span></div>
                <div><label>객실번호</label><span>{{ selectedReservation.roomId }}</span></div>
                <div><label>객실명</label><span>{{ selectedReservation.roomName }}</span></div>
                <div class="span-2"><label>객실타입</label><span>{{ selectedReservation.roomType }}</span></div>
              </div>
            </section>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn" @click="closeDetailModal">닫기</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue'
import axios from '@/api/http.js'

export default {
  name: 'ReservationMonitoring',
  setup() {
  const reservations = ref([])
  // 정렬 상태
  const sort = ref({ prop: 'createdAt', dir: 'desc' })
    const selectedReservation = ref(null)
    const showDetailModal = ref(false)
    const viewMode = ref('calendar')

    const calendarMonth = ref(new Date())
    const calendarDays = ref([])
    const calendarLoading = ref(false)
    const calendarError = ref('')
    const calendarStatus = ref('')
    const calendarHotelId = ref('')
    const calendarHotels = ref([])
    const showDayModal = ref(false)
    const selectedDay = ref(null)
    const updatingStatus = ref(false)
    
    // 필터 상태
    const filters = reactive({
      hotelName: '',
      userName: '',
      reservationStatus: '',
      paymentStatus: ''
    })

    // 페이지네이션 상태
    const pagination = reactive({
      currentPage: 0,
      totalPages: 0,
      totalElements: 0,
      size: 20
    })

    // 통계 데이터
    const totalReservations = ref(0)
    const confirmedReservations = ref(0)
    const pendingReservations = ref(0)
    const cancelledReservations = ref(0)
    const paidReservations = ref(0)

    // 예약 목록 조회
    const searchReservations = async () => {
      try {
        const params = {
          page: pagination.currentPage,
          size: pagination.size
        }

        if (filters.hotelName) params.hotelName = filters.hotelName
        if (filters.userName) params.userName = filters.userName
        if (filters.reservationStatus) params.status = filters.reservationStatus
        if (filters.paymentStatus) params.paymentStatus = filters.paymentStatus

  // 정렬 파라미터 전달 (Spring Pageable과 호환되는 sort=prop,dir)
  // 비활성 컬럼 방지: 호텔명/체크인/체크아웃은 정렬값으로 보내지 않음
  const disabled = ['hotelName','startDate','endDate']
  const propToSend = disabled.includes(sort.value.prop) ? 'createdAt' : sort.value.prop
  const dirToSend = disabled.includes(sort.value.prop) ? 'desc' : sort.value.dir
  params.sort = `${propToSend},${dirToSend}`
  const response = await axios.get('/admin/reservations', { params })
        const data = response.data?.data || {
          content: [],
          totalPages: 0,
          totalElements: 0
        }
        
        const items = Array.isArray(data.content) ? data.content : []
        // BE ReservationDetailDto -> UI 행 변환 (실제 데이터 매핑)
        reservations.value = items.map(r => ({
          reservationId: r.reservationId,
          hotelName: r.hotelName || '-',
          roomName: r.roomName || '-',
          roomType: r.roomType || '-',
          userName: r.userName || '-',
          userEmail: r.userEmail || '',
          userPhone: r.userPhone || '',
          checkInDate: r.startDate,
          checkOutDate: r.endDate,
          totalAmount: r.totalPrice ?? 0,
          reservationStatus: r.reservationStatus || 'PENDING',
          paymentStatus: r.paymentStatus || (r.paymentId ? 'COMPLETED' : 'NONE'),
          reservationCreatedAt: r.createdAt,
          hotelId: r.hotelId,
          roomId: r.roomId,
          paymentId: r.paymentId,
          paymentMethod: r.paymentMethod,
          paidAmount: r.totalPrice ?? 0,
          paymentCreatedAt: r.paymentCreatedAt,
          specialRequests: null,
          guestCount: (r.numAdult || 0) + (r.numKid || 0)
        }))
        pagination.totalPages = data.totalPages || 0
        pagination.totalElements = data.totalElements || 0

        // 통계 업데이트 (실제로는 별도 API 호출)
        updateStatistics()
        
      } catch (error) {
        alert('예약 목록을 불러오는데 실패했습니다.')
      }
    }

    // 통계 업데이트
    const updateStatistics = () => {
      totalReservations.value = reservations.value.length
      confirmedReservations.value = reservations.value.filter(r => r.reservationStatus === 'COMPLETED').length
      pendingReservations.value = reservations.value.filter(r => r.reservationStatus === 'PENDING').length
      cancelledReservations.value = reservations.value.filter(r => r.reservationStatus === 'CANCELLED').length
      paidReservations.value = reservations.value.filter(r => r.paymentStatus === 'COMPLETED').length
    }

    // 페이지 변경
    const changePage = (page) => {
      pagination.currentPage = page
      searchReservations()
    }

    // 정렬 변경
    const sortBy = (prop) => {
  // 제한: 호텔명, 체크인, 체크아웃, 예약자, 결제상태는 정렬 비활성화
  if (prop === 'hotelName' || prop === 'startDate' || prop === 'endDate' || prop === 'userName' || prop === 'paymentStatus') return
  const disabled = ['hotelName','startDate','endDate','userName','paymentStatus']
      if (sort.value.prop === prop) {
        sort.value.dir = sort.value.dir === 'asc' ? 'desc' : 'asc'
      } else {
        sort.value.prop = prop
        sort.value.dir = 'asc'
      }
      pagination.currentPage = 0
      searchReservations()
    }

    const sortGroupClass = (prop) => {
      if (sort.value.prop !== prop) return ''
      return sort.value.dir === 'asc' ? 'asc' : 'desc'
    }

    // 필터 초기화
    const resetFilters = () => {
      filters.hotelName = ''
      filters.userName = ''
      filters.reservationStatus = ''
      filters.paymentStatus = ''
      pagination.currentPage = 0
      searchReservations()
    }

    const loadHotels = async () => {
      try {
        const res = await axios.get('/admin/hotels', { params: { page: 0, size: 200 } })
        const data = res.data?.data?.content || []
        calendarHotels.value = data.map(h => ({ id: h.id, name: h.name }))
      } catch (e) {
        calendarHotels.value = []
      }
    }

    const formatMonth = (date) => {
      return `${date.getFullYear()}년 ${String(date.getMonth() + 1).padStart(2, '0')}월`
    }

    const buildCalendarDays = (baseDate, rawDays) => {
      const days = []
      const year = baseDate.getFullYear()
      const month = baseDate.getMonth()
      const firstDay = new Date(year, month, 1)
      const startOffset = firstDay.getDay()
      const startDate = new Date(year, month, 1 - startOffset)

      for (let i = 0; i < 42; i++) {
        const date = new Date(startDate)
        date.setDate(startDate.getDate() + i)
        const key = date.toISOString().slice(0, 10)
        const serverDay = rawDays.find(d => d.date === key)
        let statusClass = 'status-empty'
        let summary = null

        if (serverDay) {
          if (serverDay.totalReservations > 0) {
            if (serverDay.cancelledReservations > 0) statusClass = 'status-cancelled'
            if (serverDay.pendingReservations > 0) statusClass = 'status-pending'
            if (serverDay.confirmedReservations > 0) statusClass = 'status-confirmed'
            summary = {
              statusClass,
              text: `${serverDay.reservations[0]?.guestName || ''} 외 ${Math.max(serverDay.totalReservations - 1, 0)}건`
            }
          }
        }

        days.push({
          key,
          date,
          outside: date.getMonth() !== month,
          total: serverDay?.totalReservations || 0,
          statusClass,
          summary,
          // 개별 상태 카운트 (상태 동그라미 표시용)
          confirmed: serverDay?.confirmedReservations || 0,
          pending: serverDay?.pendingReservations || 0,
          cancelled: serverDay?.cancelledReservations || 0,
          raw: serverDay
        })
      }

      calendarDays.value = days
    }

    const loadCalendar = async () => {
      calendarLoading.value = true
      calendarError.value = ''
      try {
        const start = new Date(calendarMonth.value.getFullYear(), calendarMonth.value.getMonth(), 1)
        const end = new Date(calendarMonth.value.getFullYear(), calendarMonth.value.getMonth() + 1, 0)

        const params = {
          monthStart: start.toISOString(),
          monthEnd: end.toISOString()
        }
        if (calendarStatus.value) params.status = calendarStatus.value
        if (calendarHotelId.value) params.hotelId = calendarHotelId.value
        // 사용자 이름 필터 지원
        const urlParams = new URLSearchParams(window.location.search)
        const userName = urlParams.get('userName')
        if (userName) params.userName = userName

        const res = await axios.get('/admin/reservations/calendar', { params })
        const data = res.data?.data || []
        buildCalendarDays(calendarMonth.value, data.map(d => ({
          date: d.date,
          totalReservations: d.totalReservations,
          confirmedReservations: d.confirmedReservations,
          pendingReservations: d.pendingReservations,
          cancelledReservations: d.cancelledReservations,
          reservations: d.reservations || []
        })))
      } catch (e) {
        calendarError.value = '달력 데이터를 불러오는데 실패했습니다.'
        calendarDays.value = []
      } finally {
        calendarLoading.value = false
      }
    }

    const changeMonth = (delta) => {
      const next = new Date(calendarMonth.value)
      next.setMonth(next.getMonth() + delta)
      calendarMonth.value = next
      loadCalendar()
    }

    const openDayModal = (day) => {
      if (!day.raw) return
      selectedDay.value = day.raw
      showDayModal.value = true
    }

    const closeDayModal = () => {
      showDayModal.value = false
      selectedDay.value = null
    }

    // 예약 상세 보기
    const viewReservationDetail = async (reservation) => {
      try {
        console.log('예약 상세 조회 시작:', reservation.reservationId)
        
        if (!reservation.reservationId) {
          alert('예약 ID가 없습니다.')
          return
        }
        
        const response = await axios.get(`/admin/reservations/${reservation.reservationId}`)
        console.log('예약 상세 조회 응답:', response.data)
        
        const data = response.data?.data || {}
        
        if (!data.reservationId) {
          console.warn('응답 데이터가 비어있음:', data)
          alert('예약 데이터를 찾을 수 없습니다.')
          return
        }
        
        // ReservationDetailDto -> 상세 모달용 데이터 변환 (안전한 처리)
        selectedReservation.value = {
          reservationId: data.reservationId || reservation.reservationId,
          transactionId: data.transactionId || '-',
          reservationStatus: data.reservationStatus || reservation.reservationStatus || 'PENDING',
          reservationCreatedAt: data.createdAt || reservation.reservationCreatedAt,
          expiresAt: data.expiresAt || null,

          hotelId: data.hotelId || reservation.hotelId || null,
          hotelName: data.hotelName || reservation.hotelName || '-',
          roomId: data.roomId || reservation.roomId,
          roomName: data.roomName || reservation.roomName || '-',
          roomType: data.roomType || reservation.roomType || '-',

          userName: data.userName || reservation.userName || '-',
          userEmail: data.userEmail || reservation.userEmail || '-',
          userPhone: data.userPhone || reservation.userPhone || '-',

          checkInDate: data.startDate || reservation.checkInDate,
          checkOutDate: data.endDate || reservation.checkOutDate,
          guestCount: (data.numAdult || 0) + (data.numKid || 0) || reservation.guestCount || 0,

          paymentMethod: data.paymentMethod || reservation.paymentMethod || '-',
          totalAmount: data.totalPrice ?? reservation.totalAmount ?? 0,
          paymentStatus: data.paymentStatus || reservation.paymentStatus || 'NONE'
        }
        
        console.log('상세 모달 데이터 설정 완료:', selectedReservation.value)
        showDetailModal.value = true
        
      } catch (error) {
        console.error('예약 상세 조회 오류:', error)
        console.error('오류 응답:', error.response?.data)
        
        let errorMessage = '예약 상세 정보를 불러오는데 실패했습니다.'
        if (error.response?.status === 404) {
          errorMessage = '예약 정보를 찾을 수 없습니다.'
        } else if (error.response?.status === 500) {
          errorMessage = '서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.'
        } else if (error.response?.data?.message) {
          errorMessage = error.response.data.message
        }
        
        alert(errorMessage)
      }
    }

    // 상세 모달 닫기
    const closeDetailModal = () => {
      showDetailModal.value = false
      selectedReservation.value = null
    }

    // 예약 상태 텍스트
    const getReservationStatusText = (status) => {
      const statusMap = {
        'PENDING': '대기중',
        'COMPLETED': '확정',
        'CANCELLED': '취소'
      }
      return statusMap[status] || status
    }

    const reservationTagType = (status) => {
      const map = { PENDING: 'warning', COMPLETED: 'success', CANCELLED: 'danger' }
      return map[status] || 'info'
    }
    const paymentTagType = (status) => {
      const map = { COMPLETED: 'success', CANCELLED: 'info', PENDING: 'warning', FAILED: 'danger', NONE: 'warning' }
      return map[status] || 'info'
    }

    // 예약 상태 클래스
    const getReservationStatusClass = (status) => {
      const classMap = {
        'PENDING': 'status-pending',
        'COMPLETED': 'status-confirmed',
        'CANCELLED': 'status-cancelled'
      }
      return classMap[status] || ''
    }

    // 결제 상태 텍스트
    const getPaymentStatusText = (status) => {
      if (!status || status === 'NONE') return '결제정보없음'
      const statusMap = {
        'COMPLETED': '결제완료',
        'CANCELLED': '취소/환불',
        'PENDING': '결제대기',
        'FAILED': '결제실패'
      }
      return statusMap[status] || status
    }

    // 결제 상태 클래스
    const getPaymentStatusClass = (status) => {
      if (!status || status === 'NONE') return 'status-no-payment'
      const classMap = {
        'COMPLETED': 'status-completed',
        'CANCELLED': 'status-cancelled',
        'PENDING': 'status-pending',
        'FAILED': 'status-failed'
      }
      return classMap[status] || ''
    }

    // 결제 수단 텍스트
    const getPaymentMethodText = (method) => {
      if (!method) return '미지정'
      const methodMap = {
        'CREDIT_CARD': '신용카드',
        'BANK_TRANSFER': '계좌이체',
        'VIRTUAL_ACCOUNT': '가상계좌',
        'MOBILE': '휴대폰결제',
        'KAKAO_PAY': '카카오페이',
        'NAVER_PAY': '네이버페이'
      }
      return methodMap[method] || method
    }

    // 날짜 포맷팅
    const formatDate = (dateString) => {
      if (!dateString) return '-'
      return new Date(dateString).toLocaleDateString('ko-KR')
    }

    // 날짜시간 포맷팅
    const formatDateTime = (dateString) => {
      if (!dateString) return '-'
      return new Date(dateString).toLocaleString('ko-KR')
    }

    // 통화 포맷팅
    const formatCurrency = (amount) => {
      if (!amount) return '-'
      return new Intl.NumberFormat('ko-KR', {
        style: 'currency',
        currency: 'KRW'
      }).format(amount)
    }

    // 예약 상태 변경
    const updateReservationStatus = async (reservationId, newStatus) => {
      if (updatingStatus.value) return
      
      const statusText = {
        'COMPLETED': '확정',
        'CANCELLED': '취소'
      }[newStatus] || newStatus
      
      const confirmMessage = `예약을 ${statusText} 처리하시겠습니까?`
      if (!confirm(confirmMessage)) return
      
      updatingStatus.value = true
      try {
        await axios.put(`/admin/reservations/${reservationId}/status?status=${newStatus}`)
        alert(`예약이 ${statusText} 처리되었습니다.`)
        
        // 목록 새로고침
        await searchReservations()
        
        // 캘린더도 새로고침 (캘린더 뷰인 경우)
        if (viewMode.value === 'calendar') {
          await loadCalendar()
        }
        
      } catch (error) {
        console.error('예약 상태 변경 실패:', error)
        const errorMsg = error?.response?.data?.error || error?.message || '예약 상태 변경에 실패했습니다.'
        alert(`❌ ${errorMsg}`)
      } finally {
        updatingStatus.value = false
      }
    }

    // 컴포넌트 마운트 시 데이터 로드
    onMounted(() => {
      try {
        const q = new URLSearchParams(window.location.search)
        const userName = q.get('userName')
        if (userName) {
          filters.userName = userName
        }
        const anchor = q.get('anchorDate')
        if (anchor) {
          const ad = new Date(anchor)
          if (!isNaN(ad.getTime())) {
            calendarMonth.value = new Date(ad.getFullYear(), ad.getMonth(), 1)
          }
        }
        const noUserReservations = q.get('noUserReservations')
        if (noUserReservations === 'true') {
          setTimeout(() => alert('해당 사용자의 예약 내역이 없습니다.'), 0)
        }
      } catch {}
      searchReservations()
      loadHotels()
      loadCalendar()
    })

    return {
      reservations,
      sort,
      selectedReservation,
      showDetailModal,
      viewMode,
      filters,
      pagination,
      totalReservations,
      confirmedReservations,
      pendingReservations,
      cancelledReservations,
      paidReservations,
      searchReservations,
      changePage,
  sortBy,
  sortGroupClass,
      resetFilters,
      viewReservationDetail,
      closeDetailModal,
      loadCalendar,
      calendarMonth,
      calendarDays,
      calendarLoading,
      calendarError,
      calendarStatus,
      calendarHotelId,
      calendarHotels,
      formatMonth,
      changeMonth,
      openDayModal,
      closeDayModal,
      showDayModal,
      selectedDay,
      getReservationStatusText,
      getReservationStatusClass,
      getPaymentStatusText,
      getPaymentStatusClass,
      getPaymentMethodText,
      formatDate,
      formatDateTime,
      formatCurrency,
      reservationTagType,
      paymentTagType,
      updateReservationStatus,
      updatingStatus
    }
  }
}
</script>

<style scoped src="@/assets/css/admin/reservation-monitoring.css"></style>