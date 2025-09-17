<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
// /admin, /owner 로 시작하면 백오피스 레이아웃
const isBackoffice = computed(() =>
  route.path.startsWith('/admin') || route.path.startsWith('/owner')
)
</script>

<template>
  <!-- 사용자 사이트 레이아웃 -->
  <div v-if="!isBackoffice" class="site">
    <header class="topbar" role="banner">
      <div class="container topbar__inner">
        <router-link class="logo" to="/hotels/1">egoda</router-link>
        <nav class="actions" aria-label="주요 메뉴">
          <router-link class="btn btn--ghost" to="/me">마이 페이지</router-link>
          <router-link class="btn btn--ghost" to="/wishlist">찜 목록</router-link>
          <router-link class="btn btn--primary" to="/login">로그인</router-link>
        </nav>
      </div>
    </header>

    <section class="searchbar sticky-search">
      <div class="container">
        <div class="searchgrid">
          <div class="field">
            <label>어느 곳을 찾고 계신가요?</label>
            <input placeholder="도시, 숙소명 등" />
          </div>
          <div class="field"><label>체크-인</label><input type="date" /></div>
          <div class="field"><label>체크-아웃</label><input type="date" /></div>
          <div class="field"><label>인원</label><button type="button" class="ghost">성인 2명 · 객실 1개</button></div>
          <button class="btn btn--primary" type="button">검색</button>
        </div>
      </div>
    </section>

    <router-view class="page" />
  </div>

  <!-- 백오피스 레이아웃(상단바/검색바 없음) -->
  <div v-else class="backoffice">
    <router-view />
  </div>
</template>

<!-- App.vue 안에서는 스타일 제거(전부 assets로 분리) -->
