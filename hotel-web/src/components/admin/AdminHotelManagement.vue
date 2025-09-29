<template>
  <div class="hotel-management">
    <div class="page-header">
      <h1>호텔 및 숙소 관리</h1>
      <p class="page-description">전체 호텔 현황을 관리하고 승인/거부 처리를 할 수 있습니다.</p>
    </div>

    <!-- 통계 카드 -->
    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-icon"></div>
        <div class="stat-content">
          <div class="stat-number">{{ stats.totalHotels }}</div>
          <div class="stat-label">전체 호텔</div>
        </div>
      </div>
      <div class="stat-card pending">
        <div class="stat-icon"></div>
        <div class="stat-content">
          <div class="stat-number">{{ stats.pendingHotels }}</div>
          <div class="stat-label">승인 대기</div>
        </div>
      </div>
      <div class="stat-card approved">
        <div class="stat-icon"></div>
        <div class="stat-content">
          <div class="stat-number">{{ stats.approvedHotels }}</div>
          <div class="stat-label">승인 완료</div>
        </div>
      </div>
      <div class="stat-card revenue">
        <div class="stat-icon"></div>
        <div class="stat-content">
          <div class="stat-number">{{ formatCurrency(stats.totalRevenue) }}</div>
          <div class="stat-label">총 매출</div>
        </div>
      </div>
    </div>

    <!-- 필터 및 검색 -->
    <div class="filters">
      <div class="filter-group">
        <input
          v-model="filters.name"
          type="text"
          placeholder="호텔명 검색..."
          class="search-input"
          @input="debounceSearch"
        />
        <select v-model="filters.status" @change="loadHotels" class="status-select">
          <option value="">모든 상태</option>
          <option value="PENDING">승인 대기</option>
          <option value="APPROVED">승인 완료</option>
          <option value="REJECTED">승인 거부</option>
          <option value="SUSPENDED">정지</option>
        </select>
        <input
          v-model="filters.city"
          type="text"
          placeholder="도시 검색..."
          class="search-input"
          @input="debounceSearch"
        />
      </div>
    </div>

    <!-- 호텔 목록 테이블 -->
    <div class="table-container">
      <table class="hotels-table">
        <thead>
          <tr>
            <th>호텔명</th>
            <th>사업자</th>
            <th>위치</th>
            <th>객실 수</th>
            <th>예약 수</th>
            <th>평점</th>
            <th>매출</th>
            <th>상태</th>
            <th>등록일</th>
            <th>작업</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="hotel in hotels.content" :key="hotel.id" :class="`status-${hotel.status.toLowerCase()}`">
            <td>
              <div class="hotel-info">
                <strong>{{ hotel.name }}</strong>
                <div class="hotel-address">{{ hotel.address }}</div>
              </div>
            </td>
            <td>
              <div class="business-info">
                <div>{{ hotel.businessName }}</div>
                <div class="business-email">{{ hotel.businessEmail }}</div>
              </div>
            </td>
            <td>{{ hotel.city }}</td>
            <td>{{ hotel.roomCount }}</td>
            <td>{{ hotel.reservationCount }}</td>
            <td>
              <div class="rating">{{ hotel.averageRating ? hotel.averageRating.toFixed(1) : '0.0' }}</div>
            </td>
            <td>{{ formatCurrency(hotel.totalRevenue) }}</td>
            <td>
              <span :class="`status-badge status-${hotel.status.toLowerCase()}`">
                {{ getStatusText(hotel.status) }}
              </span>
            </td>
            <td>{{ formatDate(hotel.createdAt) }}</td>
            <td>
              <div class="action-buttons">
                <button @click="viewHotelDetail(hotel)" class="btn-detail">상세</button>
                <button 
                  v-if="hotel.status === 'PENDING'" 
                  @click="updateHotelStatus(hotel.id, 'APPROVED')" 
                  class="btn-approve"
                >
                  승인
                </button>
                <button 
                  v-if="hotel.status === 'PENDING'" 
                  @click="showRejectModal(hotel)" 
                  class="btn-reject"
                >
                  거부
                </button>
                <button 
                  v-if="hotel.status === 'APPROVED'" 
                  @click="showSuspendModal(hotel)" 
                  class="btn-suspend"
                >
                  정지
                </button>
                <button 
                  v-if="hotel.status === 'SUSPENDED'" 
                  @click="updateHotelStatus(hotel.id, 'APPROVED')" 
                  class="btn-approve"
                >
                  해제
                </button>
                <button @click="deleteHotel(hotel.id)" class="btn-delete">삭제</button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 페이지네이션 -->
    <div class="pagination">
      <button 
        @click="changePage(hotels.number - 1)" 
        :disabled="hotels.first"
        class="page-btn"
      >
        이전
      </button>
      <span class="page-info">
        {{ hotels.number + 1 }} / {{ hotels.totalPages }}
      </span>
      <button 
        @click="changePage(hotels.number + 1)" 
        :disabled="hotels.last"
        class="page-btn"
      >
        다음
      </button>
    </div>

    <!-- 호텔 상세 모달 -->
    <div v-if="showDetailModal" class="modal-overlay" @click="closeDetailModal">
      <div class="modal-content large" @click.stop>
        <div class="modal-header">
          <h2>{{ selectedHotel.name }} 상세 정보</h2>
          <button @click="closeDetailModal" class="close-btn">&times;</button>
        </div>
        <div class="modal-body">
          <div class="hotel-detail">
            <div class="detail-section">
              <h3>기본 정보</h3>
              <div class="detail-grid">
                <div class="detail-item">
                  <label>호텔명:</label>
                  <span>{{ selectedHotel.name }}</span>
                </div>
                <div class="detail-item">
                  <label>설명:</label>
                  <span>{{ selectedHotel.description }}</span>
                </div>
                <div class="detail-item">
                  <label>주소:</label>
                  <span>{{ selectedHotel.address }}</span>
                </div>
                <div class="detail-item">
                  <label>전화번호:</label>
                  <span>{{ selectedHotel.phone }}</span>
                </div>
                <div class="detail-item">
                  <label>이메일:</label>
                  <span>{{ selectedHotel.email }}</span>
                </div>
              </div>
            </div>

            <div class="detail-section">
              <h3>사업자 정보</h3>
              <div class="detail-grid">
                <div class="detail-item">
                  <label>사업자명:</label>
                  <span>{{ selectedHotel.businessName }}</span>
                </div>
                <div class="detail-item">
                  <label>사업자 이메일:</label>
                  <span>{{ selectedHotel.businessEmail }}</span>
                </div>
              </div>
            </div>

            <div class="detail-section">
              <h3>운영 현황</h3>
              <div class="detail-grid">
                <div class="detail-item">
                  <label>총 객실 수:</label>
                  <span>{{ selectedHotel.roomCount }}개</span>
                </div>
                <div class="detail-item">
                  <label>총 예약 수:</label>
                  <span>{{ selectedHotel.reservationCount }}건</span>
                </div>
                <div class="detail-item">
                  <label>평균 평점:</label>
                  <span>{{ selectedHotel.averageRating ? selectedHotel.averageRating.toFixed(1) : '0.0' }}</span>
                </div>
                <div class="detail-item">
                  <label>총 매출:</label>
                  <span>{{ formatCurrency(selectedHotel.totalRevenue) }}</span>
                </div>
              </div>
            </div>

            <div v-if="selectedHotel.rooms && selectedHotel.rooms.length > 0" class="detail-section">
              <h3>객실 목록</h3>
              <div class="rooms-list">
                <div v-for="room in selectedHotel.rooms" :key="room.id" class="room-item">
                  <div class="room-info">
                    <strong>{{ room.name || room.roomType || '객실명 없음' }}</strong>
                    <span class="room-status">{{ room.status || 'AVAILABLE' }}</span>
                  </div>
                  <div class="room-details">
                    <span>수용인원: {{ room.capacity || room.maxPeople || 0 }}명</span>
                    <span>기본가격: {{ formatCurrency(room.basePrice || room.price || 0) }}</span>
                  </div>
                </div>
              </div>
            </div>
            
            <div v-else-if="selectedHotel.roomCount > 0" class="detail-section">
              <h3>객실 목록</h3>
              <p class="no-room-details">총 {{ selectedHotel.roomCount }}개의 객실이 있습니다. (상세 정보는 별도 관리 메뉴에서 확인 가능)</p>
            </div>

            <div v-if="selectedHotel.rejectionReason" class="detail-section rejection-reason">
              <h3>거부/정지 사유</h3>
              <p>{{ selectedHotel.rejectionReason }}</p>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 거부 사유 입력 모달 -->
    <div v-if="showRejectReasonModal" class="modal-overlay" @click="closeRejectModal">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h2>호텔 승인 거부</h2>
          <button @click="closeRejectModal" class="close-btn">&times;</button>
        </div>
        <div class="modal-body">
          <p><strong>{{ rejectTargetHotel.name }}</strong> 호텔의 승인을 거부하시겠습니까?</p>
          <div class="form-group">
            <label>거부 사유:</label>
            <textarea 
              v-model="rejectReason" 
              placeholder="거부 사유를 입력해주세요..."
              class="reason-textarea"
            ></textarea>
          </div>
        </div>
        <div class="modal-footer">
          <button @click="closeRejectModal" class="btn-cancel">취소</button>
          <button @click="confirmReject" class="btn-reject">거부</button>
        </div>
      </div>
    </div>

    <!-- 정지 사유 입력 모달 -->
    <div v-if="showSuspendReasonModal" class="modal-overlay" @click="closeSuspendModal">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h2>호텔 운영 정지</h2>
          <button @click="closeSuspendModal" class="close-btn">&times;</button>
        </div>
        <div class="modal-body">
          <p><strong>{{ suspendTargetHotel.name }}</strong> 호텔의 운영을 정지하시겠습니까?</p>
          <div class="form-group">
            <label>정지 사유:</label>
            <textarea 
              v-model="suspendReason" 
              placeholder="정지 사유를 입력해주세요..."
              class="reason-textarea"
            ></textarea>
          </div>
        </div>
        <div class="modal-footer">
          <button @click="closeSuspendModal" class="btn-cancel">취소</button>
          <button @click="confirmSuspend" class="btn-suspend">정지</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue'
