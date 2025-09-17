<template>
  <header class="app-header">
    <div class="container">
      <div class="brand" @click="$router.push('/')">EGODA</div>
      <nav>
        <a @click.prevent="$router.push('/login')">로그인</a>
        <a @click.prevent="$router.push('/register')">회원가입</a>
      </nav>
    </div>
  </header>
</template>

<script>
export default {
  name: 'AppHeader'
}
</script>

<style scoped>
.app-header { background:#fff; border-bottom:1px solid #eee; }
.container { max-width:1100px; margin:0 auto; padding:12px 16px; display:flex; align-items:center; justify-content:space-between; }
.brand { font-weight:800; font-size:20px; cursor:pointer; }
nav a { margin-left:16px; color:#333; cursor:pointer; }
nav a:hover{ color:#4ec89f; }
</style>
