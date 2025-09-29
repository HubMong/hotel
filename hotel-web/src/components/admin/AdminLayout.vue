<template>
  <div class="admin-layout">
    <aside class="sidebar" :class="{ open: isSidebarOpen }">
      <div class="sidebar-header">
        <h2>관리자 패널</h2>
      </div>
      <nav class="menu">
        <router-link class="menu-item" :class="{active: $route.path === '/admin/dashboard'}" to="/admin/dashboard">대시보드</router-link>
        <router-link class="menu-item" :class="{active: $route.path === '/admin/users'}" to="/admin/users">사용자 관리</router-link>
        <router-link class="menu-item" :class="{active: $route.path === '/admin/businesses'}" to="/admin/businesses">사업자 승인</router-link>
        <router-link class="menu-item" :class="{active: $route.path === '/admin/hotels'}" to="/admin/hotels">호텔 관리</router-link>
        <router-link class="menu-item" :class="{active: $route.path === '/admin/reservations'}" to="/admin/reservations">예약 관리</router-link>
        <router-link class="menu-item" :class="{active: $route.path === '/admin/sales'}" to="/admin/sales">매출·수수료</router-link>
        <router-link class="menu-item" :class="{active: $route.path === '/admin/payments'}" to="/admin/payments">결제 관리</router-link>
        <router-link class="menu-item" :class="{active: $route.path === '/admin/reviews'}" to="/admin/reviews">리뷰 관리</router-link>
        <router-link class="menu-item" :class="{active: $route.path === '/admin/coupons'}" to="/admin/coupons">쿠폰 관리</router-link>
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
          <span class="admin-name">{{ adminName }}</span>
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
      isSidebarOpen: false
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
    closeSidebar() { this.isSidebarOpen = false }
  },
  mounted() {
    this.$watch(() => this.$route.fullPath, () => {
      this.isSidebarOpen = false
    })
  }
}

</script>
<style scoped src="@/assets/css/admin/admin-layout.css"></style>