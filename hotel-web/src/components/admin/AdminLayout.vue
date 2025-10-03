<template>
  <div class="admin-layout">
    <aside class="sidebar" :class="{ open: isSidebarOpen }">
      <div class="sidebar-header">
        <h2>관리자 패널</h2>
      </div>
      <nav class="menu">
        <div class="menu-group">
          <div class="menu-group-title">대시보드</div>
          <router-link class="menu-item" :class="{active: $route.path === '/admin/dashboard'}" to="/admin/dashboard">
            <span>대시보드</span>
          </router-link>
        </div>
        
        <div class="menu-group">
          <div class="menu-group-title">사용자 관리</div>
          <router-link class="menu-item" :class="{active: $route.path === '/admin/users'}" to="/admin/users">
            <span>사용자 관리</span>
          </router-link>
          <router-link class="menu-item" :class="{active: $route.path === '/admin/businesses'}" to="/admin/businesses">
            <span>사업자 승인</span>
          </router-link>
        </div>
        
        <div class="menu-group">
          <div class="menu-group-title">호텔 운영</div>
          <router-link class="menu-item" :class="{active: $route.path === '/admin/hotels'}" to="/admin/hotels">
            <span>호텔 관리</span>
          </router-link>
          <router-link class="menu-item" :class="{active: $route.path === '/admin/reservations'}" to="/admin/reservations">
            <span>예약 관리</span>
          </router-link>
          <router-link class="menu-item" :class="{active: $route.path === '/admin/reviews'}" to="/admin/reviews">
            <span>리뷰 관리</span>
          </router-link>
          <router-link class="menu-item" :class="{active: $route.path === '/admin/inquiries'}" to="/admin/inquiries">
            <span>문의 관리</span>
          </router-link>
        </div>
        
        <div class="menu-group">
          <div class="menu-group-title">재정 관리</div>
          <router-link class="menu-item" :class="{active: $route.path === '/admin/sales'}" to="/admin/sales">
            <span>매출·수수료</span>
          </router-link>
          <router-link class="menu-item" :class="{active: $route.path === '/admin/payments'}" to="/admin/payments">
            <span>결제 관리</span>
          </router-link>
          <router-link class="menu-item" :class="{active: $route.path === '/admin/coupons'}" to="/admin/coupons">
            <span>쿠폰 관리</span>
          </router-link>
        </div>
      </nav>
    </aside>

    <div v-if="isSidebarOpen" class="sidebar-overlay" @click="closeSidebar"></div>

    <div class="main-content">
      <header class="top-nav">
        <div class="nav-left">
          <button class="burger" aria-label="사이드바 열기/닫기" @click="toggleSidebar">
            <span></span>
            <span></span>
            <span></span>
          </button>
          <h1>{{ pageTitle }}</h1>
        </div>
        <div class="nav-right">
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
export default {
  name: 'AdminLayout',
  data() {
    return {
      adminName: '관리자',
      isSidebarOpen: false,
      // SSE 상태
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
        '/admin/hotels': '호텔 관리',
        '/admin/reservations': '예약 관리',
        '/admin/payments': '결제 관리',
        '/admin/reviews': '리뷰 관리',
        '/admin/coupons': '쿠폰 관리'
      }
      return routeMap[this.$route.path] || '관리자 패널'
    }
  },
  methods: {
    logout() {
      if (confirm('로그아웃 하시겠습니까?')) {
        try {
          localStorage.removeItem('token');
          localStorage.removeItem('user');
          localStorage.removeItem('userRole');
        } catch (e) {}
        this.$router.push('/').then(() => {
          window.location.reload();
        })
      }
    },
    toggleSidebar() { this.isSidebarOpen = !this.isSidebarOpen },
    closeSidebar() { this.isSidebarOpen = false },
    // SSE 연결 관리
    connectSSE() {
      if (this.sse) return
      // Attach JWT via query string because EventSource cannot set Authorization header
      const base = 'http://localhost:8888/api/admin/events/sse'
      const token = (typeof localStorage !== 'undefined' && localStorage.getItem('token')) || ''
      if (!token) { this.scheduleReconnect(); return }
      const url = `${base}?token=${encodeURIComponent(token)}`
      try {
        const es = new EventSource(url)
        this.sse = es
        es.addEventListener('ready', () => {
          this.sseConnected = true
          this.sseBackoffMs = 1000
        })
        es.addEventListener('user-login', (ev) => {
          let payload = null
          try { payload = JSON.parse(ev.data) } catch {}
          try { window.dispatchEvent(new CustomEvent('admin:refresh-dashboard', { detail: { source: 'sse', type: 'user-login', payload } })) } catch {}
          try { window.dispatchEvent(new CustomEvent('admin:refresh-users', { detail: { source: 'sse', type: 'user-login', payload } })) } catch {}
        })
        es.onerror = () => {
          this.sseConnected = false
          try { es.close() } catch {}
          this.sse = null
          this.scheduleReconnect()
        }
      } catch (e) {
        this.scheduleReconnect()
      }
    },
    scheduleReconnect() {
      if (this.sseRetryTimer) {
        clearTimeout(this.sseRetryTimer)
        this.sseRetryTimer = null
      }
      const delay = Math.min(this.sseBackoffMs || 1000, 30000)
      this.sseBackoffMs = Math.min((this.sseBackoffMs || 1000) * 2, 30000)
      this.sseRetryTimer = setTimeout(() => {
        this.connectSSE()
      }, delay)
    },
    teardownSSE() {
      if (this.sseRetryTimer) {
        clearTimeout(this.sseRetryTimer)
        this.sseRetryTimer = null
      }
      if (this.sse) {
        try { this.sse.close() } catch {}
        this.sse = null
      }
      this.sseConnected = false
    }
  },
  mounted() {
    this.$watch(() => this.$route.fullPath, () => {
      this.isSidebarOpen = false
    })
    // 관리자 레이아웃에서 SSE 연결 시작 (자동 갱신)
    this.connectSSE()
  },
  beforeUnmount() {
    this.teardownSSE()
  }
}

</script>
<style scoped src="@/assets/css/admin/admin-layout.css"></style>