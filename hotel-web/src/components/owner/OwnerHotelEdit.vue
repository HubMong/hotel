<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import OwnerApi from '@/api/OwnerApi'

const route = useRoute(); const router = useRouter()
const id = route.params.id

const loading = ref(true)
const err = ref(null)
const model = ref({ name:'', address:'', country:'KR', starRating:0, description:'' })
const images = ref([])        // [{url, sortNo, isCover}]
const amenityIds = ref([])    // [1,2,3]

async function load(){
  if(id==='new'){ loading.value=false; return }
  try{
    const data = await OwnerApi.getHotel(id)
    model.value = { ...model.value, ...data }
    images.value = data.images || []
    amenityIds.value = (data.amenities || []).map(a=>a.id)
  }catch(e){ console.error(e); err.value='불러오기 실패' }
  finally{ loading.value=false }
}
onMounted(load)

async function save(){
  try{
    if(id==='new'){
      const created = await OwnerApi.createHotel(model.value) // 서버에서 status=PENDING
      alert('등록 완료! 관리자 승인을 기다려주세요.')
      router.push('/owner/hotels')
    }else{
      await OwnerApi.updateHotel(id, model.value)
      // 이미지/편의시설은 별도 저장 API 호출(필요 시)
      alert('저장되었습니다.')
    }
  }catch(e){ console.error(e); alert('저장 실패') }
}
</script>

<template>
  <div class="page-head">
    <h1>{{ id==='new' ? '호텔 등록' : '호텔 수정' }}</h1>
  </div>

  <div class="panel form">
    <div v-if="loading" class="muted">불러오는 중…</div>
    <div v-else-if="err" class="error">{{ err }}</div>

    <template v-else>
      <div class="row"><label>호텔명</label><input v-model="model.name" class="input" /></div>
      <div class="row"><label>주소</label><input v-model="model.address" class="input" /></div>
      <div class="row"><label>국가</label><input v-model="model.country" class="input small" /></div>
      <div class="row"><label>성급</label><input type="number" min="0" max="5" v-model.number="model.starRating" class="input small" /></div>
      <div class="row"><label>소개</label><textarea v-model="model.description" class="textarea"></textarea></div>

      <!-- TODO: 이미지/편의시설 에디터는 이후 단계에서 컴포넌트 분리 -->
      <div class="actions">
        <button class="admin-btn" @click="$router.back()">취소</button>
        <button class="admin-btn primary" @click="save">저장</button>
      </div>
    </template>
  </div>
</template>
