<template>
  <div class="coupon-management">
    <div class="management-header">
      <h2>쿠폰 관리</h2>
      <button class="btn btn-primary" @click="showCreateModal = true">쿠폰 생성</button>
    </div>

    <!-- 통계 카드 -->
    <div class="stats-cards">
      <div class="stat-card">
        <div class="stat-icon total"></div>
        <div class="stat-content">
          <h3>{{ stats.totalCoupons || 0 }}</h3>
          <p>전체 쿠폰</p>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon active"></div>
        <div class="stat-content">
          <h3>{{ stats.activeCoupons || 0 }}</h3>
          <p>활성 쿠폰</p>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon inactive"></div>
        <div class="stat-content">
          <h3>{{ stats.inactiveCoupons || 0 }}</h3>
          <p>비활성 쿠폰</p>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon expired"></div>
        <div class="stat-content">
          <h3>{{ stats.expiredCoupons || 0 }}</h3>
          <p>만료된 쿠폰</p>
        </div>
      </div>
    </div>

    <!-- 필터 섹션 -->
    <div class="filter-section">
      <div class="filter-row">
        <div class="filter-group">
          <label>상태</label>
          <select v-model="filters.status" @change="loadCoupons">
            <option value="">전체</option>
            <option value="ACTIVE">활성</option>
            <option value="INACTIVE">비활성</option>
          </select>
        </div>
        <div class="filter-group">
          <label>할인 타입</label>
          <select v-model="filters.discountType" @change="loadCoupons">
            <option value="">전체</option>
            <option value="PERCENT">퍼센트</option>
            <option value="FIXED">고정금액</option>
          </select>
        </div>
        <div class="filter-group">
          <label>쿠폰 코드</label>
          <input 
            type="text" 
            v-model="filters.code" 
            placeholder="코드 검색"
            @input="debounceSearch"
          />
        </div>
        <div class="filter-group">
          <label>쿠폰명</label>
          <input 
            type="text" 
            v-model="filters.name" 
            placeholder="쿠폰명 검색"
            @input="debounceSearch"
          />
        </div>
        <div class="filter-group">
          <button class="btn btn-secondary" @click="resetFilters">초기화</button>
        </div>
      </div>
    </div>

    <!-- 쿠폰 테이블 -->
    <div class="table-container">
      <table class="coupon-table">
        <thead>
          <tr>
            <th>
              ID
              <button class="sort-btn" @click.stop="sortBy('id')" style="background:none;border:none;padding:0;margin-left:4px;vertical-align:middle;">
                <svg width="12" height="16" viewBox="0 0 12 24">
                  <polygon points="6,2 2,8 10,8" :fill="sort.prop==='id' && sort.order==='ascending' ? '#222' : '#ccc'" />
                  <polygon points="6,22 2,16 10,16" :fill="sort.prop==='id' && sort.order==='descending' ? '#222' : '#ccc'" />
                </svg>
              </button>
            </th>
            <th>
              코드
              <button class="sort-btn" @click.stop="sortBy('code')" style="background:none;border:none;padding:0;margin-left:4px;vertical-align:middle;">
                <svg width="12" height="16" viewBox="0 0 12 24">
                  <polygon points="6,2 2,8 10,8" :fill="sort.prop==='code' && sort.order==='ascending' ? '#222' : '#ccc'" />
                  <polygon points="6,22 2,16 10,16" :fill="sort.prop==='code' && sort.order==='descending' ? '#222' : '#ccc'" />
                </svg>
              </button>
            </th>
            <th>
              쿠폰명
              <button class="sort-btn" @click.stop="sortBy('name')" style="background:none;border:none;padding:0;margin-left:4px;vertical-align:middle;">
                <svg width="12" height="16" viewBox="0 0 12 24">
                  <polygon points="6,2 2,8 10,8" :fill="sort.prop==='name' && sort.order==='ascending' ? '#222' : '#ccc'" />
                  <polygon points="6,22 2,16 10,16" :fill="sort.prop==='name' && sort.order==='descending' ? '#222' : '#ccc'" />
                </svg>
              </button>
            </th>
            <th>할인</th>
            <th>
              상태
              <button class="sort-btn" @click.stop="sortBy('status')" style="background:none;border:none;padding:0;margin-left:4px;vertical-align:middle;">
                <svg width="12" height="16" viewBox="0 0 12 24">
                  <polygon points="6,2 2,8 10,8" :fill="sort.prop==='status' && sort.order==='ascending' ? '#222' : '#ccc'" />
                  <polygon points="6,22 2,16 10,16" :fill="sort.prop==='status' && sort.order==='descending' ? '#222' : '#ccc'" />
                </svg>
              </button>
            </th>
            <th>유효기간</th>
            <th>사용량</th>
            <th>
              생성일
              <button class="sort-btn" @click.stop="sortBy('createdAt')" style="background:none;border:none;padding:0;margin-left:4px;vertical-align:middle;">
                <svg width="12" height="16" viewBox="0 0 12 24">
                  <polygon points="6,2 2,8 10,8" :fill="sort.prop==='createdAt' && sort.order==='ascending' ? '#222' : '#ccc'" />
                  <polygon points="6,22 2,16 10,16" :fill="sort.prop==='createdAt' && sort.order==='descending' ? '#222' : '#ccc'" />
                </svg>
              </button>
            </th>
            <th>작업</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="coupon in coupons" :key="coupon.id">
            <td>{{ coupon.id }}</td>
            <td>
              <span class="coupon-code">{{ coupon.code }}</span>
            </td>
            <td>{{ coupon.name }}</td>
            <td>
              <span class="discount-display">{{ coupon.discountDisplay }}</span>
            </td>
            <td>
              <span 
                class="status-badge" 
                :class="coupon.status.toLowerCase()"
              >
                {{ getStatusText(coupon.status) }}
              </span>
            </td>
            <td>
              <div class="validity-info">
                <div>{{ formatDate(coupon.validFrom) }}</div>
                <div>~ {{ formatDate(coupon.validUntil) }}</div>
                <span 
                  v-if="!coupon.isValid" 
                  class="expired-badge"
                >
                  만료됨
                </span>
              </div>
            </td>
            <td>
              <div class="usage-info">
                <span>{{ coupon.usedCount }}</span>
                <span v-if="coupon.usageLimit">/ {{ coupon.usageLimit }}</span>
                <span v-else>/ 무제한</span>
                <div v-if="coupon.remainingUsage !== null" class="remaining">
                  (남은 횟수: {{ coupon.remainingUsage }})
                </div>
              </div>
            </td>
            <td>{{ formatDate(coupon.createdAt) }}</td>
            <td>
              <div class="action-buttons">
                <button class="btn btn-sm btn-outline" @click="editCoupon(coupon)" title="수정">수정</button>
                <button 
                  class="btn btn-sm btn-warning"
                  @click="toggleCouponStatus(coupon)"
                  :title="coupon.status === 'ACTIVE' ? '비활성화' : '활성화'"
                >{{ coupon.status === 'ACTIVE' ? '비활성화' : '활성화' }}</button>
                <button 
                  class="btn btn-sm btn-danger"
                  @click="deleteCoupon(coupon)"
                  :disabled="coupon.usedCount > 0"
                  title="삭제"
                >삭제</button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 페이지네이션 -->
    <div class="pagination-container">
      <div class="pagination-info">
        총 {{ pagination.totalElements }}개 중 
        {{ pagination.size * pagination.number + 1 }}-{{ Math.min(pagination.size * (pagination.number + 1), pagination.totalElements) }}개 표시
      </div>
      <div class="pagination">
        <button 
          @click="goToPage(0)" 
          :disabled="pagination.first"
          class="btn btn-sm"
        >
          처음
        </button>
        <button 
          @click="goToPage(pagination.number - 1)" 
          :disabled="pagination.first"
          class="btn btn-sm"
        >
          이전
        </button>
        <span class="page-info">
          {{ pagination.number + 1 }} / {{ pagination.totalPages }}
        </span>
        <button 
          @click="goToPage(pagination.number + 1)" 
          :disabled="pagination.last"
          class="btn btn-sm"
        >
          다음
        </button>
        <button 
          @click="goToPage(pagination.totalPages - 1)" 
          :disabled="pagination.last"
          class="btn btn-sm"
        >
          마지막
        </button>
      </div>
    </div>

    <!-- 쿠폰 생성/수정 모달 -->
    <div v-if="showCreateModal || showEditModal" class="modal-overlay" @click="closeModals">
      <div class="modal-content coupon-modal" @click.stop>
        <div class="modal-header minimal">
          <h3>{{ showCreateModal ? '쿠폰 생성' : '쿠폰 수정' }}</h3>
          <button class="modal-close" @click="closeModals" aria-label="닫기">×</button>
        </div>
        
        <form class="coupon-form" @submit.prevent="showCreateModal ? createCoupon() : updateCoupon()">
          <div class="form-row">
            <div class="form-group">
              <label>쿠폰 코드 *</label>
              <input 
                type="text" 
                v-model="couponForm.code" 
                required
                :disabled="showEditModal"
                placeholder="WELCOME2024"
              />
              <small class="field-hint">영문/숫자 조합 (대문자 권장)</small>
            </div>
            <div class="form-group">
              <label>쿠폰명 *</label>
              <input 
                type="text" 
                v-model="couponForm.name" 
                required
                placeholder="신규 회원 환영 쿠폰"
              />
              <small class="field-hint">관리 화면 및 사용자 화면에 표시될 이름</small>
            </div>
          </div>

          <div class="form-group">
            <label>설명</label>
            <textarea 
              v-model="couponForm.description" 
              placeholder="쿠폰에 대한 설명을 입력하세요"
              rows="3"
            ></textarea>
            <small class="field-hint">선택 입력 – 쿠폰 목적 또는 제한 조건 등을 안내</small>
          </div>

          <div class="form-row">
            <div class="form-group">
              <label>할인 타입 *</label>
              <select v-model="couponForm.discountType" required>
                <option value="PERCENT">퍼센트 할인</option>
                <option value="FIXED">고정 금액 할인</option>
              </select>
              <small class="field-hint">퍼센트는 결제 금액 비율, 고정은 특정 금액 차감</small>
            </div>
            <div class="form-group">
              <label>할인 값 *</label>
              <input 
                type="number" 
                v-model.number="couponForm.discountValue" 
                required
                min="1"
                :max="couponForm.discountType === 'PERCENT' ? 100 : 1000000"
                :placeholder="couponForm.discountType === 'PERCENT' ? '10' : '10000'"
              />
              <span class="input-suffix">
                {{ couponForm.discountType === 'PERCENT' ? '%' : '원' }}
              </span>
              <small class="field-hint" v-if="couponForm.discountType==='PERCENT'">1~100 사이 퍼센트 값</small>
              <small class="field-hint" v-else>할인 금액(원). 최대 1,000,000</small>
            </div>
          </div>

          <div class="form-row">
            <div class="form-group">
              <label>최소 주문 금액</label>
              <input 
                type="number" 
                v-model.number="couponForm.minOrderAmount" 
                min="0"
                placeholder="0"
              />
              <span class="input-suffix">원</span>
              <small class="field-hint">해당 금액 이상 주문 시 적용 (선택)</small>
            </div>
            <div class="form-group">
              <label>최대 할인 금액</label>
              <input 
                type="number" 
                v-model.number="couponForm.maxDiscountAmount" 
                min="0"
                placeholder="제한 없음"
              />
              <span class="input-suffix">원</span>
              <small class="field-hint" v-if="couponForm.discountType==='PERCENT'">퍼센트 할인 시 절대 상한 (선택)</small>
              <small class="field-hint" v-else>고정 금액 할인은 일반적으로 불필요</small>
            </div>
          </div>

          <div class="form-row">
            <div class="form-group">
              <label>유효 시작일 *</label>
              <input 
                type="datetime-local" 
                v-model="couponForm.validFrom" 
                required
              />
            </div>
            <div class="form-group">
              <label>유효 종료일 *</label>
              <input 
                type="datetime-local" 
                v-model="couponForm.validUntil" 
                required
              />
            </div>
          </div>

          <div class="form-row">
            <div class="form-group">
              <label>사용 제한 횟수</label>
              <input 
                type="number" 
                v-model.number="couponForm.usageLimit" 
                min="1"
                placeholder="제한 없음"
              />
              <small class="field-hint">미입력 시 무제한 사용 가능</small>
            </div>
            <div class="form-group">
              <label>상태</label>
              <select v-model="couponForm.status">
                <option value="ACTIVE">활성</option>
                <option value="INACTIVE">비활성</option>
              </select>
              <small class="field-hint">생성 후 즉시 사용 가능 여부</small>
            </div>
          </div>

          <div class="modal-actions minimal">
            <button type="button" class="btn btn-secondary subtle" @click="closeModals">취소</button>
            <button type="submit" class="btn btn-primary solid">{{ showCreateModal ? '생성' : '수정' }}</button>
          </div>
        </form>
      </div>
    </div>

    <!-- 로딩 스피너 -->
    <div v-if="loading" class="loading-overlay">
      <div class="spinner"></div>
    </div>
  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue'
