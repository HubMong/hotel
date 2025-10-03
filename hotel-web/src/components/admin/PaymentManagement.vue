<template>
  <div class="payment-management">
    <div class="page-header">
      <h1>결제 관리</h1>
      <p class="page-description">결제 내역을 모니터링하고 환불을 처리합니다.</p>
    </div>
    <div class="page-toolbar">
      <div class="actions">
        <button class="btn" @click="filterDrawer = true">필터</button>
        <button class="btn btn-primary" :disabled="loading" @click="loadPayments(0)">새로고침</button>
        <div class="dropdown">
          <button class="btn" @click="showColumnMenu = !showColumnMenu">컬럼</button>
          <div class="dropdown-menu" v-if="showColumnMenu">
            <div class="dropdown-item" v-for="col in toggleableColumns" :key="col.key">
              <label>
                <input type="checkbox" :value="col.key" v-model="visibleKeys" /> {{ col.label }}
              </label>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 필터 패널 -->
    <div class="side-panel" v-if="filterDrawer">
      <div class="side-panel-header">
        <strong>결제 필터</strong>
        <button class="btn" @click="filterDrawer = false">닫기</button>
      </div>
      <div class="side-panel-body">
        <div class="field">
          <label>호텔명</label>
          <input v-model="filters.hotelName" placeholder="호텔명" @keyup.enter="applyFilters" />
        </div>
        <div class="field">
          <label>고객명</label>
          <input v-model="filters.userName" placeholder="고객명" @keyup.enter="applyFilters" />
        </div>
        <div class="field">
          <label>상태</label>
          <select v-model="filters.status">
            <option value="">전체</option>
            <option v-for="s in statusOptions" :key="s" :value="s">{{ s }}</option>
          </select>
        </div>
      </div>
      <div class="side-panel-footer">
        <button class="btn btn-primary" @click="applyFilters">적용</button>
        <button class="btn" @click="resetFilters">초기화</button>
      </div>
    </div>

    <!-- 로딩 상태 -->
    <div v-if="loading" class="loading-state">
      <div class="spinner"></div>
      <p>결제 데이터를 불러오는 중...</p>
    </div>

    <!-- 빈 상태 -->
    <div v-else-if="!loading && payments.items.length === 0" class="empty-state">
      <p>조회된 결제 내역이 없습니다.</p>
    </div>

    <!-- 결제 목록 테이블 -->
    <div v-else class="table-section">
      <table class="payments-table">
        <thead>
          <tr>
            <th v-if="isVisible('id')">
              ID
              <button class="sort-btn" @click.stop="sortBy('id')" style="background:none;border:none;padding:0;margin-left:4px;vertical-align:top;">
                <svg width="12" height="24" viewBox="0 0 12 24" xmlns="http://www.w3.org/2000/svg" style="display:inline-block;vertical-align:top;">
                  <polygon points="6,2 2,8 10,8" :fill="sort.prop==='id' && sort.order==='ascending' ? '#222' : '#ccc'" />
                  <polygon points="6,22 2,16 10,16" :fill="sort.prop==='id' && sort.order==='descending' ? '#222' : '#ccc'" />
                </svg>
              </button>
            </th>
            <th v-if="isVisible('reservationNumber')">예약번호</th>
            <th v-if="isVisible('hotelName')">호텔명</th>
            <th v-if="isVisible('userName')">고객명</th>
            <th v-if="isVisible('amount')" class="text-right">
              금액
              <button class="sort-btn" @click.stop="sortBy('amount')" style="background:none;border:none;padding:0;margin-left:4px;vertical-align:top;">
                <svg width="12" height="24" viewBox="0 0 12 24" xmlns="http://www.w3.org/2000/svg" style="display:inline-block;vertical-align:top;">
                  <polygon points="6,2 2,8 10,8" :fill="sort.prop==='amount' && sort.order==='ascending' ? '#222' : '#ccc'" />
                  <polygon points="6,22 2,16 10,16" :fill="sort.prop==='amount' && sort.order==='descending' ? '#222' : '#ccc'" />
                </svg>
              </button>
            </th>
            <th v-if="isVisible('method')">결제수단</th>
            <th v-if="isVisible('status')">
              상태
              <button class="sort-btn" @click.stop="sortBy('status')" style="background:none;border:none;padding:0;margin-left:4px;vertical-align:top;">
                <svg width="12" height="24" viewBox="0 0 12 24" xmlns="http://www.w3.org/2000/svg" style="display:inline-block;vertical-align:top;">
                  <polygon points="6,2 2,8 10,8" :fill="sort.prop==='status' && sort.order==='ascending' ? '#222' : '#ccc'" />
                  <polygon points="6,22 2,16 10,16" :fill="sort.prop==='status' && sort.order==='descending' ? '#222' : '#ccc'" />
                </svg>
              </button>
            </th>
            <th v-if="isVisible('createdAt')">
              결제일시
              <button class="sort-btn" @click.stop="sortBy('createdAt')" style="background:none;border:none;padding:0;margin-left:4px;vertical-align:top;">
                <svg width="12" height="24" viewBox="0 0 12 24" xmlns="http://www.w3.org/2000/svg" style="display:inline-block;vertical-align:top;">
                  <polygon points="6,2 2,8 10,8" :fill="sort.prop==='createdAt' && sort.order==='ascending' ? '#222' : '#ccc'" />
                  <polygon points="6,22 2,16 10,16" :fill="sort.prop==='createdAt' && sort.order==='descending' ? '#222' : '#ccc'" />
                </svg>
              </button>
            </th>
            <th v-if="isVisible('cancelledAt')">취소일시</th>
            <th>작업</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in payments.items" :key="row.id">
            <td v-if="isVisible('id')">{{ row.id }}</td>
            <td v-if="isVisible('reservationNumber')"><span class="mono">{{ row.reservationNumber }}</span></td>
            <td v-if="isVisible('hotelName')">{{ row.hotelName }}</td>
            <td v-if="isVisible('userName')">{{ row.userName }}</td>
            <td v-if="isVisible('amount')" class="text-right">{{ formatCurrency(row.amount) }}</td>
            <td v-if="isVisible('method')">{{ row.method }}</td>
            <td v-if="isVisible('status')"><span class="badge" :class="statusBadgeClass(row.status)">{{ row.status }}</span></td>
            <td v-if="isVisible('createdAt')">{{ formatDateTime(row.createdAt) }}</td>
            <td v-if="isVisible('cancelledAt')">{{ formatDateTime(row.cancelledAt) }}</td>
            <td>
              <button 
                class="btn btn-danger btn-sm" 
                :class="{ 'btn-disabled': row.status !== 'COMPLETED' }"
                :disabled="refundingId === row.id"
                @click="confirmRefund(row)"
              >
                {{ refundingId === row.id ? '처리중...' : '환불/취소' }}
              </button>
            </td>
          </tr>
        </tbody>
      </table>

      <div class="pagination">
        <div class="left">
          <label>페이지 크기
            <select v-model.number="payments.size" @change="onSizeChange(payments.size)">
              <option :value="10">10</option>
              <option :value="20">20</option>
              <option :value="50">50</option>
            </select>
          </label>
        </div>
        <div class="right">
          <button class="btn" :disabled="payments.page===0" @click="onPageChange(payments.page)">이전</button>
          <span class="page-info">{{ payments.page + 1 }} / {{ payments.totalPages }} · 총 {{ payments.totalElements }}건</span>
          <button class="btn" :disabled="payments.page===payments.totalPages-1" @click="onPageChange(payments.page+2)">다음</button>
        </div>
      </div>
    </div>

  </div>
