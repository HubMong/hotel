<!-- src/App.vue -->
<script setup>
import '@/assets/styles/app.css'   // ← 전역/공통 스타일
</script>

<template>
  <!-- 상단 헤더 (고정) -->
  <header class="topbar" role="banner">
    <div class="container topbar__inner">
      <router-link class="logo" to="/hotels/1" aria-label="호텔 상세 홈">egoda</router-link>
      <nav class="actions" aria-label="주요 메뉴">
        <router-link class="btn btn--ghost" to="/me">마이 페이지</router-link>
        <router-link class="btn btn--ghost" to="/wishlist">찜 목록</router-link>
        <router-link class="btn btn--primary" to="/login">로그인</router-link>
      </nav>
    </div>
  </header>

  <!-- 검색바 (헤더 아래에 고정) -->
  <section class="searchbar sticky-search">
    <div class="container">
      <div class="searchgrid">
        <div class="field">
          <label>어느 곳을 찾고 계신가요?</label>
          <input placeholder="도시, 숙소명 등" />
        </div>
        <div class="field">
          <label>체크-인</label>
          <input type="date" />
        </div>
        <div class="field">
          <label>체크-아웃</label>
          <input type="date" />
        </div>
        <div class="field">
          <label>인원</label>
          <button type="button" class="ghost">성인 2명 · 객실 1개</button>
        </div>
        <button class="btn btn--primary" type="button">검색</button>
      </div>
    </div>
  </section>

  <router-view class="page" />
</template>