import api from '../../api/http'

export default {
  name: 'CouponManagement',
  setup() {
    // 반응형 데이터
    const coupons = ref([])
    const stats = ref({})
    const loading = ref(false)
    const showCreateModal = ref(false)
    const showEditModal = ref(false)
  const editingCoupon = ref(null)
  const justCreatedId = ref(null)
  const justCreatedCoupon = ref(null)
    
    // 필터 상태
    const filters = reactive({
      status: '',
      discountType: '',
      code: '',
      name: ''
    })

    // 페이지네이션 상태
    const pagination = reactive({
      number: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
      first: true,
      last: true
    })

    // 쿠폰 폼 데이터
    const couponForm = reactive({
      code: '',
      name: '',
      description: '',
      discountType: 'PERCENT',
      discountValue: null,
      minOrderAmount: null,
      maxDiscountAmount: null,
      validFrom: '',
      validUntil: '',
      usageLimit: null,
      status: 'ACTIVE'
    })

    // 검색 디바운스
    let searchTimeout = null
    const debounceSearch = () => {
      clearTimeout(searchTimeout)
      searchTimeout = setTimeout(() => {
        pagination.number = 0
        loadCoupons()
      }, 500)
    }

    // 정렬 상태 및 기본값
    const sort = ref({ prop: 'id', order: 'descending' })
    const defaultSort = 'id,desc'

    // 허용된 정렬 필드 -> API 필드 매핑
    const toApiSort = ({ prop, order }) => {
      if (!prop) return defaultSort
      const dir = order === 'ascending' ? 'asc' : 'desc'
      // UI 필드명과 API 필드명이 다른 경우만 매핑
      const map = {
        id: 'id',
        code: 'code',
        name: 'name',
        status: 'isActive',
        // 백엔드에 createdAt 정렬이 없을 수 있어, 생성일 정렬은 id 정렬로 매핑
        createdAt: 'id'
      }
      const apiProp = map[prop]
      return apiProp ? `${apiProp},${dir}` : defaultSort
    }

    // UI-API 매핑 유틸
    const mapUiToApiDiscountType = (val) => {
      if (val === 'PERCENT') return 'PERCENTAGE'
      if (val === 'FIXED') return 'FIXED_AMOUNT'
      return val || null
    }
    const mapApiToUiDiscountType = (val) => {
      if (val === 'PERCENTAGE') return 'PERCENT'
      if (val === 'FIXED_AMOUNT') return 'FIXED'
      return val || ''
    }
    const normalizeDateTimeSeconds = (val) => {
      if (!val) return val
      // datetime-local 형식(YYYY-MM-DDTHH:mm)에 초가 없으면 ":00" 추가
      return val.length === 16 ? `${val}:00` : val
    }

    // 쿠폰 목록 로드
    const loadCoupons = async () => {
      loading.value = true
      try {
        const params = {
          page: pagination.number,
          size: pagination.size,
          sort: toApiSort(sort.value)
        }

        // 필터 조건 추가
        if (filters.status) params.active = filters.status === 'ACTIVE'
        if (filters.discountType) params.discountType = mapUiToApiDiscountType(filters.discountType)
        if (filters.code) params.code = filters.code
        if (filters.name) params.name = filters.name

        console.log('쿠폰 목록 요청 파라미터:', params)

        const response = await api.get('/admin/coupons', { params })
        console.log('쿠폰 목록 응답:', response.data)

        const data = response?.data?.data || {}
        const items = Array.isArray(data.content) ? data.content : []
        
        // Coupon 엔티티를 UI 형태로 변환
        coupons.value = items.map(coupon => ({
          id: coupon.id,
          code: coupon.code,
          name: coupon.name,
          discountDisplay: getDiscountDisplay(coupon),
          status: coupon.isActive ? 'ACTIVE' : 'INACTIVE',
          validFrom: coupon.validFrom,
          validUntil: coupon.validTo,
          isValid: coupon.validTo ? new Date(coupon.validTo) > new Date() : true,
          usedCount: 0, // 추후 구현
          usageLimit: null, // 추후 구현
          remainingUsage: null, // 추후 구현
          createdAt: new Date().toISOString(), // 추후 구현
          discountType: coupon.discountType,
          discountValue: coupon.discountValue,
          minOrderAmount: coupon.minSpend
        }))
        
        Object.assign(pagination, {
          number: data.number || 0,
          size: data.size || 20,
          totalElements: data.totalElements || 0,
          totalPages: data.totalPages || 0,
          first: data.first ?? true,
          last: data.last ?? true
        })

        console.log('쿠폰 목록 매핑 완료:', coupons.value.length, '건')

      } catch (error) {
        console.error('쿠폰 목록 로드 실패:', error)
        alert('쿠폰 목록을 불러오는데 실패했습니다.')
      } finally {
        loading.value = false
      }
    }

    // 쿠폰 통계 로드
    const loadStats = async () => {
      try {
        const response = await api.get('/admin/coupons/stats')
        console.log('쿠폰 통계 응답:', response.data)
        stats.value = response?.data?.data || response.data
      } catch (error) {
        console.error('쿠폰 통계 로드 실패:', error)
      }
    }

    // 할인 표시 텍스트 생성
    const getDiscountDisplay = (coupon) => {
      if (coupon.discountType === 'PERCENTAGE') {
        return `${coupon.discountValue}%`
      } else {
        return `${coupon.discountValue.toLocaleString()}원`
      }
    }

    // 쿠폰 생성
    const createCoupon = async () => {
      try {
        const formData = {
          userId: 1, // 기본 관리자 ID, 추후 실제 로그인 사용자로 변경
          name: couponForm.name,
          code: couponForm.code,
          discountType: mapUiToApiDiscountType(couponForm.discountType),
          discountValue: couponForm.discountValue,
          minSpend: couponForm.minOrderAmount || 0,
          validFrom: normalizeDateTimeSeconds(couponForm.validFrom),
          validTo: normalizeDateTimeSeconds(couponForm.validUntil),
          isActive: couponForm.status === 'ACTIVE'
        }

        console.log('쿠폰 생성 요청 데이터:', formData)

        const resp = await api.post('/admin/coupons', formData)
        console.log('쿠폰 생성 응답:', resp.data)

        alert('쿠폰이 성공적으로 생성되었습니다.')
        closeModals()
        pagination.number = 0
        await loadCoupons()
        await loadStats()
      } catch (error) {
        console.error('쿠폰 생성 실패:', error)
        alert(error.response?.data?.message || '쿠폰 생성에 실패했습니다.')
      }
    }

    // 쿠폰 수정
    const updateCoupon = async () => {
      try {
        const formData = { ...couponForm }
        // 할인 타입 매핑 (UI -> API)
        if (formData.discountType) {
          formData.discountType = mapUiToApiDiscountType(formData.discountType)
        }
        // 날짜 문자열 정규화 (초 추가)
        if (formData.validFrom) formData.validFrom = normalizeDateTimeSeconds(formData.validFrom)
        if (formData.validUntil) formData.validUntil = normalizeDateTimeSeconds(formData.validUntil)
        
        // 변경되지 않은 필드 제거
        Object.keys(formData).forEach(key => {
          if (formData[key] === editingCoupon.value[key] || 
              formData[key] === null || 
              formData[key] === '') {
            delete formData[key]
          }
        })

        await api.put(`/admin/coupons/${editingCoupon.value.id}`, formData)

        alert('쿠폰이 성공적으로 수정되었습니다.')
        closeModals()
        await loadCoupons()
        await loadStats()
      } catch (error) {
        console.error('쿠폰 수정 실패:', error)
        alert(error.response?.data?.message || '쿠폰 수정에 실패했습니다.')
      }
    }

    // 쿠폰 상태 토글
    const toggleCouponStatus = async (coupon) => {
      const newStatus = coupon.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'
      const action = newStatus === 'ACTIVE' ? '활성화' : '비활성화'
      
      if (!confirm(`정말로 이 쿠폰을 ${action}하시겠습니까?`)) return

      try {
        console.log('쿠폰 상태 변경 요청:', { couponId: coupon.id, newStatus })
        
        await api.put(`/admin/coupons/${coupon.id}/status`, {
          status: newStatus
        })

        alert(`쿠폰이 성공적으로 ${action}되었습니다.`)
        await loadCoupons()
        await loadStats()
      } catch (error) {
        console.error('쿠폰 상태 변경 실패:', error)
        alert(`쿠폰 ${action}에 실패했습니다.`)
      }
    }

    // 쿠폰 삭제
    const deleteCoupon = async (coupon) => {
      if (coupon.usedCount > 0) {
        alert('이미 사용된 쿠폰은 삭제할 수 없습니다.')
        return
      }

      if (!confirm('정말로 이 쿠폰을 삭제하시겠습니까?')) return

      try {
        await api.delete(`/admin/coupons/${coupon.id}`)

        alert('쿠폰이 성공적으로 삭제되었습니다.')
        await loadCoupons()
        await loadStats()
      } catch (error) {
        console.error('쿠폰 삭제 실패:', error)
        alert('쿠폰 삭제에 실패했습니다.')
      }
    }

    // 쿠폰 수정 모달 열기
    const editCoupon = (coupon) => {
      editingCoupon.value = coupon
      
      // 폼에 기존 데이터 설정
      Object.assign(couponForm, {
        code: coupon.code,
        name: coupon.name,
        description: coupon.description || '',
        // API 값 -> UI 값 매핑
        discountType: mapApiToUiDiscountType(coupon.discountType),
        discountValue: coupon.discountValue,
        minOrderAmount: coupon.minOrderAmount,
        maxDiscountAmount: coupon.maxDiscountAmount,
        validFrom: formatDateTimeLocal(coupon.validFrom),
        validUntil: formatDateTimeLocal(coupon.validUntil),
        usageLimit: coupon.usageLimit,
        status: coupon.status
      })
      
      showEditModal.value = true
    }

    // 모달 닫기
    const closeModals = () => {
      showCreateModal.value = false
      showEditModal.value = false
      editingCoupon.value = null
      
      // 폼 초기화
      Object.assign(couponForm, {
        code: '',
        name: '',
        description: '',
        discountType: 'PERCENT',
        discountValue: null,
        minOrderAmount: null,
        maxDiscountAmount: null,
        validFrom: '',
        validUntil: '',
        usageLimit: null,
        status: 'ACTIVE'
      })
    }

    // 페이지 이동
    const goToPage = (page) => {
      if (page >= 0 && page < pagination.totalPages) {
        pagination.number = page
        loadCoupons()
      }
    }

    // 정렬 토글
    const sortBy = (prop) => {
      if (sort.value.prop === prop) {
        sort.value.order = sort.value.order === 'ascending' ? 'descending' : 'ascending'
      } else {
        sort.value = { prop, order: 'ascending' }
      }
      pagination.number = 0
      loadCoupons()
    }

    // 필터 초기화
    const resetFilters = () => {
      Object.assign(filters, {
        status: '',
        discountType: '',
        code: '',
        name: ''
      })
      pagination.number = 0
      loadCoupons()
    }

    // 유틸리티 함수들
    const formatDate = (dateString) => {
      if (!dateString) return '-'
      const date = new Date(dateString)
      return date.toLocaleDateString('ko-KR') + ' ' + 
             date.toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' })
    }

    const formatDateTimeLocal = (dateString) => {
      if (!dateString) return ''
      const date = new Date(dateString)
      return date.toISOString().slice(0, 16)
    }

    const getStatusText = (status) => {
      const statusMap = {
        'ACTIVE': '활성',
        'INACTIVE': '비활성'
      }
      return statusMap[status] || status
    }

    // 컴포넌트 마운트 시 데이터 로드
    onMounted(() => {
      loadCoupons()
      loadStats()
    })

    return {
      // 반응형 데이터
      coupons,
      stats,
      loading,
      showCreateModal,
      showEditModal,
      filters,
      pagination,
      couponForm,
      
      // 함수들
      loadCoupons,
      createCoupon,
      updateCoupon,
      editCoupon,
      deleteCoupon,
      toggleCouponStatus,
      closeModals,
      goToPage,
      resetFilters,
      debounceSearch,
      formatDate,
      getStatusText,
      getDiscountDisplay,
      sort,
      sortBy
    }
  }
}
</script>
<style scoped src="@/assets/css/admin/coupon-management.css"></style>
<style scoped>
/* 강제 2열 카드 배치 (컴포넌트 로컬 우선 적용) */
.coupon-management .stats-cards {
  display: grid !important;
  grid-template-columns: repeat(2, minmax(0, 1fr)) !important;
  gap: 20px !important;
}

/* 카드가 컬럼 너비를 꽉 채우도록 */
.coupon-management .stat-card {
  width: 100% !important;
}

/* 아주 작은 화면에서만 1열로 */
@media (max-width: 640px) {
  .coupon-management .stats-cards {
    grid-template-columns: 1fr !important;
  }
}
</style>