<template>
  <div class="business-management">
    <div class="page-header">
      <h1>사업자 승인 관리</h1>
      <p class="page-description">사업자 등록 신청을 검토하고 승인/반려 처리를 할 수 있습니다.</p>
    </div>

    <!-- 상태별 요약 카드 -->
    <div class="summary-cards">
      <div class="card pending">
        <div class="card-icon"></div>
        <div class="card-content">
          <h3>승인 대기</h3>
          <p class="card-number">{{ statusCounts.PENDING }}</p>
        </div>
      </div>

      <div class="card approved">
        <div class="card-icon">✅</div>
        <div class="card-content">
          <h3>승인 완료</h3>
          <p class="card-number">{{ statusCounts.APPROVED }}</p>
        </div>
      </div>

      <div class="card rejected">
        <div class="card-icon">❌</div>
        <div class="card-content">
          <h3>반려</h3>
          <p class="card-number">{{ statusCounts.REJECTED }}</p>
        </div>
      </div>

      <div class="card suspended">
        <div class="card-icon">🚫</div>
        <div class="card-content">
          <h3>정지</h3>
          <p class="card-number">{{ statusCounts.SUSPENDED }}</p>
        </div>
      </div>
    </div>

    <!-- 필터 섹션 -->
    <div class="filter-section">
      <div class="filter-form">
        <div class="filter-group">
          <label>상태 필터</label>
          <select v-model="selectedStatus" @change="onStatusChange" class="filter-select">
            <option value="">전체</option>
            <option value="PENDING">승인 대기</option>
            <option value="APPROVED">승인 완료</option>
            <option value="REJECTED">반려</option>
            <option value="SUSPENDED">정지</option>
          </select>
        </div>
        
        <div class="filter-buttons">
          <button @click="resetFilters" class="btn btn-secondary">필터 초기화</button>
        </div>
      </div>
    </div>

    <!-- 사업자 목록 테이블 -->
    <div class="table-section">
      <div class="table-header">
        <h2>사업자 목록</h2>
        <span class="total-count">총 {{ totalElements }}개</span>
      </div>

      <div v-if="loading" class="loading">
        <p>데이터를 불러오는 중...</p>
      </div>

      <div v-else-if="businesses.length === 0" class="no-data">
        <p>등록된 사업자가 없습니다.</p>
      </div>

      <table v-else class="business-table">
        <thead>
          <tr>
            <th>
              ID
              <button class="sort-btn" @click.stop="sortBy('id')" style="background:none;border:none;padding:0;margin-left:4px;vertical-align:middle;">
                <svg width="18" height="23" viewBox="0 0 12 16" xmlns="http://www.w3.org/2000/svg" style="display:inline-block;vertical-align:middle;">
                  <polygon points="6,2 2,6 10,6" :fill="sort.prop==='id' && sort.order==='ascending' ? '#222' : '#ccc'" />
                  <polygon points="6,14 2,10 10,10" :fill="sort.prop==='id' && sort.order==='descending' ? '#222' : '#ccc'" />
                </svg>
              </button>
            </th>
            <th>
              상태
              <button class="sort-btn" @click.stop="sortBy('status')" style="background:none;border:none;padding:0;margin-left:4px;vertical-align:middle;">
                <svg width="18" height="23" viewBox="0 0 12 16" xmlns="http://www.w3.org/2000/svg" style="display:inline-block;vertical-align:middle;">
                  <polygon points="6,2 2,6 10,6" :fill="sort.prop==='status' && sort.order==='ascending' ? '#222' : '#ccc'" />
                  <polygon points="6,14 2,10 10,10" :fill="sort.prop==='status' && sort.order==='descending' ? '#222' : '#ccc'" />
                </svg>
              </button>
            </th>
            <th>
              사업자명
              <button class="sort-btn" @click.stop="sortBy('businessName')" style="background:none;border:none;padding:0;margin-left:4px;vertical-align:middle;">
                <svg width="18" height="23" viewBox="0 0 12 16" xmlns="http://www.w3.org/2000/svg" style="display:inline-block;vertical-align:middle;">
                  <polygon points="6,2 2,6 10,6" :fill="sort.prop==='businessName' && sort.order==='ascending' ? '#222' : '#ccc'" />
                  <polygon points="6,14 2,10 10,10" :fill="sort.prop==='businessName' && sort.order==='descending' ? '#222' : '#ccc'" />
                </svg>
              </button>
            </th>
            <th>
              사업자번호
              <button class="sort-btn" @click.stop="sortBy('businessNumber')" style="background:none;border:none;padding:0;margin-left:4px;vertical-align:middle;">
                <svg width="18" height="23" viewBox="0 0 12 16" xmlns="http://www.w3.org/2000/svg" style="display:inline-block;vertical-align:middle;">
                  <polygon points="6,2 2,6 10,6" :fill="sort.prop==='businessNumber' && sort.order==='ascending' ? '#222' : '#ccc'" />
                  <polygon points="6,14 2,10 10,10" :fill="sort.prop==='businessNumber' && sort.order==='descending' ? '#222' : '#ccc'" />
                </svg>
              </button>
            </th>
            <th>
              연락처
              <button class="sort-btn" @click.stop="sortBy('businessPhone')" style="background:none;border:none;padding:0;margin-left:4px;vertical-align:middle;">
                <svg width="18" height="23" viewBox="0 0 12 16" xmlns="http://www.w3.org/2000/svg" style="display:inline-block;vertical-align:middle;">
                  <polygon points="6,2 2,6 10,6" :fill="sort.prop==='businessPhone' && sort.order==='ascending' ? '#222' : '#ccc'" />
                  <polygon points="6,14 2,10 10,10" :fill="sort.prop==='businessPhone' && sort.order==='descending' ? '#222' : '#ccc'" />
                </svg>
              </button>
            </th>
            <th>
              주소
              <button class="sort-btn" @click.stop="sortBy('address')" style="background:none;border:none;padding:0;margin-left:4px;vertical-align:middle;">
                <svg width="18" height="23" viewBox="0 0 12 16" xmlns="http://www.w3.org/2000/svg" style="display:inline-block;vertical-align:middle;">
                  <polygon points="6,2 2,6 10,6" :fill="sort.prop==='address' && sort.order==='ascending' ? '#222' : '#ccc'" />
                  <polygon points="6,14 2,10 10,10" :fill="sort.prop==='address' && sort.order==='descending' ? '#222' : '#ccc'" />
                </svg>
              </button>
            </th>
            <th>
              신청일
              <button class="sort-btn" @click.stop="sortBy('createdAt')" style="background:none;border:none;padding:0;margin-left:4px;vertical-align:middle;">
                <svg width="18" height="23" viewBox="0 0 12 16" xmlns="http://www.w3.org/2000/svg" style="display:inline-block;vertical-align:middle;">
                  <polygon points="6,2 2,6 10,6" :fill="sort.prop==='createdAt' && sort.order==='ascending' ? '#222' : '#ccc'" />
                  <polygon points="6,14 2,10 10,10" :fill="sort.prop==='createdAt' && sort.order==='descending' ? '#222' : '#ccc'" />
                </svg>
              </button>
            </th>
            <th>관리</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="business in businesses" :key="business.id" :class="getRowClass(business.status)">
            <td>{{ business.id }}</td>
            <td>
              <span :class="['status-badge', 'status-' + business.status.toLowerCase()]">
                {{ getStatusLabel(business.status) }}
              </span>
            </td>
            <td>
              <div class="business-info">
                <strong>{{ business.businessName }}</strong>
                <small class="business-user">{{ business.businessName }} ({{ business.businessEmail }})</small>
              </div>
            </td>
            <td>{{ formatBusinessNumber(business.businessNumber) }}</td>
            <td>{{ formatPhoneNumber(business.businessPhone) }}</td>
            <td>{{ business.address }}</td>
            <td>{{ formatDate(business.createdAt) }}</td>
            <td>
              <div class="action-buttons">
                <button
                  v-if="business.status === 'PENDING'"
                  @click="updateStatus(business, 'APPROVED')"
                  class="btn btn-approve"
                  title="승인"
                >승인</button>
                
                <button
                  v-if="business.status === 'PENDING'"
                  @click="updateStatus(business, 'REJECTED')"
                  class="btn btn-reject"
                  title="반려"
                >반려</button>
                
                <button
                  v-if="business.status === 'APPROVED'"
                  @click="updateStatus(business, 'SUSPENDED')"
                  class="btn btn-suspend"
                  title="정지"
                >정지</button>
                
                <button
                  v-if="business.status === 'SUSPENDED'"
                  @click="updateStatus(business, 'APPROVED')"
                  class="btn btn-activate"
                  title="활성화"
                >활성화</button>
                
                <button
                  @click="viewDetails(business)"
                  class="btn btn-info"
                  title="상세보기"
                >상세</button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 페이지네이션 -->
    <div class="pagination-section" v-if="totalPages > 1">
      <div class="pagination">
        <button
          @click="changePage(0)"
          :disabled="currentPage === 0"
          class="page-btn"
        >
          ⏮️ 처음
        </button>
        
        <button
          @click="changePage(currentPage - 1)"
          :disabled="currentPage === 0"
          class="page-btn"
        >
          ⬅️ 이전
        </button>
        
        <span class="page-info">
          {{ currentPage + 1 }} / {{ totalPages }} 페이지
        </span>
        
        <button
          @click="changePage(currentPage + 1)"
          :disabled="currentPage >= totalPages - 1"
          class="page-btn"
        >
          다음 ➡️
        </button>
        
        <button
          @click="changePage(totalPages - 1)"
          :disabled="currentPage >= totalPages - 1"
          class="page-btn"
        >
          마지막 ⏭️
        </button>
      </div>
    </div>

    <!-- 상세 정보 모달 -->
    <div v-if="selectedBusiness" class="modal-overlay" @click="closeModal">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>사업자 상세 정보</h3>
          <button @click="closeModal" class="close-btn">✕</button>
        </div>
        
        <div class="modal-body">
          <div class="detail-section">
            <h4>기본 정보</h4>
            <div class="detail-grid">
              <div class="detail-item">
                <label>사업자명:</label>
                <span>{{ selectedBusiness.businessName }}</span>
              </div>
              <div class="detail-item">
                <label>사업자번호:</label>
                <span>{{ formatBusinessNumber(selectedBusiness.businessNumber) }}</span>
              </div>
              <div class="detail-item">
                <label>사용자 정보:</label>
                <span>{{ selectedBusiness.businessName }} ({{ selectedBusiness.businessEmail }})</span>
              </div>
              <div class="detail-item">
                <label>연락처:</label>
                <span>{{ formatPhoneNumber(selectedBusiness.businessPhone) }}</span>
              </div>
              <div class="detail-item">
                <label>주소:</label>
                <span>{{ selectedBusiness.address }}</span>
              </div>
              <div class="detail-item">
                <label>현재 상태:</label>
                <span :class="['status-badge', 'status-' + selectedBusiness.status.toLowerCase()]">
                  {{ getStatusLabel(selectedBusiness.status) }}
                </span>
              </div>
            </div>
          </div>
          
          <div class="detail-section">
            <h4>신청 정보</h4>
            <div class="detail-grid">
              <div class="detail-item">
                <label>신청일:</label>
                <span>{{ formatDate(selectedBusiness.createdAt) }}</span>
              </div>
              <div class="detail-item">
                <label>최종 수정일:</label>
                <span>{{ formatDate(getLastUpdated(selectedBusiness)) }}</span>
              </div>
            </div>
          </div>
        </div>
        
        <div class="modal-footer">
          <button @click="closeModal" class="btn btn-secondary">닫기</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import http from '@/api/http'