import axios from '@/api/http.js'

export default {
  name: 'AdminHotelManagement',
  setup() {
    const hotels = ref({
      content: [],
      totalPages: 0,
      totalElements: 0,
      number: 0,
      first: true,
      last: true
    })

    const stats = reactive({
      totalHotels: 0,
      pendingHotels: 0,
      approvedHotels: 0,
      totalRevenue: 0
    })

    const filters = reactive({
      name: '',
      status: '',
      city: ''
    })

    const selectedHotel = ref({})
    const showDetailModal = ref(false)
    const showRejectReasonModal = ref(false)
    const showSuspendReasonModal = ref(false)
    const rejectTargetHotel = ref({})
    const suspendTargetHotel = ref({})
    const rejectReason = ref('')
    const suspendReason = ref('')

    let searchTimeout = null

    const loadHotels = async (page = 0) => {
      try {
        const params = {
          page,
          size: 10,
          sort: 'createdAt,desc'
        }

        if (filters.name) params.name = filters.name
        if (filters.status) params.status = filters.status
        if (filters.city) params.city = filters.city

        const response = await axios.get('/admin/hotels', { params })
        hotels.value = response.data?.data || {
          content: [],
          totalPages: 0,
          totalElements: 0,
          number: 0,
          first: true,
          last: true
        }
        
        updateStats()
      } catch (error) {
        alert('호텔 목록을 불러오는데 실패했습니다.')
      }
    }

    const updateStats = () => {
      const content = hotels.value.content
      stats.totalHotels = content.length
      stats.pendingHotels = content.filter(h => h.status === 'PENDING').length
      stats.approvedHotels = content.filter(h => h.status === 'APPROVED').length
      stats.totalRevenue = content.reduce((sum, h) => sum + (h.totalRevenue || 0), 0)
    }

    const debounceSearch = () => {
      clearTimeout(searchTimeout)
      searchTimeout = setTimeout(() => {
        loadHotels(0)
      }, 500)
    }

    const changePage = (page) => {
      loadHotels(page)
    }

    const viewHotelDetail = async (hotel) => {
      try {
        const response = await axios.get(`/admin/hotels/${hotel.id}`)
        const hotelData = response.data?.data || {}
        
        // API 응답 데이터 구조 확인 및 안전한 할당
        selectedHotel.value = {
          ...hotelData,
          // 운영현황 데이터 명시적 매핑
          roomCount: hotelData.roomCount || 0,
          reservationCount: hotelData.reservationCount || 0,
          averageRating: hotelData.averageRating || 0.0,
          totalRevenue: hotelData.totalRevenue || 0,
          // 기본 정보 백업
          name: hotelData.name || hotel.name,
          address: hotelData.address || hotel.address,
          description: hotelData.description || hotel.description,
          businessName: hotelData.businessName || hotel.businessName,
          businessEmail: hotelData.businessEmail || hotel.businessEmail,
          phone: hotelData.phone || hotel.phone,
          status: hotelData.status || hotel.status,
          rejectionReason: hotelData.rejectionReason || hotel.rejectionReason
        }
        
        showDetailModal.value = true
      } catch (error) {
        console.error('호텔 상세 정보 로드 실패:', error)
        alert(`호텔 상세 정보를 불러오는데 실패했습니다. ${error.response?.data?.error || error.message || ''}`)
      }
    }

    const updateHotelStatus = async (hotelId, status, reason = '') => {
      try {
  await axios.put(`/admin/hotels/${hotelId}/status`, {
          status,
          reason
        })
        
        alert(`호텔 상태가 ${getStatusText(status)}(으)로 변경되었습니다.`)
        loadHotels(hotels.value.number)
      } catch (error) {
        alert('호텔 상태 변경에 실패했습니다.')
      }
    }

    const showRejectModal = (hotel) => {
      rejectTargetHotel.value = hotel
      rejectReason.value = ''
      showRejectReasonModal.value = true
    }

    const closeRejectModal = () => {
      showRejectReasonModal.value = false
      rejectTargetHotel.value = {}
      rejectReason.value = ''
    }

    const confirmReject = () => {
      if (!rejectReason.value.trim()) {
        alert('거부 사유를 입력해주세요.')
        return
      }
      
      updateHotelStatus(rejectTargetHotel.value.id, 'REJECTED', rejectReason.value)
      closeRejectModal()
    }

    const showSuspendModal = (hotel) => {
      suspendTargetHotel.value = hotel
      suspendReason.value = ''
      showSuspendReasonModal.value = true
    }

    const closeSuspendModal = () => {
      showSuspendReasonModal.value = false
      suspendTargetHotel.value = {}
      suspendReason.value = ''
    }

    const confirmSuspend = () => {
      if (!suspendReason.value.trim()) {
        alert('정지 사유를 입력해주세요.')
        return
      }
      
      updateHotelStatus(suspendTargetHotel.value.id, 'SUSPENDED', suspendReason.value)
      closeSuspendModal()
    }

    const closeDetailModal = () => {
      showDetailModal.value = false
      selectedHotel.value = {}
    }

    const deleteHotel = async (hotelId) => {
      if (!confirm('정말로 이 호텔을 삭제하시겠습니까? 이 작업은 되돌릴 수 없습니다.')) {
        return
      }

      try {
  await axios.delete(`/admin/hotels/${hotelId}`)
        alert('호텔이 삭제되었습니다.')
        loadHotels(hotels.value.number)
      } catch (error) {
        alert('호텔 삭제에 실패했습니다.')
      }
    }

    const getStatusText = (status) => {
      const statusMap = {
        'PENDING': '승인 대기',
        'APPROVED': '승인 완료',
        'REJECTED': '승인 거부',
        'SUSPENDED': '운영 정지'
      }
      return statusMap[status] || status
    }

    const formatDate = (dateString) => {
      return new Date(dateString).toLocaleDateString('ko-KR')
    }

    const formatCurrency = (amount) => {
      if (!amount) return '₩0'
      return `₩${amount.toLocaleString()}`
    }

    onMounted(() => {
      loadHotels()
    })

    return {
      hotels,
      stats,
      filters,
      selectedHotel,
      showDetailModal,
      showRejectReasonModal,
      showSuspendReasonModal,
      rejectTargetHotel,
      suspendTargetHotel,
      rejectReason,
      suspendReason,
      loadHotels,
      debounceSearch,
      changePage,
      viewHotelDetail,
      updateHotelStatus,
      showRejectModal,
      closeRejectModal,
      confirmReject,
      showSuspendModal,
      closeSuspendModal,
      confirmSuspend,
      closeDetailModal,
      deleteHotel,
      getStatusText,
      formatDate,
      formatCurrency
    }
  }
}
</script>

<style scoped src="@/assets/css/admin/admin-hotel-management.css"></style>