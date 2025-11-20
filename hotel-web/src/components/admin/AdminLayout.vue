<template>
  <div class="admin-layout">
    <aside class="sidebar" :class="{ open: isSidebarOpen }">
      <div class="sidebar-header">
        <h2>관리자 패널</h2>
      </div>

      <nav class="menu">
        <div class="menu-group">
          <div class="menu-group-title">대시보드</div>
          <router-link
            class="menu-item"
            :class="{ active: $route.path === '/admin/dashboard' }"
            to="/admin/dashboard"
          >
            <span>대시보드</span>
          </router-link>
        </div>

        <div class="menu-group">
          <div class="menu-group-title">사용자 관리</div>
          <router-link
            class="menu-item"
            :class="{ active: $route.path === '/admin/users' }"
            to="/admin/users"
          >
            <span>사용자 관리</span>
          </router-link>
          <router-link
            class="menu-item"
            :class="{ active: $route.path === '/admin/businesses' }"
            to="/admin/businesses"
          >
            <span>사업자 승인</span>
          </router-link>
        </div>

        <div class="menu-group">
          <div class="menu-group-title">호텔 운영</div>
          <router-link
            class="menu-item"
            :class="{ active: $route.path === '/admin/reviews' }"
            to="/admin/reviews"
          >
            <span>리뷰 관리</span>
          </router-link>
          <router-link
            class="menu-item"
            :class="{ active: $route.path === '/admin/inquiries' }"
            to="/admin/inquiries"
          >
            <span>문의 관리</span>
            <span v-if="unansweredCount > 0" class="badge">{{ unansweredCount }}</span> 
          </router-link>
        </div>

        <div class="menu-group">
          <div class="menu-group-title">재정 관리</div>
          <router-link
            class="menu-item"
            :class="{ active: $route.path === '/admin/sales' }"
            to="/admin/sales"
          >
            <span>매출·수수료</span>
          </router-link>
          <router-link
            class="menu-item"
            :class="{ active: $route.path === '/admin/payments' }"
            to="/admin/payments"
          >
            <span>결제 관리</span>
          </router-link>
          <router-link
            class="menu-item"
            :class="{ active: $route.path === '/admin/coupons' }"
            to="/admin/coupons"
          >
            <span>쿠폰 관리</span>
          </router-link>
        </div>
      </nav>
    </aside>

    <div v-if="isSidebarOpen" class="sidebar-overlay" @click="closeSidebar"></div>

    <div class="main-content">
      <header class="top-nav">
        <div class="nav-left">
          <button
            class="burger"
            aria-label="사이드바 열기/닫기"
            @click="toggleSidebar"
          >
            <span></span>
            <span></span>
            <span></span>
          </button>
          <h1>{{ pageTitle }}</h1>
        </div>

        <div class="nav-right">
          <div class="notification-area" @click="toggleNotificationDropdown">
            <button class="btn btn-icon" aria-label="알림 보기">
              <img src="@/assets/icons/bell.png" alt="알림" class="bell-icon" style="width: 20px; height: 20px;" /> 
              <span v-if="unansweredCount > 0" class="notification-badge">
                {{ unansweredCount }}
              </span>
            </button>

            <div v-if="showNotificationDropdown" class="notification-dropdown">
              <div class="notification-header">
                알림
                <span class="notification-status">{{ unansweredCount }}개 미처리</span>
              </div>

              <div v-if="unansweredCount > 0">
                <div 
                  class="notification-item notification-action-link"
                  @click="$router.push('/admin/inquiries'); showNotificationDropdown = false;"
                >
                  <i class="fas fa-exclamation-circle text-danger"></i>
                  <span class="message-text">현재 미답변 문의가 {{ unansweredCount }}건 있습니다.</span>
                  <i class="fas fa-arrow-right"></i>
                </div>
              </div>
              <div v-else class="notification-item no-notifications">
                <i class="fas fa-check-circle text-success"></i>
                <span>새로운 미처리 알림이 없습니다.</span>
              </div>
              
              <div class="notification-footer" @click.stop="clearNotifications">
                닫기
              </div>
            </div>
          </div>
          <div class="admin-profile">
            <div class="admin-avatar">{{ adminName.charAt(0) }}</div>
            <div class="admin-info">
              <span class="admin-name">{{ adminName }}</span>
              <span class="admin-role">관리자</span>
            </div>
          </div>
          <button class="btn btn-logout" @click="logout">로그아웃</button>
        </div>
      </header>

      <main class="content">
        <div class="admin-page">
          <router-view />
        </div>
      </main>
    </div>
  </div>
</template>

<script>
import http, { resolveBackendUrl } from '@/api/http'
import { getAuthUser, clearAuthUser, notifyAuthChanged } from '@/utils/auth-storage'

