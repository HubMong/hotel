<template>
  <div class="business-apply">
    <div class="apply-container">
      <div class="apply-header">
        <h2>사업자 등록 신청</h2>
        <p class="description">사업자 정보를 입력하여 사업자 등록을 신청하세요.</p>
      </div>

      <form @submit.prevent="submitApplication" class="apply-form">
        <div class="form-group">
          <label for="businessName">사업자명 *</label>
          <input
            id="businessName"
            v-model="form.businessName"
            type="text"
            required
            placeholder="사업자명을 입력하세요"
          />
        </div>

        <div class="form-group">
          <label for="businessNumber">사업자 등록번호 *</label>
          <input
            id="businessNumber"
            v-model="form.businessNumber"
            type="text"
            required
            placeholder="000-00-00000 형식으로 입력하세요"
          />
        </div>

        <div class="form-group">
          <label for="address">주소 *</label>
          <input
            id="address"
            v-model="form.address"
            type="text"
            required
            placeholder="사업장 주소를 입력하세요"
          />
        </div>

        <div class="form-group">
          <label for="phone">연락처 *</label>
          <input
            id="phone"
            v-model="form.phone"
            type="tel"
            required
            placeholder="010-0000-0000 형식으로 입력하세요"
          />
        </div>

        <div class="form-actions">
          <button type="button" @click="goBack" class="btn btn-secondary">
            취소
          </button>
          <button type="submit" :disabled="loading" class="btn btn-primary">
            {{ loading ? '신청 중...' : '신청하기' }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/api/http'

const router = useRouter()
const loading = ref(false)

const form = reactive({
  businessName: '',
  businessNumber: '',
  address: '',
  phone: ''
})

const submitApplication = async () => {
  console.log('=== 사업자 신청 폼 제출 시작 ===')
  console.log('폼 데이터:', form)
  
  loading.value = true
  
  try {
  console.log('API 호출 시작: businesses/apply')
  const response = await api.post('/businesses/apply', form)
    console.log('API 응답:', response.data)
    
    alert(response.data.message || '신청이 완료되었습니다. 관리자 승인 후 활동할 수 있습니다.')
    router.push('/')
    
  } catch (error) {
    console.error('사업자 신청 오류:', error)
    console.error('오류 응답:', error.response?.data)
    
    if (error.response?.status === 401) {
      alert('인증이 필요합니다. 다시 로그인 후 시도해주세요.')
    } else if (error.response?.data?.error) {
      alert(error.response.data.error)
    } else {
      alert('신청 처리 중 오류가 발생했습니다. 다시 시도해주세요.')
    }
  } finally {
    loading.value = false
    console.log('=== 사업자 신청 폼 제출 완료 ===')
  }
}

const goBack = () => {
  router.back()
}
</script>

<style scoped src="@/assets/css/homepage/business-apply.css"></style>