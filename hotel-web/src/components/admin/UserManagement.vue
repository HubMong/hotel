<template>
  <div class="user-management">
    <div class="page-header">
      <h1>사용자 관리</h1>
      <p class="page-description">전체 사용자를 조회하고 역할을 관리할 수 있습니다.</p>
    </div>

    <!-- 검색 필터 (네이티브) -->
    <div class="search-section">
      <form class="search-form" @submit.prevent="searchUsers">
        <div class="search-group">
          <label>이름</label>
          <input class="search-input" v-model="searchForm.name" placeholder="사용자 이름 검색" @keyup.enter="searchUsers" />
        </div>
        <div class="search-group">
          <label>이메일</label>
          <input class="search-input" v-model="searchForm.email" placeholder="이메일 검색" @keyup.enter="searchUsers" />
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
          <button type="submit" class="btn btn-primary">검색</button>
          <button type="button" class="btn btn-secondary" @click="resetSearch">초기화</button>
        </div>
      </form>
    </div>

    <!-- 사용자 목록 (네이티브) -->
    <div class="table-section">
      <div class="table-header">
        <h2>사용자 목록</h2>
        <div class="total-count">총 {{ totalElements }}명</div>
      </div>

      <div v-if="loading" class="loading">불러오는 중…</div>
      <div v-else-if="!users || users.length === 0" class="no-data">검색 결과가 없습니다.</div>
      <table v-else class="user-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>이름</th>
            <th>이메일</th>
            <th>현재 역할</th>
            <th>상태</th>
            <th>가입 방식</th>
            <th>가입일</th>
            <th>역할 변경</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in users" :key="row.id" class="user-row">
            <td>{{ row.id }}</td>
            <td>{{ row.name }}</td>
            <td>{{ row.email }}</td>
            <td>
              <span :class="['role-badge', roleBadgeClass(row.role)]">{{ getRoleLabel(row.role) }}</span>
            </td>
            <td>
              <span :class="['status-badge', statusBadgeClass(row.status)]">{{ getStatusLabel(row.status) }}</span>
            </td>
            <td>
              <span class="provider-badge">{{ getProviderLabel(row.provider) }}</span>
            </td>
            <td>{{ formatDate(row.createdOn) }}</td>
            <td>
              <select class="role-select" :disabled="row.id === currentUserId" :value="row.role" @change="e => updateUserRole(row, e.target.value)">
                <option value="USER">일반 사용자</option>
                <option value="BUSINESS">사업자</option>
                <option value="ADMIN">관리자</option>
              </select>
            </td>
          </tr>
        </tbody>
      </table>

      <div class="pagination-section" v-if="totalPages > 1">
        <div class="pagination">
          <button class="page-btn" :disabled="currentPage === 0" @click="changePage(currentPage - 1)">이전</button>
          <span class="page-info">{{ currentPage + 1 }} / {{ totalPages }}</span>
          <button class="page-btn" :disabled="currentPage >= totalPages - 1" @click="changePage(currentPage + 1)">다음</button>
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
      searchForm: {
        name: '',
        email: '',
        role: ''
      },
      currentPage: 0,
      totalPages: 0,
      totalElements: 0,
      pageSize: 20,
      currentUserId: null // TODO: 현재 로그인한 사용자 ID
    }
  },
  mounted() {
    this.loadUsers();
    // TODO: 현재 사용자 정보 로드
    // this.loadCurrentUser();
  },
  methods: {
    async loadUsers() {
      this.loading = true;
      try {
        const params = { page: this.currentPage, size: this.pageSize };
        if (this.searchForm.name) params.name = this.searchForm.name;
        if (this.searchForm.email) params.email = this.searchForm.email;
        if (this.searchForm.role) params.role = this.searchForm.role;

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
    }
  }
}

</script>
<style scoped src="@/assets/css/admin/user-management.css"></style>