export default {
  name: 'AdminLayout',
  data() {
    return {
      adminName: '관리자',
      isSidebarOpen: false,

      // 🔑 [복원] 알림 데이터 상태 추가
      unansweredCount: 0, 
      showNotificationDropdown: false,

      // SSE 관련
      sse: null,
      sseConnected: false,
      sseBackoffMs: 1000,
      sseRetryTimer: null
    }
  },
  computed: {
    pageTitle() {
      const routeMap = {
        '/admin/dashboard': '대시보드',
        '/admin/users': '사용자 관리',
        '/admin/businesses': '사업자 승인',
        '/admin/hotels': '호텔 승인',
        '/admin/payments': '결제 관리',
        '/admin/reviews': '리뷰 관리',
        '/admin/coupons': '쿠폰 관리',
        '/admin/inquiries': '문의 관리',
      }
      return routeMap[this.$route.path] || '관리자 패널'
    }
  },
  methods: {
    // 🔑 [401 해결] 로그아웃 로직
    logout() {
      if (confirm('로그아웃 하시겠습니까?')) {
        // 1. SSE 연결 및 재연결 타이머를 즉시 종료 (401 오류 방지 핵심)
        this.teardownSSE(); 
        http.post('/users/logout').catch(() => {});
        clearAuthUser();
        notifyAuthChanged();
        // 2. 로그인 페이지로 이동
        this.$router.push('/login');
      }
    },
    
    toggleSidebar() { this.isSidebarOpen = !this.isSidebarOpen },
    closeSidebar() { this.isSidebarOpen = false },
    
    // 🔑 [복원 + 401 해결] 문의 수 조회 로직
    async fetchUnansweredCount() {
    try {
      if (!getAuthUser()) {
        this.unansweredCount = 0;
        return;
      }
      const res = await http.get('/admin/inquiries/unanswered-count')
            this.unansweredCount =
                typeof res.data === 'number' ? res.data : res.data?.count || 0
        } catch (e) {
          const status = e?.response?.status
          if (status === 401) {
            this.teardownSSE()
            clearAuthUser()
            notifyAuthChanged()
            this.$router.push('/login')
          } else {
            console.error('❌ 미답변 문의 수 로드 실패:', e)
          }
        }
    },

    // 🔑 [복원] 알림 드롭다운 제어 로직
    toggleNotificationDropdown() {
      this.showNotificationDropdown = !this.showNotificationDropdown
    },
    clearNotifications() {
      this.showNotificationDropdown = false
    },


    // SSE 연결 로직 (수정: fetchUnansweredCount 호출 추가)
    connectSSE() {
      if (this.sse) return
      const base = resolveBackendUrl('/api/admin/events/sse')
      if (!getAuthUser()) { this.scheduleReconnect(); return }
      try {
        const es = new EventSource(base, { withCredentials: true })
        this.sse = es
        es.addEventListener('ready', () => {
          this.sseConnected = true
          this.sseBackoffMs = 1000
          this.fetchUnansweredCount() // 🔑 [복원] 연결 성공 시 알림 수 로드
        })
        es.addEventListener('user-login', (ev) => {
          let payload = null
          try { payload = JSON.parse(ev.data) } catch {}
          try { window.dispatchEvent(new CustomEvent('admin:refresh-dashboard', { detail: { source: 'sse', type: 'user-login', payload } })) } catch {}
          try { window.dispatchEvent(new CustomEvent('admin:refresh-users', { detail: { source: 'sse', type: 'user-login', payload } })) } catch {}
        })
        // 🔑 [복원] 새 문의가 왔을 때 알림 수 갱신 로직
        es.addEventListener('new-inquiry', () => {
             this.fetchUnansweredCount() 
             if ('Notification' in window && Notification.permission === 'granted') {
                 new Notification('🔔 관리자 알림', { body: '새로운 웹사이트 문의가 등록되었습니다.' })
             }
        })

        es.onerror = () => {
          this.sseConnected = false
          try { es.close() } catch {}
          this.sse = null
          this.scheduleReconnect()
        }
      } catch {
        this.scheduleReconnect()
      }
    },
    
    scheduleReconnect() {
      if (this.sseRetryTimer) clearTimeout(this.sseRetryTimer)
      const delay = Math.min(this.sseBackoffMs || 1000, 30000)
      this.sseBackoffMs = Math.min((this.sseBackoffMs || 1000) * 2, 30000)
      this.sseRetryTimer = setTimeout(() => this.connectSSE(), delay)
    },
    
    teardownSSE() {
      if (this.sseRetryTimer) clearTimeout(this.sseRetryTimer)
      if (this.sse) { try { this.sse.close() } catch {} this.sse = null }
      this.sseConnected = false
    }
  },
  mounted() {
    this.$watch(() => this.$route.fullPath, () => { this.isSidebarOpen = false })
    this.connectSSE()
    // 🔑 [복원] 초기 로딩 시 알림 수 로드
    this.fetchUnansweredCount() 
  },
  beforeUnmount() { this.teardownSSE() }
}
</script>

<style src="@/assets/css/admin/admin-layout.css"></style>