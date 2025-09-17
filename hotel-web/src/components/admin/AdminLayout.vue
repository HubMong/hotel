<script setup>
import { RouterView, useRouter } from 'vue-router'
const router = useRouter()
function go(path){ router.push(path) }
</script>

<template>
  <div class="admin-app">
    <header class="admin-header">
      <div class="ah-inner">
        <strong class="brand" @click="go('/admin')">egoda · Admin</strong>
        <!-- (필요시) 우측 사용자 메뉴 -->
      </div>
    </header>

    <div class="admin-body">
      <aside class="admin-side">
        <nav class="side-nav">
          <button class="side-link" @click="go('/admin')">대시보드</button>
          <div class="side-label">심사/승인</div>
          <button class="side-link" @click="go('/admin/businesses')">사업자 승인</button>
          <div class="side-label">숙소 관리</div>
          <button class="side-link" @click="go('/admin/hotels')">호텔 목록</button>
        </nav>
      </aside>

      <main class="admin-main">
        <div class="admin-container">
          <RouterView />
        </div>
      </main>
    </div>
  </div>
</template>

<!-- 백오피스 전용 CSS를 이 컴포넌트에서만 로드 -->
<style src="@/assets/styles/admin/admin.css"></style>