</template>

<script>
import { ref, reactive, onMounted, computed } from 'vue'
import api from '../../api/http'

export default {
  name: 'PaymentManagement',
  setup() {
    const loading = ref(false)
    const filterDrawer = ref(false)
    const refundingId = ref(null)
    const statusOptions = ['PENDING', 'COMPLETED', 'CANCELLED', 'FAILED']
    const filters = reactive({ hotelName: '', userName: '', status: '' })
    const payments = reactive({ items: [], page: 0, totalPages: 0, totalElements: 0, size: 20 })
    const showColumnMenu = ref(false)

    // 컬럼 정의 및 가시성 컨트롤
    const allColumns = [
      { key: 'id', label: 'ID' },
      { key: 'reservationNumber', label: '예약번호' },
      { key: 'hotelName', label: '호텔명' },
      { key: 'userName', label: '고객명' },
      { key: 'amount', label: '금액' },
      { key: 'method', label: '결제수단' },
      { key: 'status', label: '상태' },
      { key: 'createdAt', label: '결제일시' },
      { key: 'cancelledAt', label: '취소일시' }
    ]
    const visibleKeys = ref(allColumns.map(c => c.key))
    const toggleableColumns = computed(() => allColumns)
    const isVisible = (key) => visibleKeys.value.includes(key)

    // 정렬 상태
    const sort = ref({ prop: 'createdAt', order: 'descending' })

    const loadPayments = async (page = 0) => {
      loading.value = true
      try {
        const paramsObj = {}
        if (filters.hotelName) paramsObj.hotelName = filters.hotelName
        if (filters.userName) paramsObj.userName = filters.userName
        if (filters.status) paramsObj.status = filters.status
        paramsObj.page = page
        paramsObj.size = payments.size
        // 정렬 파라미터
        const sortParam = toApiSort(sort.value)
        if (sortParam) paramsObj.sort = sortParam

        console.log('결제 목록 요청 파라미터:', paramsObj)

        const res = await api.get('/admin/payments', { params: paramsObj })
        console.log('결제 목록 전체 응답:', res)
        console.log('결제 목록 data:', res.data)
        console.log('결제 목록 data.data:', res.data?.data)
        
        const responseData = res?.data
        let data = {}
        
        // 응답 구조 확인: res.data 또는 res.data.data
        if (responseData?.content && Array.isArray(responseData.content)) {
          // Spring Page 객체가 직접 반환되는 경우
          data = responseData
          console.log('직접 Page 구조 사용')
        } else if (responseData?.data?.content && Array.isArray(responseData.data.content)) {
          // ApiResponse로 감싸진 경우
          data = responseData.data
          console.log('ApiResponse 감싸진 구조 사용')
        } else {
          console.warn('예상하지 못한 응답 구조:', responseData)
          data = {}
        }
        
        console.log('최종 data:', data)
        console.log('content 배열:', data.content)
        
        // PaymentSummaryDto 배열을 UI 형태로 변환
        const items = Array.isArray(data.content) ? data.content : []
        console.log('매핑 전 items:', items)
        
        if (items.length > 0) {
          console.log('첫 번째 item 구조:', items[0])
        }
        
        payments.items = items.map(payment => {
          console.log('매핑 중인 payment:', payment)
          return {
            id: payment.paymentId,
            reservationNumber: payment.transactionId || '-',
            hotelName: payment.hotelName || '-',
            userName: payment.userName || '-',
            amount: payment.totalPrice || 0,
            method: payment.paymentMethod || '-',
            status: payment.paymentStatus || 'UNKNOWN',
            createdAt: payment.createdAt,
            cancelledAt: payment.canceledAt
          }
        })
        
        payments.page = (data.page ?? data.number ?? 0)
        payments.totalPages = data.totalPages || 0
        payments.totalElements = data.totalElements || 0
        
        console.log('결제 목록 매핑 완료:', payments.items.length, '건')
        console.log('매핑 후 첫 번째 item:', payments.items[0])
        
      } catch (err) {
        console.error('결제 목록 로딩 오류:', err)
        console.error('에러 응답:', err?.response)
        console.error('에러 데이터:', err?.response?.data)
        const status = err?.response?.status
        const message = err?.response?.data?.message || err.message
        if (status === 401) {
          alert('로그인이 필요합니다. 다시 로그인해 주세요.')
        } else if (status === 403) {
          alert('접근 권한이 없습니다. 관리자 계정으로 로그인하세요.')
        } else {
          alert(`결제 목록을 불러오지 못했습니다: ${message}`)
        }
        payments.items = []
        payments.page = 0
        payments.totalPages = 0
        payments.totalElements = 0
      } finally {
        loading.value = false
      }
    }

    const sortBy = (prop) => {
      if (sort.value.prop === prop) {
        sort.value.order = sort.value.order === 'ascending' ? 'descending' : 'ascending'
      } else {
        sort.value = { prop, order: 'ascending' }
      }
      loadPayments(0)
    }

    const sortIcon = (prop) => {
      if (sort.value.prop !== prop) return ''
      return sort.value.order === 'ascending' ? 'asc' : 'desc'
    }

    const toApiSort = ({ prop, order }) => {
      if (!prop || !order) return null
      const dir = order === 'ascending' ? 'asc' : 'desc'
      // 매핑 가능한 필드만 허용
      const allowed = new Set(['id', 'createdAt', 'amount', 'status'])
      if (!allowed.has(prop)) return null
      return `${prop},${dir}`
    }
    const onPageChange = (page1Based) => {
      const target = Math.max(0, Math.min((page1Based - 1), payments.totalPages - 1))
      loadPayments(target)
    }

    const onSizeChange = (size) => {
      payments.size = size
      loadPayments(0)
    }

    const resetFilters = () => {
      filters.hotelName = ''
      filters.userName = ''
      filters.status = ''
      loadPayments(0)
    }

    const applyFilters = () => {
      filterDrawer.value = false
      loadPayments(0)
    }

    const confirmRefund = async (row) => {
      if (!row) {
        alert('결제 정보가 없습니다.')
        return
      }
      
      // 상태별 상세한 안내 메시지
      if (row.status !== 'COMPLETED') {
        let reason = ''
        switch (row.status) {
          case 'PENDING':
            reason = '아직 결제가 완료되지 않았습니다. 결제 대기 중인 건은 환불할 수 없습니다.'
            break
          case 'CANCELLED':
            reason = '이미 취소된 결제입니다. 취소된 결제는 다시 환불할 수 없습니다.'
            break
          case 'FAILED':
            reason = '결제가 실패한 건입니다. 실패한 결제는 환불할 수 없습니다.'
            break
          default:
            reason = `현재 상태(${row.status})에서는 환불이 불가능합니다.`
        }
        alert(`환불할 수 없습니다.\n\n사유: ${reason}`)
        return
      }
      
      if (!confirm(`결제 ID ${row.id}를 환불/취소하시겠습니까?`)) return
      
      refundingId.value = row.id
      console.log('환불 요청 시작:', row.id)
      
      try {
        const response = await api.put(`/admin/payments/${row.id}/refund`)
        console.log('환불 응답:', response)
        alert('환불이 처리되었습니다.')
        loadPayments(payments.page)
      } catch (e) {
        console.error('환불 처리 오류:', e)
        console.error('에러 응답:', e?.response)
        const status = e?.response?.status
        const message = e?.response?.data?.message || e.message
        
        if (status === 401) {
          alert('로그인이 필요합니다. 다시 로그인해 주세요.')
        } else if (status === 403) {
          alert('접근 권한이 없습니다. 관리자 계정으로 로그인하세요.')
        } else if (status === 400) {
          alert(`잘못된 요청입니다: ${message}`)
        } else if (status === 404) {
          alert('결제 정보를 찾을 수 없습니다.')
        } else if (status === 409) {
          alert(`환불할 수 없습니다: ${message}`)
        } else {
          alert(`환불 처리 중 오류가 발생했습니다: ${message}`)
        }
      } finally {
        refundingId.value = null
      }
    }

    const formatCurrency = (n) => {
      if (n == null) return '-'
      return new Intl.NumberFormat('ko-KR', { style: 'currency', currency: 'KRW', maximumFractionDigits: 0 }).format(n)
    }

    const formatDateTime = (v) => {
      if (!v) return '-'
      const d = new Date(v)
      return d.toLocaleString('ko-KR')
    }

    const statusBadgeClass = (s) => {
      const m = {
        COMPLETED: 'badge-success',
        PENDING: 'badge-warning',
        CANCELLED: 'badge-danger',
        FAILED: 'badge-danger'
      }
      return m[s] || 'badge-default'
    }

    onMounted(() => {
      loadPayments(0)
    })

    return { 
      loading, 
      filterDrawer, 
      refundingId, 
      showColumnMenu,
      statusOptions, 
      filters, 
      payments,
      visibleKeys, 
      toggleableColumns, 
      isVisible,
      sort, 
      sortBy, 
      sortIcon, 
      onPageChange, 
      onSizeChange,
      loadPayments, 
      confirmRefund, 
      applyFilters, 
      resetFilters,
      formatCurrency, 
      formatDateTime, 
      statusBadgeClass
    }
  }
}
</script>

<style scoped src="@/assets/css/admin/payment-management.css"></style>