export default {
  name: 'HotelManagement',
  data() {
    return {
      businesses: [],
      loading: false,
      selectedStatus: '',
      currentPage: 0,
      totalPages: 0,
      totalElements: 0,
      pageSize: 20,
      selectedBusiness: null,
      sort: { prop: 'createdAt', order: 'descending' },

      statusCounts: {
        PENDING: 0,
        APPROVED: 0,
        REJECTED: 0,
        SUSPENDED: 0
      }
    }
  },
  mounted() {
    this.ensureAdminAndLoad();
  },
  methods: {
    async ensureAdminAndLoad() {
      try {
        const resp = await http.get('/user/info');
        const role = resp?.data?.role;
        if (role !== 'ADMIN') {
          alert('관리자 권한이 필요합니다.');
          this.$router.push('/');
          return;
        }
        await this.loadBusinesses();
        await this.loadStatusCounts();
      } catch (e) {
        if (e?.response?.status === 401) {
          alert('로그인이 만료되었습니다. 다시 로그인해주세요.');
          this.$router.push('/login');
        } else {
          alert('접근 중 오류가 발생했습니다.');
          this.$router.push('/');
        }
      }
    },
    onStatusChange() {
      this.currentPage = 0
      this.loadBusinesses()
    },
    async loadBusinesses() {
      this.loading = true;
      try {
        const params = {
          page: this.currentPage,
          size: this.pageSize
        }
        if (this.selectedStatus) params.status = this.selectedStatus
        const s = this.toApiSort(this.sort)
        if (s) params.sort = s
        console.log('API 요청 파라미터:', params)

        const resp = await http.get('/admin/hotels', { params })
        const page = resp?.data?.data || {}

        this.businesses = page.content || []
        this.totalPages = page.totalPages || 0
        this.totalElements = page.totalElements || 0
        this.currentPage = (page.page ?? page.number ?? 0)
      } catch (error) {
  // 오류는 알림으로만 처리
        let msg = '사업자 목록을 불러오는데 실패했습니다.'
        if (error?.response?.status === 401) {
          msg = '로그인이 만료되었습니다. 다시 로그인해주세요.'
        } else if (error?.response?.status === 403) {
          msg = '관리자 권한이 필요합니다.'
        } else if (error?.response?.data?.error) {
          msg = error.response.data.error
        } else if (error?.message) {
          msg = error.message
        }
        alert(msg)
      } finally {
        this.loading = false;
      }
    },
    sortBy(prop) {
      console.log('정렬 클릭:', prop, '현재 정렬:', this.sort)
      if (this.sort.prop === prop) {
        this.sort.order = this.sort.order === 'ascending' ? 'descending' : 'ascending'
      } else {
        this.sort = { prop, order: 'ascending' }
      }
      console.log('새 정렬:', this.sort)
      this.currentPage = 0
      this.loadBusinesses()
    },
    toApiSort({ prop, order }) {
      if (!prop || !order) return null
      const dir = order === 'ascending' ? 'asc' : 'desc'
      const allowed = new Set(['id','status','businessName','businessNumber','businessPhone','address','createdAt'])
      if (!allowed.has(prop)) return null
      return `${prop},${dir}`
    },
    
    async loadStatusCounts() {
      try {
        const res = await http.get('/admin/hotels/status-counts')
        const data = res?.data?.data || {}
        this.statusCounts = {
          PENDING: Number(data.PENDING ?? 0),
          APPROVED: Number(data.APPROVED ?? 0),
          REJECTED: Number(data.REJECTED ?? 0),
          SUSPENDED: Number(data.SUSPENDED ?? 0)
        }
      } catch (e) {
        console.error('상태 카운트 로딩 실패', e)
      }
    },
    
    async updateStatus(business, newStatus) {
      const statusMessages = {
        'APPROVED': '승인',
        'REJECTED': '반려',
        'SUSPENDED': '정지'
      };
      
      const confirmMessage = `${business.businessName}를 "${statusMessages[newStatus]}" 처리하시겠습니까?`;
      if (!confirm(confirmMessage)) {
        return;
      }
      
      try {
        // 사전 인증 확인
        try {
          const who = await http.get('/user/info')
          if (who?.data?.role !== 'ADMIN') {
            alert('관리자 권한이 필요합니다. 다시 로그인해주세요.')
            this.$router.push('/login')
            return
          }
        } catch (e) {
          if (e?.response?.status === 401) {
            alert('로그인이 만료되었습니다. 다시 로그인해주세요.')
            this.$router.push('/login')
            return
          }
        }

        await http.put(`/admin/hotels/${business.id}/status`, { status: newStatus })

        // 로컬 데이터 업데이트
        business.status = newStatus

        alert(`${business.businessName}가 성공적으로 ${statusMessages[newStatus]} 처리되었습니다.`)

        // 카운트 다시 로드
        this.loadStatusCounts()
      } catch (error) {
        const msg = error?.response?.data?.error || error?.message || '상태 변경에 실패했습니다.'
        alert(msg)
      }
    },
    
    viewDetails(business) {
      this.selectedBusiness = business;
    },
    
    closeModal() {
      this.selectedBusiness = null;
    },
    
    resetFilters() {
      this.selectedStatus = '';
      this.currentPage = 0;
      this.loadBusinesses();
    },
    
    changePage(page) {
      if (page >= 0 && page < this.totalPages) {
        this.currentPage = page;
        this.loadBusinesses();
      }
    },
    
    getStatusLabel(status) {
      const statusLabels = {
        'PENDING': '승인 대기',
        'APPROVED': '승인 완료',
        'REJECTED': '반려',
        'SUSPENDED': '정지'
      };
      return statusLabels[status] || status;
    },
    
    getRowClass(status) {
      return status === 'PENDING' ? 'pending-row' : '';
    },
    
    formatDate(dateString) {
      if (!dateString) return '-'
      const date = new Date(dateString)
      if (isNaN(date.getTime())) return '-'
      return date.toLocaleDateString('ko-KR', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
      });
    },

    getLastUpdated(business) {
      if (!business) return null
      // 승인/정지/반려 등 상태 변경 시각이 최종 수정 개념에 더 가까움
      return business.approvalDate || business.createdAt || null
    },
    
    formatBusinessNumber(businessNumber) {
      if (!businessNumber) return '-';
      
      // 숫자만 추출
      const numbers = businessNumber.toString().replace(/\D/g, '');
      
      // 10자리 숫자인 경우 000-00-0000 형식으로 포맷
      if (numbers.length === 10) {
        return `${numbers.substring(0, 3)}-${numbers.substring(3, 5)}-${numbers.substring(5)}`;
      }
      
      // 다른 형식이면 그대로 반환
      return businessNumber;
    },
    
    formatPhoneNumber(phoneNumber) {
      if (!phoneNumber) return '-';
      
      // 숫자만 추출
      const numbers = phoneNumber.toString().replace(/\D/g, '');
      
      // 11자리 숫자인 경우 000-0000-0000 형식으로 포맷 (010-1234-5678)
      if (numbers.length === 11) {
        return `${numbers.substring(0, 3)}-${numbers.substring(3, 7)}-${numbers.substring(7)}`;
      }
      
      // 10자리 숫자인 경우 000-000-0000 형식으로 포맷 (02-123-4567)
      if (numbers.length === 10) {
        return `${numbers.substring(0, 3)}-${numbers.substring(3, 6)}-${numbers.substring(6)}`;
      }
      
      // 다른 형식이면 그대로 반환
      return phoneNumber;
    }
  }
}
</script>

<style scoped src="@/assets/css/admin/hotel-management.css"></style>