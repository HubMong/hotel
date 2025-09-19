<template>
  <div>
    <h2>회원가입</h2>
    <form @submit.prevent="handleRegister">
      <input v-model="username" placeholder="아이디" />
      <input type="password" v-model="password" placeholder="비밀번호" />
      <button type="submit">회원가입</button>
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

const handleRegister = async () => {
  try {
    await api.post("/auth/register", {
      username: username.value,
      password: password.value,
    });
    alert("회원가입 완료");
    router.push("/login");
  } catch {
    alert("회원가입 실패");
  }
};
</script>
