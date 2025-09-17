<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AdminApi from '@/api/AdminApi'

const route = useRoute()
const router = useRouter()
const id = route.params.id

const loading = ref(true)
const err = ref(null)
const model = ref({ name:'', address:'', starRating: 0, description:'', active:true })

async function load() {
  if(id === 'new'){ loading.value = false; return }
  try{
    const data = await AdminApi.getHotel(id)
    model.value = { ...model.value, ...data }
  }catch(e){ console.error(e); err.value = '불러오기 실패' }
  finally{ loading.value = false }
}
onMounted(load)

async function save(){
  try{
    if(id==='new') await AdminApi.createHotel(model.value)
    else await AdminApi.updateHotel(id, model.value)
    alert('저장되었습니다.')
    router.push('/admin/hotels')
  }catch(e){ console.error(e); alert('저장 실패') }
}
</script>

<template>
  <div class="page-head">
    <h1>{{ id==='new' ? '새 호텔 등록' : '호텔 수정' }}</h1>
  </div>

  <div class="panel form">
    <div v-if="loading" class="muted">불러오는 중…</div>
    <div v-else-if="err" class="error">{{ err }}</div>

    <template v-else>
      <div class="row">
        <label>호텔명</label>
        <input v-model="model.name" class="input" />
      </div>
      <div class="row">
        <label>주소</label>
        <input v-model="model.address" class="input" />
      </div>
      <div class="row">
        <label>성급</label>
        <input type="number" min="0" max="5" v-model.number="model.starRating" class="input small" />
      </div>
      <div class="row">
        <label>설명</label>
        <textarea v-model="model.description" class="textarea"></textarea>
      </div>
      <div class="row">
        <label>상태</label>
        <label class="chk"><input type="checkbox" v-model="model.active" /> 활성</label>
      </div>

      <div class="actions">
        <button class="admin-btn" @click="$router.back()">취소</button>
        <button class="admin-btn primary" @click="save">저장</button>
      </div>
    </template>
  </div>
</template>

<style scoped>
.form .row{ display:grid; grid-template-columns: 120px 1fr; gap:10px; align-items:center; margin:10px 0; }
.input{ height:36px; padding:0 10px; border:1px solid var(--line); border-radius:8px; }
.input.small{ width:120px; }
.textarea{ min-height:120px; padding:10px; border:1px solid var(--line); border-radius:8px; resize:vertical; }
.chk{ display:flex; align-items:center; gap:6px; }
.actions{ display:flex; justify-content:flex-end; gap:8px; margin-top:14px; }
.muted{ color:#6b7280; }
.error{ color:#b42318; }
</style>
