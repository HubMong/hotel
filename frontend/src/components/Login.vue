<template>
  <div>
    <h2>로그인</h2>
    <form @submit.prevent="handleLogin">
      <input v-model="username" placeholder="아이디" />
      <input type="password" v-model="password" placeholder="비밀번호" />
      <button type="submit">로그인</button>
    </form>
  </div>
</template>

<script setup>
import { ref } from "vue";
import { useRouter } from "vue-router";
import api from "../api/api";

const username = ref("");
const password = ref("");
const router = useRouter();

const handleLogin = async () => {
  try {
    const res = await api.post("/auth/login", {
      username: username.value,
      password: password.value,
    });
    localStorage.setItem("token", res.data);
    window.dispatchEvent(new Event("storage"));
    alert("로그인 성공");
    router.push("/hotels");
  } catch {
    alert("로그인 실패");
  }
};
</script>
