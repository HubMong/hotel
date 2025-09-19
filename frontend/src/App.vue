<template>
  <div>
    <nav>
      <router-link to="/hotels">호텔 목록</router-link>
      <router-link to="/checkout">결제하기</router-link>
      <router-link to="/wishlist">내 위시리스트</router-link> |
      <router-link to="/search">검색</router-link>
      <router-link to="/login" v-if="!isLoggedIn">로그인</router-link> |
      <router-link to="/register" v-if="!isLoggedIn">회원가입</router-link>
      <button v-if="isLoggedIn" @click="logout">로그아웃</button>
    </nav>

    <router-view />
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from "vue";
import { useRouter } from "vue-router";

const router = useRouter();
const isLoggedIn = ref(!!localStorage.getItem("token"));

const logout = () => {
  localStorage.removeItem("token");
  isLoggedIn.value = false;
  // ✅ 로그아웃 시에도 이벤트 발생시켜서 다른 컴포넌트도 반응하도록
  window.dispatchEvent(new Event("storage"));
  router.push("/login");
};

const updateLoginState = () => {
  isLoggedIn.value = !!localStorage.getItem("token");
};

onMounted(() => {
  window.addEventListener("storage", updateLoginState);
});

onBeforeUnmount(() => {
  window.removeEventListener("storage", updateLoginState);
});
</script>
