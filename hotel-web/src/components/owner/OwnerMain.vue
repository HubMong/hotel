<template>
  <div class="owner-page">
    <aside class="sidebar">
      <div class="sidebar-header">
        <div class="logo">🏨 호텔 관리자</div>
      </div>

      <nav>
        <ul>
          <li
            :class="{ active: isActiveMenu('OwnerDashboard') }"
            :aria-current="isActiveMenu('OwnerDashboard') ? 'page' : null"
            @click="navigateTo('OwnerDashboard')"
          >
            대시보드
          </li>

          <li
            :class="{ active: isActiveMenu('OwnerHotelList','OwnerHotelCreate','OwnerHotelEdit') }"
            :aria-current="isActiveMenu('OwnerHotelList','OwnerHotelCreate','OwnerHotelEdit') ? 'page' : null"
            @click="navigateTo('OwnerHotelList')"
          >
            내 호텔
          </li>

          <li
            :class="{ active: isActiveMenu('OwnerReservation') }"
            :aria-current="isActiveMenu('OwnerReservation') ? 'page' : null"
            @click="navigateTo('OwnerReservation')"
          >
            예약 관리
          </li>

          <li
            :class="{ active: isActiveMenu('OwnerReview') }"
            :aria-current="isActiveMenu('OwnerReview') ? 'page' : null"
            @click="navigateTo('OwnerReview')"
          >
            리뷰 관리
          </li>

          <li
            :class="{ active: isActiveMenu('OwnerSupport') }"
            :aria-current="isActiveMenu('OwnerSupport') ? 'page' : null"
            @click="navigateTo('OwnerSupport')"
          >
            문의 관리
          </li>
        </ul>
      </nav>

      <div class="sidebar-footer">
        <button class="btn-homepage" @click="$router.push('/')">홈페이지 가기</button>
        <button class="btn-logout-sidebar" @click="logoutAndGoHome">로그아웃</button>
      </div>
    </aside>

    <main class="main-content">
      <router-view :user="user" @logout="logoutAndGoHome"></router-view>
    </main>
  </div>
</template>

<script>
import http from '@/api/http'
import UserApi from '@/api/UserApi'
import { getAuthUser, setAuthUser, clearAuthUser, notifyAuthChanged, getAuthRole } from '@/utils/auth-storage'

export default {
  name: 'OwnerMain',
  data() {
    return { 
        user: null,
        // 알림 관련 상태 제거됨
    }
  },
  methods: {
    async checkLoginStatus() {
      const stored = getAuthUser()
      if (stored) {
        this.user = stored
        this.ensureBusinessAccess()
        return
      }

      try {
        const data = await UserApi.getInfo()
        if (data) {
          setAuthUser(data)
          this.user = data
          this.ensureBusinessAccess()
          return
        }
      } catch (error) {
        const status = error.response?.status
        if (status === 401 || error.message?.includes('401')) {
          // 401 오류는 무시
        } else {
          console.warn('사업자 정보를 불러올 수 없습니다.', error)
        }
      }

      this.$router.push('/login')
    },
    ensureBusinessAccess() {
      const role = getAuthRole()
      if (role !== 'BUSINESS') {
        alert('접근 권한이 없습니다.')
        this.$router.push('/')
      }
    },
    
    // 알림 관련 메서드 제거됨

    // 라우트 이름으로 이동 (중복 네비 방지)
    navigateTo(routeName) {
      if (this.$route.name !== routeName) {
        this.$router.push({ name: routeName })
      }
    },
    // 현재 라우트가 메뉴 그룹에 포함되면 활성화
    isActiveMenu(...routeNames) {
      return routeNames.includes(this.$route.name)
    },
    async logoutAndGoHome() {
      try {
        await http.post('/users/logout')
      } catch (error) {
        console.warn('로그아웃 실패(무시 가능)', error)
      }
      clearAuthUser()
      notifyAuthChanged()
      alert('로그아웃 되었습니다.')
      this.$router.push('/')
    },
    // 다른 탭에서 로그아웃/로그인 변화가 있을 때 처리
    onStorage(e) {
      if (e.key === 'auth.lastChange') {
        this.checkLoginStatus()
      }
    },
    onAuthChange() {
      this.checkLoginStatus()
    }
  },
  mounted() {
    this.checkLoginStatus()
    window.addEventListener('storage', this.onStorage)
    window.addEventListener('authchange', this.onAuthChange)
    // 알림 초기 로드 및 폴링 시작 로직 제거됨
  },
  beforeUnmount() {
    window.removeEventListener('storage', this.onStorage)
    window.removeEventListener('authchange', this.onAuthChange)
    // 알림 폴링 중지 로직 제거됨
  }
}
</script>

<style scoped>
/* 전체 레이아웃 */
.owner-page {
  display: flex;
  height: 100vh;
  width: 100vw;
  margin: 0;
  background: #f3f4f6;
  font-family: 'Pretendard', sans-serif;
}

/* 사이드바 */
.sidebar {
  width: 240px;
  background: #1f2937;
  color: #e5e7eb;
  padding: 24px 16px;
  box-sizing: border-box;
  position: fixed;
  inset: 0 auto 0 0; /* top:0; left:0; bottom:0; */
  display: flex;
  flex-direction: column;
  transition: width 0.2s;
}

.sidebar-header .logo {
    font-weight: 800;
    font-size: 22px;
    margin-bottom: 30px; /* 기존 스타일 마진 복구 */
    text-align: center;
    color: #fff;
}
.sidebar .logo {
    margin-bottom: 30px; 
}


.sidebar nav { flex-grow: 1; }

.sidebar ul { list-style: none; padding: 0; margin: 0; }

.sidebar li {
  padding: 14px 20px;
  cursor: pointer;
  border-radius: 8px;
  margin: 8px 0;
  font-weight: 600;
  color: #d1d5db;
  transition: background-color .2s, color .2s;
  position: relative; /* 뱃지 위치 기준점 */
}

.sidebar li.active,
.sidebar li:hover {
  background: #4b5563;
  color: #fff;
}

/* 사이드바 하단 */
.sidebar-footer {
  padding: 16px;
  border-top: 1px solid #374151;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.sidebar-footer button {
  width: 100%;
  padding: 12px;
  border: none;
  border-radius: 8px;
  color: white;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  transition: background-color 0.2s;
}

.btn-homepage { background-color: #4f46e5; }
.btn-homepage:hover { background-color: #4338ca; }

.btn-logout-sidebar { background-color: #ef4444; }
.btn-logout-sidebar:hover { background-color: #dc2626; }

/* 메인 콘텐츠 영역 */
.main-content {
  margin-left: 240px;
  width: calc(100% - 240px);
  height: 100vh;
  padding: 0;
  box-sizing: border-box;
  overflow-y: auto;
}

/* 🔑 [페이지네이션 스타일 추가 (CSS는 그대로 유지)] */
.pagination-controls {
    display: flex;
    justify-content: center;
    align-items: center;
    margin-top: 25px;
    padding: 10px 0;
}

.pagination-controls button {
    background-color: #fff;
    border: 1px solid #ddd;
    padding: 8px 14px;
    margin: 0 4px;
    border-radius: 4px;
    cursor: pointer;
    font-size: 14px;
    transition: all 0.2s;
}

.pagination-controls button:hover:not(:disabled),
.pagination-controls button.active {
    background-color: #4f46e5;
    color: white;
    border-color: #4f46e5;
}

.pagination-controls button:disabled {
    cursor: not-allowed;
    opacity: 0.5;
}
</style>