<template>
  <div class="admin-shell">
    <aside class="admin-aside">
      <div class="brand">Owner Console</div>
      <nav class="nav">
        <router-link to="/owner" class="nav-i">대시보드</router-link>
        <router-link to="/owner/hotels" class="nav-i">내 호텔</router-link>
      </nav>
    </aside>

    <main class="admin-main">
      <div class="inner">
        <router-view />
      </div>
    </main>
  </div>
</template>

<style src="@/assets/styles/admin/owner.css"></style>

