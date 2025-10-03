<template>
  <div class="user-management">
    <div class="page-header">
      <h1>사용자 관리</h1>
      <p class="page-description">전체 사용자를 조회하고 관계를 관리할 수 있습니다.</p>
      <div class="header-actions">
        <button class="btn btn-outline" @click="showAdvancedFilters = !showAdvancedFilters">
          고급 필터
        </button>
        <!-- 빠른 통계 버튼 제거 -->
      </div>
    </div>

    <!-- 빠른 통계 영역 제거 -->

    <!-- 고급 필터 -->
    <div v-if="showAdvancedFilters" class="advanced-filters">
      <div class="filter-header">
        <h3>고급 필터링</h3>
        <button class="btn btn-sm btn-secondary" @click="resetAdvancedFilters">전체 초기화</button>
      </div>
        <div class="filter-group" v-if="advancedFilters.activityStatus === 'DORMANT'">
          <label>휴면 기준</label>
          <select class="search-select" v-model.number="advancedFilters.dormantMonths">
            <option :value="6">최근 6개월 미접속</option>
            <option :value="12">최근 1년 미접속</option>
            <option :value="24">최근 2년 미접속</option>
          </select>
        </div>
      <div class="filter-grid">
        <div class="filter-group">
          <label>가입 기간</label>
          <select class="search-select" v-model="advancedFilters.joinPeriod">
            <option value="">전체</option>
            <option value="today">오늘</option>
            <option value="week">최근 1주</option>
            <option value="month">최근 1개월</option>
            <option value="quarter">최근 3개월</option>
            <option value="year">최근 1년</option>
          </select>
        </div>
        <div class="filter-group">
          <label>활동 상태</label>
          <select class="search-select" v-model="advancedFilters.activityStatus">
            <option value="">전체</option>
            <option value="ACTIVE">활성</option>
            <option value="DORMANT">휴면 (1년 이상 미접속)</option>
          </select>
        </div>
        <div class="filter-group">
          <label>예약 여부</label>
          <select class="search-select" v-model="advancedFilters.hasReservation">
            <option value="">전체</option>
            <option value="yes">예약 경험 있음</option>
            <option value="no">예약 경험 없음</option>
            <option value="frequent">단골 고객 (3회 이상)</option>
          </select>
        </div>
        <div class="filter-group">
          <label>가입 방식</label>
          <select class="search-select" v-model="advancedFilters.provider">
            <option value="">전체</option>
            <option value="LOCAL">이메일</option>
            <option value="GOOGLE">구글</option>
            <option value="NAVER">네이버</option>
            <option value="KAKAO">카카오</option>
          </select>
        </div>
      </div>
      <div class="filter-actions">
        <button class="btn btn-primary" @click="applyAdvancedFilters">필터 적용</button>
      </div>
    </div>

    <!-- 기본 검색 필터 -->
    <div class="search-section">
      <form class="search-form" @submit.prevent="searchUsers">
        <div class="search-group">
          <label>빠른 검색</label>
          <input class="search-input" v-model="searchForm.name" placeholder="이름으로 검색..." @keyup.enter="searchUsers" />
        </div>
        <div class="search-group">
          <label>이메일</label>
          <input class="search-input" v-model="searchForm.email" placeholder="이메일로 검색..." @keyup.enter="searchUsers" />
        </div>
        <div class="search-group">
          <label>역할</label>
          <select class="search-select" v-model="searchForm.role">
            <option value="">전체</option>
            <option value="USER">일반 사용자</option>
            <option value="BUSINESS">사업자</option>
            <option value="ADMIN">관리자</option>
          </select>
        </div>
        <div class="search-buttons">
          <button type="submit" class="btn btn-primary">
            검색
          </button>
          <button type="button" class="btn btn-secondary" @click="resetSearch">
            초기화
          </button>
        </div>
      </form>
    </div>

    <!-- 사용자 목록 및 요약 패널 -->
    <div class="main-content-area">
      <!-- 사용자 목록 -->
      <div class="table-section" :class="{ 'with-panel': selectedUser }">
        <div class="table-header">
          <h2>사용자 목록</h2>
          <div class="table-controls">
            <!-- 카드/표 보기 토글 버튼 제거 -->
            <div class="total-count">총 {{ totalElements }}명</div>
          </div>
        </div>

        <div v-if="loading" class="loading">불러오는 중…</div>
        <div v-else-if="!users || users.length === 0" class="no-data">검색 결과가 없습니다.</div>
        
        <!-- 테이블 뷰 -->
  <table v-else class="user-table">
          <thead>
            <tr>
              <th>
                사용자 정보
                <button class="sort-btn" @click.stop="sortBy('name')" style="background:none;border:none;padding:0;margin-left:4px;vertical-align:top;">
                  <svg width="12" height="24" viewBox="0 0 12 24" xmlns="http://www.w3.org/2000/svg" style="display:inline-block;vertical-align:top;">
                    <polygon points="6,2 2,8 10,8" :fill="sort.prop==='name' && sort.order==='ascending' ? '#222' : '#ccc'" />
                    <polygon points="6,22 2,16 10,16" :fill="sort.prop==='name' && sort.order==='descending' ? '#222' : '#ccc'" />
                  </svg>
                </button>
              </th>
              <th>역할 & 상태</th>
              <th>
                가입 정보
                <button class="sort-btn" @click.stop="sortBy('createdOn')" style="background:none;border:none;padding:0;margin-left:4px;vertical-align:top;">
                  <svg width="12" height="24" viewBox="0 0 12 24" xmlns="http://www.w3.org/2000/svg" style="display:inline-block;vertical-align:top;">
                    <polygon points="6,2 2,8 10,8" :fill="sort.prop==='createdOn' && sort.order==='ascending' ? '#222' : '#ccc'" />
                    <polygon points="6,22 2,16 10,16" :fill="sort.prop==='createdOn' && sort.order==='descending' ? '#222' : '#ccc'" />
                  </svg>
                </button>
              </th>
              <th>빠른 액션</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in users" :key="row.id" class="user-row" :class="{ active: selectedUser?.id === row.id }" @click="selectUser(row)">
              <td class="user-info">
                <div class="user-avatar">{{ row.name?.charAt(0) || '?' }}</div>
                <div class="user-details">
                  <div class="user-name">{{ row.name }}</div>
                  <div class="user-email">{{ row.email }}</div>
                  <div class="user-id">ID: {{ row.id }}</div>
                </div>
              </td>
              <td class="user-status">
                <div class="badge-group">
                  <span :class="['role-badge', roleBadgeClass(row.role)]">{{ getRoleLabel(row.role) }}</span>
                  <span :class="['status-badge', statusBadgeClass(row.status)]">{{ getStatusLabel(row.status) }}</span>
                </div>
                <div class="provider-info">
                  <span class="provider-badge">{{ getProviderLabel(row.provider) }}</span>
                </div>
              </td>
              <td class="user-join">
                <div class="join-date">{{ formatDate(row.createdOn) }}</div>
                <div class="join-period">
                  최근 활동: {{ getLastActivityText(row.lastLoginAt) }}
                  <span v-if="row.lastLoginAt"> ({{ formatDateCompact(row.lastLoginAt) }})</span>
                </div>
                <div class="join-period join-period-secondary">가입: {{ getJoinPeriod(row.createdOn) }}</div>
              </td>
              <td class="user-actions" @click.stop>
                <div class="quick-actions">
                  <button class="action-btn" @click="viewReservations(row)" title="예약 내역">
                    예약
                  </button>
                  <button class="action-btn" @click="openAssignCouponModal(row)" title="쿠폰 지급">
                    쿠폰
                  </button>
                  <select class="role-select-compact" :disabled="row.id === currentUserId" :value="row.role" @change="e => updateUserRole(row, e.target.value)">
                    <option value="USER">일반</option>
                    <option value="BUSINESS">사업</option>
                    <option value="ADMIN">관리</option>
                  </select>
                </div>
              </td>
            </tr>
          </tbody>
        </table>

        <!-- 카드 뷰 -->
        <!-- 카드 뷰 완전 제거 -->
      </div>

      <!-- 사용자 요약 패널 -->
      <div v-if="selectedUser && !showAssignCouponModal" class="user-summary-panel">
        <div class="panel-header">
          <div class="panel-title">
            <div class="panel-avatar">{{ selectedUser.name?.charAt(0) || '?' }}</div>
            <div class="panel-user-info">
              <h3>{{ selectedUser.name }}</h3>
              <p>{{ selectedUser.email }}</p>
            </div>
          </div>
          <button class="panel-close" @click="selectedUser = null">닫기</button>
        </div>
        
        <div class="panel-content">
          <div class="summary-section">
            <h4>주요 지표</h4>
            <div class="metrics-grid">
              <div class="metric-item">
                <div class="metric-value">{{ selectedUserStats.totalReservations }}</div>
                <div class="metric-label">총 예약</div>
              </div>
              <div class="metric-item">
                <div class="metric-value">{{ formatCurrency(selectedUserStats.totalPayment) }}</div>
                <div class="metric-label">총 결제</div>
              </div>
              <div class="metric-item">
                <div class="metric-value">{{ selectedUserStats.totalCoupons }}</div>
                <div class="metric-label">보유 쿠폰</div>
              </div>
              <div class="metric-item">
                <div class="metric-value">{{ selectedUserStats.lastActivity }}</div>
                <div class="metric-label">최근 활동</div>
              </div>
            </div>
          </div>

          <div class="summary-section">
            <h4>최근 예약</h4>
            <div class="recent-reservations">
              <div v-if="selectedUserStats.recentReservations.length === 0" class="no-data-small">
                예약 내역이 없습니다.
              </div>
              <div v-else v-for="reservation in selectedUserStats.recentReservations" :key="reservation.id" class="reservation-item" :class="'border-' + (reservation.status || '').toLowerCase()">
                <div class="reservation-content">
                  <div class="reservation-hotel">{{ reservation.hotelName }}</div>
                  <div class="reservation-date">{{ formatDate(reservation.checkIn) }} ~ {{ formatDate(reservation.checkOut) }}</div>
                  <div class="reservation-status" :class="'status-' + (reservation.status || '').toLowerCase()">
                    {{ formatReservationStatus(reservation.status) }}
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div class="summary-section">
            <h4>보유 쿠폰 <span style="font-size: 0.9em; color: #666;">({{ selectedUserStats.coupons?.length || 0 }}개)</span></h4>
            <div class="coupon-list">
              <div v-if="!selectedUserStats.coupons || selectedUserStats.coupons.length === 0" class="no-data-small">
                보유한 쿠폰이 없습니다.
              </div>
              <div v-else v-for="(coupon, index) in selectedUserStats.coupons" :key="coupon.id" class="coupon-item" :class="{ 'coupon-invalid': !coupon.isValid }">
                <div class="coupon-number">{{ index + 1 }}</div>
                <div class="coupon-content">
                  <div class="coupon-top">
                    <div class="coupon-info">
                      <div class="coupon-stat">
                        <span class="stat-label">할인:</span>
                        <span class="stat-value">{{ formatCouponDiscount(coupon) }}</span>
                      </div>
                      <div class="coupon-stat">
                        <span class="stat-label">최소:</span>
                        <span class="stat-value">{{ formatCurrency(coupon.minSpend) }}</span>
                      </div>
                    </div>
                    <span class="coupon-badge" :class="coupon.isValid ? 'badge-valid' : 'badge-invalid'">
                      <span v-if="coupon.isValid">사용<br>가능</span>
                      <span v-else>만료<br>비활성</span>
                    </span>
                  </div>
                  <div class="coupon-bottom">
                    <div class="coupon-validity-text">{{ formatCouponValidity(coupon) }}</div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div class="summary-section">
            <h4>빠른 실행</h4>
            <div class="panel-actions">
              <button class="panel-action-btn" @click="openAssignCouponModal(selectedUser)">쿠폰 지급</button>
              <button class="panel-action-btn" @click="viewReservations(selectedUser)">예약 내역 보기</button>
              <button class="panel-action-btn danger" @click="toggleUserStatus(selectedUser)">
                {{ selectedUser.status === 'ACTIVE' ? '계정 정지' : '계정 활성화' }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 페이지네이션 -->
    <div class="pagination-section" v-if="totalPages > 1">
      <div class="pagination">
        <button class="page-btn" :disabled="currentPage === 0" @click="changePage(currentPage - 1)">이전</button>
        <span class="page-info">{{ currentPage + 1 }} / {{ totalPages }}</span>
        <button class="page-btn" :disabled="currentPage >= totalPages - 1" @click="changePage(currentPage + 1)">다음</button>
      </div>
    </div>

<!-- 쿠폰 할당 모달 -->
<div v-if="showAssignCouponModal" class="modal-overlay" @click="showAssignCouponModal = false">
  <div class="modal-content" @click.stop>
    <div class="modal-header">
      <h3>쿠폰 지급</h3>
      <button class="modal-close" @click="showAssignCouponModal = false">×</button>
    </div>
    <div class="modal-body">
      <div class="coupon-list">
        <div v-if="!availableCoupons.length" class="no-data-small">지급 가능한 쿠폰이 없습니다.</div>
        <div v-else class="coupon-grid">
          <label v-for="c in availableCoupons" :key="c.id" class="coupon-item">
            <input type="checkbox" :value="c.id" v-model="selectedCouponIds" />
            <span class="code">{{ c.code }}</span>
            <span class="name">{{ c.name }}</span>
            <span class="meta">{{ c.discountType }} {{ c.discountValue }}</span>
          </label>
        </div>
      </div>
    </div>
    <div class="modal-actions">
      <button class="btn btn-secondary" @click="showAssignCouponModal = false">취소</button>
      <button class="btn btn-primary" @click="assignCoupons" :disabled="!selectedCouponIds.length || assigning">{{ assigning ? '지급 중...' : '지급' }}</button>
    </div>
  </div>
</div>
  </div>
</template>

<script>
import api from '../../api/http'

export default {
  name: 'UserManagement',
  data() {
    return {
      users: [],
      loading: false,
      sort: { prop: 'createdOn', order: 'descending' },
      searchForm: {
        name: '',
        email: '',
        role: ''
      },
      advancedFilters: {
        joinPeriod: '',
        activityStatus: '',
        hasReservation: '',
        provider: '',
        dormantMonths: 12
      },
      currentPage: 0,
      totalPages: 0,
      totalElements: 0,
      pageSize: 20,
      currentUserId: null,
      
      // UI 상태
      showAdvancedFilters: false,
      showQuickStats: true,
      selectedUser: null,
      
      // 선택된 사용자 통계
      selectedUserStats: {
        totalReservations: 0,
        totalPayment: 0,
        totalCoupons: 0,
        recentReservations: []
      },
      // 쿠폰 할당 모달 상태
      showAssignCouponModal: false,
      availableCoupons: [],
      selectedCouponIds: [],
      assigning: false
    }
  },
  mounted() {
    this.loadUsers();
    this.loadUserSummaryStats();
    // SSE를 통해 전달되는 관리자 갱신 신호 수신
    try {
      window.addEventListener('admin:refresh-users', this.loadUsers)
    } catch {}
  },
  beforeUnmount() {
    try { window.removeEventListener('admin:refresh-users', this.loadUsers) } catch {}
  },
  methods: {
    async loadUserSummaryStats(){
      try {
        const res = await api.get('/admin/users/stats')
        const stats = res?.data?.data || {}
        // stats: { total, admins, business, users }
        this.totalElements = Number(stats.total ?? this.totalElements)
      } catch (e) {
        // 통계 실패는 치명적 아님
      }
    },
    async loadUsers() {
      this.loading = true;
      try {
  const params = { page: this.currentPage, size: this.pageSize };
        if (this.searchForm.name) params.name = this.searchForm.name;
        if (this.searchForm.email) params.email = this.searchForm.email;
        if (this.searchForm.role) params.role = this.searchForm.role;
        if (this.advancedFilters.activityStatus) params.activityStatus = this.advancedFilters.activityStatus;
        if (this.advancedFilters.activityStatus === 'DORMANT' && this.advancedFilters.dormantMonths) {
          params.dormantMonths = this.advancedFilters.dormantMonths
        }
  if (this.advancedFilters.provider) params.provider = this.advancedFilters.provider;
  if (this.advancedFilters.hasReservation) params.hasReservation = this.advancedFilters.hasReservation;
  if (this.advancedFilters.joinPeriod) params.joinPeriod = this.advancedFilters.joinPeriod;
        // 정렬 파라미터 맵핑 (name, createdOn 허용)
        if (this.sort && this.sort.prop) {
          const allowed = new Set(['name', 'createdOn'])
          if (allowed.has(this.sort.prop)) {
            const dir = this.sort.order === 'ascending' ? 'asc' : 'desc'
            params.sort = `${this.sort.prop},${dir}`
          }
        }

        const response = await api.get('/admin/users', { params });
        const envelope = response.data;   // { success, data, error }
        const page = envelope?.data;      // PageResponse
        this.users = page?.content || [];
        this.totalPages = page?.totalPages || 0;
        this.totalElements = page?.totalElements || 0;
        this.currentPage = page?.number || 0;
      } catch (error) {
        alert('사용자 목록을 불러오는데 실패했습니다.');
      } finally {
        this.loading = false;
      }
    },

    sortBy(prop){
      if (this.sort.prop === prop) {
        this.sort.order = this.sort.order === 'ascending' ? 'descending' : 'ascending'
      } else {
        this.sort = { prop, order: 'ascending' }
      }
      this.currentPage = 0
      this.loadUsers()
    },
    
    async updateUserRole(user, newRole) {
      if (user.role === newRole) return;
      
      const confirmMessage = `${user.name}님의 역할을 "${this.getRoleLabel(newRole)}"로 변경하시겠습니까?`;
      const ok = window.confirm(confirmMessage)
      if (!ok) {
        // 선택된 값을 원래대로 되돌림
        this.$forceUpdate();
        return;
      }
      
      try {
        const response = await api.put(`/admin/users/${user.id}/role`, { role: newRole });
        
        // 로컬 데이터 업데이트
        user.role = newRole;
        alert(`${user.name}님의 역할이 성공적으로 변경되었습니다.`);

        // 대시보드 갱신을 위한 신호 전파 (현재 탭/페이지)
        try {
          window.dispatchEvent(new CustomEvent('admin:refresh-dashboard', { detail: { reason: 'user-role-changed', userId: user.id, newRole } }))
        } catch {}
        // 다음 방문 시에도 반영되도록 세션 플래그 설정
        try {
          sessionStorage.setItem('dashboardNeedsRefresh', String(Date.now()))
        } catch {}
        
      } catch (error) {
        alert(error.response?.data?.error || '역할 변경에 실패했습니다.');
        // 실패 시 원래 값으로 되돌림
        this.$forceUpdate();
      }
    },
    
    searchUsers() {
      this.currentPage = 0;
        this.loadUsers();
    },
    
    resetSearch() {
      this.searchForm = {
        name: '',
        email: '',
        role: ''
      };
      this.currentPage = 0;
      this.loadUsers();
    },
    
    changePage(page) {
      if (page >= 0 && page < this.totalPages) {
        this.currentPage = page;
        this.loadUsers();
      }
    },
    
    getRoleLabel(role) {
      const roleLabels = {
        'USER': '일반 사용자',
        'BUSINESS': '사업자',
        'ADMIN': '관리자'
      };
      return roleLabels[role] || role;
    },
    
    getStatusLabel(status) {
      const statusLabels = {
        'ACTIVE': '활성',
        'INACTIVE': '비활성',
        'SUSPENDED': '정지'
      };
      return statusLabels[status] || status;
    },
    
    getProviderLabel(provider) {
      const providerLabels = {
        'LOCAL': '이메일',
        'GOOGLE': '구글',
        'NAVER': '네이버',
        'KAKAO': '카카오'
      };
      return providerLabels[provider] || provider;
    },
    
    formatDate(dateString) {
      const date = new Date(dateString);
      return date.toLocaleDateString('ko-KR', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
      });
    },
    lastLoginTooltip(dateString) {
      if (!dateString) return '기록 없음';
      const d = new Date(dateString);
      const pad = (n) => String(n).padStart(2, '0');
      const yyyy = d.getFullYear();
      const MM = pad(d.getMonth() + 1);
      const dd = pad(d.getDate());
      const HH = pad(d.getHours());
      const mm = pad(d.getMinutes());
      return `${yyyy}-${MM}-${dd} ${HH}:${mm}`;
    },
    formatDateCompact(dateString) {
      if (!dateString) return ''
      const d = new Date(dateString)
      const pad = (n) => String(n).padStart(2, '0')
      const yyyy = d.getFullYear()
      const MM = pad(d.getMonth() + 1)
      const dd = pad(d.getDate())
      const HH = pad(d.getHours())
      const mm = pad(d.getMinutes())
      return `${yyyy}-${MM}-${dd} ${HH}:${mm}`
    },
    roleTagType(role) {
      const map = { USER: 'info', BUSINESS: 'warning', ADMIN: 'success' }
      return map[role] || 'info'
    },
    statusTagType(status) {
      const map = { ACTIVE: 'success', INACTIVE: 'info', SUSPENDED: 'danger' }
      return map[status] || 'info'
    },
    roleBadgeClass(role) {
      const map = { USER: 'role-user', BUSINESS: 'role-business', ADMIN: 'role-admin' }
      return map[role] || 'role-user'
    },
    statusBadgeClass(status) {
      const map = { ACTIVE: 'status-active', INACTIVE: 'status-inactive', SUSPENDED: 'status-suspended' }
      return map[status] || 'status-active'
    },
    
    // 새로운 CRM 기능들
    getActiveUsersCount() {
      return this.users.filter(user => user.status === 'ACTIVE').length;
    },
    getBusinessUsersCount() {
      return this.users.filter(user => user.role === 'BUSINESS').length;
    },
    getNewUsersThisMonth() {
      const thisMonth = new Date().getMonth();
      const thisYear = new Date().getFullYear();
      return this.users.filter(user => {
        const joinDate = new Date(user.createdOn);
        return joinDate.getMonth() === thisMonth && joinDate.getFullYear() === thisYear;
      }).length;
    },
    
    getLastActivityText(dateString) {
      if (!dateString) return '기록 없음'
      const last = new Date(dateString)
      const now = new Date()
      const startOfDay = (d) => { const x = new Date(d); x.setHours(0,0,0,0); return x }
      const msPerDay = 24 * 60 * 60 * 1000
      let diffDays = Math.floor((startOfDay(now) - startOfDay(last)) / msPerDay)
      if (diffDays <= 0) return '오늘'
      if (diffDays === 1) return '어제'
      if (diffDays < 7) return `${diffDays}일 전`
      if (diffDays < 30) return `${Math.floor(diffDays / 7)}주 전`
      if (diffDays < 365) return `${Math.floor(diffDays / 30)}개월 전`
      return `${Math.floor(diffDays / 365)}년 전`
    },
    getJoinPeriod(dateString) {
      if (!dateString) return '-';
      const joinDate = new Date(dateString);
      const now = new Date();
      const startOfDay = (d) => { const x = new Date(d); x.setHours(0,0,0,0); return x }
      const msPerDay = 24 * 60 * 60 * 1000
      let diffDays = Math.floor((startOfDay(now) - startOfDay(joinDate)) / msPerDay)
      if (diffDays <= 0) return '오늘 가입';
      if (diffDays === 1) return '어제 가입';
      if (diffDays < 7) return `${diffDays}일 전 가입`;
      if (diffDays < 30) return `${Math.floor(diffDays / 7)}주 전 가입`;
      if (diffDays < 365) return `${Math.floor(diffDays / 30)}개월 전 가입`;
      return `${Math.floor(diffDays / 365)}년 전 가입`;
    },
    
    // 최근 활동 값은 요약 통계 로드 시 계산하여 selectedUserStats.lastActivity 로 제공
    
    formatCurrency(amount) {
      if (!amount) return '0원';
      return amount.toLocaleString('ko-KR') + '원';
    },
    
    formatCouponDiscount(coupon) {
      if (!coupon) return '-';
      if (coupon.discountType === 'PERCENTAGE') {
        return `${coupon.discountValue}%`;
      } else if (coupon.discountType === 'FIXED_AMOUNT') {
        return this.formatCurrency(coupon.discountValue);
      }
      return '-';
    },
    
    formatCouponValidity(coupon) {
      if (!coupon) return '';
      const validFrom = coupon.validFrom ? new Date(coupon.validFrom).toLocaleDateString('ko-KR') : '-';
      const validTo = coupon.validTo ? new Date(coupon.validTo).toLocaleDateString('ko-KR') : '무제한';
      return `유효기간: ${validFrom} ~ ${validTo}`;
    },
    
    formatReservationStatus(status) {
      if (!status) return '-'
      const map = {
        PENDING: '대기',
        CONFIRMED: '확정',
        COMPLETED: '이용완료',
        CANCELLED: '취소',
        FAILED: '실패',
        REFUNDED: '환불',
        CHECKED_IN: '체크인',
        CHECKED_OUT: '체크아웃',
        UNKNOWN: '알수없음'
      }
      return map[status] || map.UNKNOWN
    },
    
    async selectUser(user) {
      this.selectedUser = user;
      await this.loadUserStats(user);
    },
    
    async loadUserStats(user) {
      try {
        const response = await api.get(`/admin/users/${user.id}/summary`)
        const s = response?.data?.data || {}
        const lastLoginAt = user.lastLoginAt || user.updatedOn || user.createdOn
        let lastActivity = '-'
        if (lastLoginAt) {
          const d = new Date(lastLoginAt)
          const diff = (Date.now() - d.getTime()) / (1000*60*60*24)
          if (diff < 1) lastActivity = '오늘'
          else if (diff < 7) lastActivity = `${Math.floor(diff)}일 전`
          else if (diff < 30) lastActivity = `${Math.floor(diff/7)}주 전`
          else lastActivity = `${Math.floor(diff/30)}개월 전`
        }

        // 보유 쿠폰 수는 다양한 백엔드 필드명에 대응 (totalCoupons, couponCount, totalCouponCount, ownedCouponCount, userCouponCount, coupons[]) 
        const couponList = Array.isArray(s.coupons) ? s.coupons : (Array.isArray(s.userCoupons) ? s.userCoupons : null)
        let totalCoupons = Number(
          s.totalCoupons ?? s.couponCount ?? s.totalCouponCount ?? s.ownedCouponCount ?? s.userCouponCount ?? (couponList ? couponList.length : 0) ?? 0
        )
        // 낙관적 업데이트로 이미 UI가 더 큰 값을 갖고 있다면 그 값을 유지 (일시적 지연 대비)
        const prevCoupons = Number(this.selectedUserStats?.totalCoupons ?? 0)
        if (!Number.isNaN(prevCoupons) && totalCoupons < prevCoupons) {
          totalCoupons = prevCoupons
        }

        this.selectedUserStats = {
          totalReservations: Number(s.totalReservations ?? 0),
          totalPayment: Number(s.totalPayment ?? 0),
          totalCoupons,
          lastActivity,
          recentReservations: Array.isArray(s.recentReservations) ? s.recentReservations.map(r => ({
            id: r.id ?? r.reservationId,
            hotelName: r.hotelName,
            checkIn: r.checkIn ?? r.startDate,
            checkOut: r.checkOut ?? r.endDate,
            // 예약 상태가 결제 상태(COMPLETED 등)로 덮어써져 모두 COMPLETED 로 보이는 문제 수정
            // 다양한 백엔드 필드명을 우선순위로 탐색 (예약 상태 우선, 결제/기타 상태 후순위)
            status: r.reservationStatus || r.status || r.bookingStatus || r.state || r.paymentStatus || r.paymentStatusCode || 'UNKNOWN'
          })) : [],
          coupons: couponList || []
        }
      } catch (error) {
        console.error('사용자 통계 로드 실패:', error);
        this.selectedUserStats = { totalReservations: 0, totalPayment: 0, totalCoupons: 0, lastActivity: '-', recentReservations: [] }
      }
    },
    
    async applyAdvancedFilters() {
      this.currentPage = 0;
      this.loadUsers();
    },
    
    resetAdvancedFilters() {
      this.advancedFilters = {
        joinPeriod: '',
        activityStatus: '',
        hasReservation: '',
        provider: '',
        dormantMonths: 12
      };
      this.applyAdvancedFilters();
    },
    
    // 빠른 액션 기능들
    sendMessage(user) {
      this.openMessageModal(user)
    },
    
    viewReservations(user) {
      try {
        const q = { userName: user.name }
        const first = this.selectedUserStats?.recentReservations?.[0]
        if (first?.checkIn) {
          const d = new Date(first.checkIn)
          // 예약 페이지에서 월 초기화에 사용될 anchorDate
          q.anchorDate = d.toISOString()
          q.stayFrom = d.toISOString()
          q.stayTo = d.toISOString()
        } else {
          // 최근 예약 없으면 오늘 기준으로 이동하고 메시지 표시 플래그 전달
          q.anchorDate = new Date().toISOString()
          q.noUserReservations = 'true'
        }
        this.$router.push({ path: '/admin/reservations', query: q })
      } catch (e) {
        // router 미존재 시 무시
      }
    },
    
    async openAssignCouponModal(user) {
      this.selectedUser = user
      try {
        // 관리자(시스템)가 보유한 쿠폰 풀을 불러오거나, 전체 쿠폰 중 활성/유효한 것만 노출
        const res = await api.get('/admin/coupons', { params: { active: true, page: 0, size: 200 } })
        const page = res?.data?.data || {}
        const items = Array.isArray(page.content) ? page.content : []
        this.availableCoupons = items
        this.selectedCouponIds = []
        this.showAssignCouponModal = true
      } catch (e) {
        alert('쿠폰 목록을 불러오지 못했습니다.')
      }
    },
    async assignCoupons() {
      if (!this.selectedUser?.id) return
      if (!this.selectedCouponIds.length) {
        alert('지급할 쿠폰을 선택하세요.')
        return
      }
      if (this.assigning) return
      this.assigning = true
      try {
        await api.post('/admin/coupons/assign', { targetUserId: this.selectedUser.id, couponIds: this.selectedCouponIds })
        alert('쿠폰이 지급되었습니다.')
        this.showAssignCouponModal = false
        // 낙관적 업데이트: 보유 쿠폰 수를 즉시 증가
        try {
          const add = Array.isArray(this.selectedCouponIds) ? this.selectedCouponIds.length : 0
          if (add > 0 && this.selectedUserStats) {
            this.selectedUserStats = {
              ...this.selectedUserStats,
              totalCoupons: Number(this.selectedUserStats.totalCoupons || 0) + add
            }
          }
        } catch {}
        this.selectedCouponIds = []
        // 새 쿠폰 지급이 반영되도록 사용자 요약 정보를 즉시 새로고침
        try {
          await this.loadUserStats(this.selectedUser)
        } catch {}
        // 다른 관리자 화면에서도 반영되도록 브로드캐스트 (선택)
        try {
          window.dispatchEvent(new CustomEvent('admin:refresh-users', { detail: { reason: 'coupon-assigned', userId: this.selectedUser.id } }))
        } catch {}
      } catch (e) {
        alert(e.response?.data?.message || '쿠폰 지급에 실패했습니다.')
      } finally {
        this.assigning = false
      }
    },

    // 메시지 발송 모달 (간단 구현)
    openMessageModal(user){
      const content = prompt(`${user.name}님에게 보낼 메시지를 입력하세요`, '')
      if (content == null) return
      if (!content.trim()) {
        alert('메시지 내용을 입력해주세요.')
        return
      }
      // 백엔드 메시지 엔드포인트가 없으므로 콘솔/알림 처리
      // 필요 시 /api/admin/notifications/send 같은 엔드포인트로 대체
      try {
        console.log('메시지 전송:', { toUserId: user.id, content })
        alert('메시지가 전송되었습니다.')
      } catch {
        alert('메시지 전송 실패')
      }
    },
    
    async toggleUserStatus(user) {
      const newStatus = user.status === 'ACTIVE' ? 'SUSPENDED' : 'ACTIVE';
      const action = newStatus === 'ACTIVE' ? '활성화' : '정지';
      
      if (confirm(`${user.name}님의 계정을 ${action}하시겠습니까?`)) {
        try {
          await api.put(`/admin/users/${user.id}/status`, { active: newStatus === 'ACTIVE' });
          
          user.status = newStatus;
          this.selectedUser.status = newStatus;
          alert(`${user.name}님의 계정이 ${action}되었습니다.`);
        } catch (error) {
          alert('상태 변경에 실패했습니다.');
        }
      }
    }
  }
}

</script>
<style scoped src="@/assets/css/admin/user-management.css"></style>