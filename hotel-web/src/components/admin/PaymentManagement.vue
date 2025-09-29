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

    <!-- 결과 테이블 -->
    <div class="table-section" v-if="payments.items.length">
      <table class="payments-table">
        <thead>
          <tr>
            <th v-if="isVisible('id')" @click="sortBy('id')">ID <span class="sort" :class="sortIcon('id')"></span></th>
            <th v-if="isVisible('reservationNumber')">예약번호</th>
            <th v-if="isVisible('hotelName')">호텔명</th>
            <th v-if="isVisible('userName')">고객명</th>
            <th v-if="isVisible('amount')" class="text-right" @click="sortBy('amount')">금액 <span class="sort" :class="sortIcon('amount')"></span></th>
            <th v-if="isVisible('method')">결제수단</th>
            <th v-if="isVisible('status')" @click="sortBy('status')">상태 <span class="sort" :class="sortIcon('status')"></span></th>
            <th v-if="isVisible('paidAt')" @click="sortBy('paidAt')">결제일시 <span class="sort" :class="sortIcon('paidAt')"></span></th>
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
            <td v-if="isVisible('paidAt')">{{ formatDateTime(row.paidAt) }}</td>
            <td v-if="isVisible('cancelledAt')">{{ formatDateTime(row.cancelledAt) }}</td>
            <td>
              <button 
                class="btn btn-danger btn-sm" 
                :disabled="row.status !== 'PAID' || refundingId === row.id"
                @click="confirmRefund(row)"
              >
                {{ refundingId === row.id ? '처리중...' : '환불' }}
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
          <button class="btn" :disabled="payments.page >= payments.totalPages-1" @click="onPageChange(payments.page + 2)">다음</button>
        </div>
      </div>
    </div>

    <!-- 빈 상태 -->
    <div v-else class="empty">
      <h3>표시할 결제 내역이 없습니다</h3>
      <p>검색 조건을 변경해 보세요.</p>
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
    const statusOptions = ['PENDING', 'PAID', 'CANCELLED', 'FAILED', 'REFUNDED', 'PARTIAL_REFUNDED']
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
      { key: 'paidAt', label: '결제일시' },
      { key: 'cancelledAt', label: '취소일시' }
    ]
    const visibleKeys = ref(allColumns.map(c => c.key))
    const toggleableColumns = computed(() => allColumns)
    const isVisible = (key) => visibleKeys.value.includes(key)

    // 정렬 상태
    const sort = ref({ prop: 'paidAt', order: 'descending' })

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
        console.log('결제 목록 응답:', res.data)
        
        const data = res?.data?.data || {}
        
        // PaymentSummaryDto 배열을 UI 형태로 변환
        const items = Array.isArray(data.content) ? data.content : []
        payments.items = items.map(payment => ({
          id: payment.paymentId,
          reservationNumber: payment.transactionId || '-',
          hotelName: payment.hotelName || '-',
          userName: payment.userName || '-',
          amount: payment.totalPrice || 0,
          method: payment.paymentMethod || '-',
          status: payment.paymentStatus || 'UNKNOWN',
          paidAt: payment.paymentCreatedAt,
          cancelledAt: payment.refundedAt
        }))
        
        payments.page = (data.page ?? data.number ?? 0)
        payments.totalPages = data.totalPages || 0
        payments.totalElements = data.totalElements || 0
        
        console.log('결제 목록 매핑 완료:', payments.items.length, '건')
        
      } catch (err) {
        console.error('결제 목록 로딩 오류:', err)
        const status = err?.response?.status
        if (status === 401) {
          alert('로그인이 필요합니다. 다시 로그인해 주세요.')
        } else if (status === 403) {
          alert('접근 권한이 없습니다. 관리자 계정으로 로그인하세요.')
        } else {
          alert('결제 목록을 불러오지 못했습니다.')
        }
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
      const allowed = new Set(['id', 'paidAt', 'amount', 'status'])
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
      if (!row || row.status !== 'PAID') return
      if (!confirm('이 결제를 환불하시겠습니까?')) return
      refundingId.value = row.id
      try {
        await api.put(`/admin/payments/${row.id}/refund`)
        alert('환불이 처리되었습니다.')
        loadPayments(payments.page)
      } catch (e) {
        const status = e?.response?.status
        if (status === 401) {
          alert('로그인이 필요합니다. 다시 로그인해 주세요.')
        } else if (status === 403) {
          alert('접근 권한이 없습니다. 관리자 계정으로 로그인하세요.')
        } else {
          alert('환불 처리 중 오류가 발생했습니다.')
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

    onMounted(() => loadPayments(0))

    const statusBadgeClass = (s) => {
      const m = {
        PAID: 'badge-success',
        PENDING: 'badge-warning',
        CANCELLED: 'badge-danger',
        FAILED: 'badge-danger',
        REFUNDED: 'badge-info',
        PARTIAL_REFUNDED: 'badge-info'
      }
      return m[s] || 'badge-default'
    }

    return { 
      loading, filterDrawer, refundingId, showColumnMenu,
      statusOptions, filters, payments,
      visibleKeys, toggleableColumns, isVisible,
      sortBy, sortIcon, onPageChange, onSizeChange,
      loadPayments, confirmRefund, applyFilters, resetFilters,
      formatCurrency, formatDateTime, statusBadgeClass
    }
  }
}
</script>

<style scoped src="@/assets/css/admin/payment-management.css"></style>