<template>
  <div class="payment-management">
    <header class="page-header">
      <div class="page-title-group">
        <h1>결제 관리</h1>
        <p class="page-description">결제 현황과 환불, 영수증 발급, 고객 커뮤니케이션을 한 화면에서 처리합니다.</p>
      </div>
      <div class="header-actions">
        <button class="btn" type="button" @click.stop="filterDrawer = true">고급 필터</button>
        <div class="dropdown" @click.stop>
          <button class="btn" type="button" @click="presetMenuOpen = !presetMenuOpen">필터 프리셋</button>
          <div v-if="presetMenuOpen" class="dropdown-menu dropdown-presets">
            <div v-if="savedPresets.length === 0" class="dropdown-empty">저장된 프리셋이 없습니다.</div>
            <div v-for="preset in savedPresets" :key="preset.id" class="dropdown-item preset-item">
              <span class="preset-name" @click="applyPreset(preset)">{{ preset.name }}</span>
              <button class="icon-button" type="button" @click.stop="deletePreset(preset.id)">×</button>
            </div>
          </div>
        </div>
        <div class="dropdown" @click.stop>
          <button class="btn" type="button" @click="columnMenuOpen = !columnMenuOpen">컬럼</button>
          <div v-if="columnMenuOpen" class="dropdown-menu dropdown-columns">
            <label v-for="column in toggleableColumns" :key="column.key" class="dropdown-item">
              <input type="checkbox" :value="column.key" v-model="visibleKeys" />
              {{ column.label }}
            </label>
          </div>
        </div>
        <button class="btn btn-secondary" type="button" @click="restoreDefaultColumns">기본 컬럼</button>
  <button class="btn btn-primary" type="button" :disabled="loading" @click="loadPaymentsAndDashboard(0)">새로고침</button>
      </div>
    </header>

    <section v-if="metrics" class="metrics-grid">
      <article class="metric-card">
        <span class="metric-label">오늘 결제액</span>
        <span class="metric-value">{{ formatCurrency(metrics.totalTodayAmount) }}</span>
        <span class="metric-sub" :class="trendClass(metrics.dayOverDayAmountRate)">
          {{ formatTrend(metrics.dayOverDayAmountRate) }}
        </span>
      </article>
      <article class="metric-card">
        <span class="metric-label">오늘 결제 건수</span>
        <span class="metric-value">{{ formatNumber(metrics.totalTodayCount) }}건</span>
        <span class="metric-sub">평균 결제금액 {{ formatCurrency(metrics.avgPaymentAmount) }}</span>
      </article>
      <article class="metric-card">
        <span class="metric-label">오늘 환불율</span>
        <span class="metric-value">{{ formatRate(metrics.refundRateToday) }}</span>
      </article>
      <article class="metric-card status-card">
        <span class="metric-label">결제 상태 분포</span>
        <ul class="status-summary">
          <li v-for="item in metricsStatusSummary" :key="item.label" class="status-item">
            <span class="status-dot" :class="item.className"></span>
            <span class="status-label">{{ item.label }}</span>
            <strong class="status-count">{{ formatNumber(item.value) }}</strong>
          </li>
        </ul>
      </article>
    </section>

    <section class="toolbar-section">
      <div class="toolbar-left">
        <div class="quick-filters">
          <span class="quick-filters-label">빠른 필터</span>
          <button
            v-for="quick in quickFilters"
            :key="quick.key"
            type="button"
            class="quick-filter-button"
            :class="{ active: filters.quickFilter === quick.key }"
            @click="applyQuickFilter(quick.key)"
          >
            {{ quick.label }}
          </button>
          <button
            v-if="filters.quickFilter"
            type="button"
            class="quick-filter-button clear"
            @click="clearQuickFilter"
          >
            초기화
          </button>
        </div>
        <div class="inline-fields">
          <div class="field-group">
            <label>상태</label>
            <select v-model="filters.status">
              <option value="">전체</option>
              <option v-for="status in statusOptions" :key="status" :value="status">{{ status }}</option>
            </select>
          </div>
          <div class="field-group">
            <label>기간</label>
            <FlatPickr
              v-model="filters.dateRange"
              :config="flatpickrDateTimeConfig"
              placeholder="검색 기간을 선택하세요"
            />
          </div>
          <div class="field-group field-actions">
            <button class="btn btn-outline" type="button" @click="applyFilters">검색</button>
            <button class="btn btn-outline" type="button" @click="resetFilters">초기화</button>
          </div>
        </div>
      </div>
      <div class="toolbar-right">
        <div class="export-panel">
          <select v-model="exportForm.format">
            <option v-for="format in exportFormats" :key="format.value" :value="format.value">
              {{ format.label }}
            </option>
          </select>
          <select v-model="exportForm.scope">
            <option v-for="scope in exportScopes" :key="scope.value" :value="scope.value">
              {{ scope.label }}
            </option>
          </select>
          <div class="dropdown" @click.stop>
            <button class="btn" type="button" @click="exportColumnMenuOpen = !exportColumnMenuOpen">
              내보내기 컬럼
            </button>
            <div v-if="exportColumnMenuOpen" class="dropdown-menu dropdown-columns">
              <label v-for="column in toggleableColumns" :key="`export-${column.key}`" class="dropdown-item">
                <input type="checkbox" :value="column.key" v-model="exportColumns" />
                {{ column.label }}
              </label>
            </div>
          </div>
          <button class="btn btn-primary" type="button" :disabled="exporting" @click="startExport">
            {{ exporting ? '요청 중...' : '내보내기' }}
          </button>
        </div>
        <div class="selection-indicator">
          <span>선택 {{ selectedRowCount }}건</span>
          <button v-if="selectedRowCount" class="btn btn-link" type="button" @click="clearSelection">
            선택 해제
          </button>
        </div>
      </div>
    </section>

    <section v-if="activeFilters.length" class="active-filters">
      <span v-for="chip in activeFilters" :key="chip" class="filter-chip">{{ chip }}</span>
    </section>

    <section
      v-if="exportJob.status"
      class="export-status"
      :data-status="exportJob.status"
    >
      <div class="status-header">
        <h2>내보내기 상태</h2>
        <div class="status-actions">
          <button class="btn btn-outline" type="button" @click="checkExportStatus" v-if="exportJob.status !== 'COMPLETED'">
            새로고침
          </button>
          <button
            class="btn btn-primary"
            type="button"
            v-if="exportJob.status === 'COMPLETED'"
            @click="downloadExport(exportJob.jobId)"
          >
            다운로드
          </button>
        </div>
      </div>
      <p class="status-summary-text">
        {{ exportStatusLabel }}
        <span v-if="exportJob.message" class="status-message">· {{ exportJob.message }}</span>
      </p>
      <div v-if="exportJob.totalRows" class="progress-wrapper">
        <div class="progress-bar">
          <div class="progress-value" :style="{ width: exportProgress + '%' }"></div>
        </div>
        <span class="progress-label">
          {{ formatNumber(exportJob.processedRows) }} / {{ formatNumber(exportJob.totalRows) }} 건
        </span>
      </div>
      <p v-if="exportJob.jobId" class="status-footnote">작업 ID: {{ exportJob.jobId }}</p>
    </section>

    <div v-if="loading" class="loading-state">
      <div class="spinner"></div>
      <p>결제 데이터를 불러오는 중입니다...</p>
    </div>

    <div v-else-if="payments.items.length === 0" class="empty-state">
      <p>조건에 맞는 결제 내역이 없습니다.</p>
    </div>

    <section v-else class="table-section">
      <div class="table-toolbar">
        <span>총 {{ formatNumber(payments.totalElements) }}건 · 페이지 {{ payments.page + 1 }} / {{ payments.totalPages }}</span>
      </div>
      <table class="payments-table">
        <thead>
          <tr>
            <th class="selection-cell">
              <input
                type="checkbox"
                :checked="allRowsSelected"
                @change="toggleSelectAll($event.target.checked)"
              />
            </th>
            <th
              v-for="column in visibleColumns"
              :key="column.key"
              :class="['sortable-header', column.align === 'right' ? 'text-right' : 'text-left']"
            >
              <button
                v-if="column.sortable"
                class="sort-header"
                type="button"
                @click="sortBy(column.key)"
              >
                <span>{{ column.label }}</span>
                <span class="sort-indicator">{{ sortIndicator(column.key) }}</span>
              </button>
              <span v-else>{{ column.label }}</span>
            </th>
            <th class="actions-cell">작업</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="row in payments.items"
            :key="row.id"
            :class="{ selected: selectedPayment && selectedPayment.id === row.id }"
            @click="selectPayment(row)"
          >
            <td class="selection-cell">
              <input
                type="checkbox"
                :checked="isRowSelected(row.id)"
                @change.stop="toggleRowSelection(row.id, $event.target.checked)"
              />
            </td>
              <td
                v-for="column in visibleColumns"
                :key="`${row.id}-${column.key}`"
                :class="['table-cell', column.align === 'right' ? 'text-right' : 'text-left', 'table-cell-horizontal']"
              >
                <!-- 거래번호: 앞 6자리 + ... -->
                <template v-if="column.key === 'transactionId'">
                  <span v-if="row[column.key] && row[column.key].length > 6">
                    {{ row[column.key].slice(0, 6) + '...' }}
                  </span>
                  <span v-else>
                    {{ row[column.key] ?? '-' }}
                  </span>
                </template>
                <!-- 호텔명/고객명: 2줄로 표시 -->
                <template v-else-if="column.key === 'hotelName'">
                  <span class="cell-multiline">
                    {{ row[column.key] }}<br />
                    <span class="cell-sub">{{ row['hotelId'] ? 'ID: ' + row['hotelId'] : '' }}</span>
                  </span>
                </template>
                <template v-else-if="column.key === 'userName'">
                  <span class="cell-multiline">
                    {{ row[column.key] }}<br />
                    <span class="cell-sub">{{ row['userEmail'] }}</span>
                  </span>
                </template>
                <!-- 영수증 -->
                <template v-else-if="column.key === 'receiptUrl'">
                  <button
                    class="btn btn-link"
                    type="button"
                    @click.stop="downloadReceipt(row)"
                    :disabled="!row.receiptUrl"
                  >
                    {{ row.receiptUrl ? '보기' : '없음' }}
                  </button>
                </template>
                <!-- 금액 -->
                <template v-else-if="column.type === 'currency'">
                  {{ formatCurrency(row[column.key]) }}
                </template>
                <!-- 날짜 -->
                <template v-else-if="column.type === 'datetime'">
                  {{ formatDateTime(row[column.key]) }}
                </template>
                <!-- 기타 -->
                <template v-else>
                  {{ row[column.key] ?? '-' }}
                </template>
              </td>
            <td class="actions-cell">
              <div class="row-actions">
                <button
                  class="btn btn-sm"
                  type="button"
                  @click.stop="confirmRefund(row)"
                  :disabled="!canRefund(row)"
                >
                  환불
                </button>
                <button
                  class="btn btn-sm"
                  type="button"
                  @click.stop="downloadReceipt(row)"
                  :disabled="!row.receiptUrl"
                >
                  영수증
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>

      <div class="pagination">
        <div class="left">
          <label>
            페이지 크기
            <select v-model.number="payments.size" @change="onSizeChange">
              <option :value="10">10</option>
              <option :value="20">20</option>
              <option :value="50">50</option>
            </select>
          </label>
        </div>
        <div class="right">
          <button class="btn" type="button" :disabled="payments.page === 0" @click="onPageChange(payments.page)">
            이전
          </button>
          <span class="page-info">{{ payments.page + 1 }} / {{ Math.max(1, payments.totalPages) }}</span>
          <button
            class="btn"
            type="button"
            :disabled="payments.page >= payments.totalPages - 1"
            @click="onPageChange(payments.page + 2)"
          >
            다음
          </button>
        </div>
      </div>
    </section>

    <section v-if="selectedPayment" class="detail-panel">
      <header class="detail-header">
        <div>
          <h2>선택한 결제 상세</h2>
          <p>결제 ID: {{ selectedPayment.id }} · 거래번호 {{ selectedPayment.transactionId || '-' }}</p>
        </div>
        <button class="btn btn-outline" type="button" @click="selectedPayment = null">닫기</button>
      </header>
      <div class="detail-grid">
        <div class="detail-item">
          <span class="detail-label">호텔</span>
          <span class="detail-value">{{ selectedPayment.hotelName }}</span>
        </div>
        <div class="detail-item">
          <span class="detail-label">고객</span>
          <span class="detail-value">{{ selectedPayment.userName }} ({{ selectedPayment.userEmail || '이메일 없음' }})</span>
        </div>
        <div class="detail-item">
          <span class="detail-label">총 결제금액</span>
          <span class="detail-value">{{ formatCurrency(selectedPayment.totalPrice) }}</span>
        </div>
        <div class="detail-item">
          <span class="detail-label">결제수단 / 상태</span>
          <span class="detail-value">
            {{ selectedPayment.paymentMethod }} ·
            <span class="badge" :class="statusBadgeClass(selectedPayment.paymentStatus)">
              {{ selectedPayment.paymentStatus }}
            </span>
          </span>
        </div>
        <div class="detail-item">
          <span class="detail-label">결제일시</span>
          <span class="detail-value">{{ formatDateTime(selectedPayment.createdAt) }}</span>
        </div>
      </div>
      <div class="detail-actions">
        <button
          class="btn btn-primary"
          type="button"
          @click="confirmRefund(selectedPayment)"
          :disabled="!canRefund(selectedPayment)"
        >
          환불
        </button>
        <button class="btn btn-outline" type="button" @click="downloadReceipt(selectedPayment)" :disabled="!selectedPayment.receiptUrl">
          영수증 보기
        </button>
      </div>
      <div v-if="selectedPayment.memo" class="detail-memo">
        <h3>메모</h3>
        <p>{{ selectedPayment.memo }}</p>
      </div>
    </section>

    <section class="settlement-section">
      <header>
        <h2>정산 보고서</h2>
        <p>기간별·호텔별 정산 금액과 플랫폼 수수료를 확인하세요.</p>
      </header>
      <div class="settlement-controls">
        <div class="field-group">
          <label>정산 기간</label>
          <FlatPickr
            v-model="settlementForm.dateRange"
            :config="flatpickrDateConfig"
            placeholder="정산 기간을 선택하세요"
          />
        </div>
        <div class="field-group">
          <label>호텔 선택</label>
          <select v-model="settlementForm.hotelId">
            <option value="">전체 호텔</option>
            <option v-for="hotel in settlementHotels" :key="hotel.id" :value="String(hotel.id)">
              {{ hotel.name }} (ID: {{ hotel.id }})
            </option>
          </select>
        </div>
        <div class="field-group field-actions">
          <label>&nbsp;</label>
          <button
            class="btn btn-primary"
            type="button"
            :disabled="settlementForm.loading"
            @click="generateSettlementReport"
          >
            {{ settlementForm.loading ? '생성 중...' : '보고서 생성' }}
          </button>
        </div>
      </div>
      <div v-if="settlementForm.report" class="settlement-report">
        <table class="report-table">
          <thead>
            <tr>
              <th>호텔</th>
              <th class="text-right">총 매출</th>
              <th class="text-right">VAT</th>
              <th class="text-right">플랫폼 수수료</th>
              <th class="text-right">정산 금액</th>
              <th class="text-right">예약 건수</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="line in settlementForm.report.lines" :key="line.hotelId">
              <td>{{ line.hotelName }}</td>
              <td class="text-right">{{ formatCurrency(line.grossAmount) }}</td>
              <td class="text-right">{{ formatCurrency(line.vat) }}</td>
              <td class="text-right">{{ formatCurrency(line.platformFee) }}</td>
              <td class="text-right">{{ formatCurrency(line.netPayout) }}</td>
              <td class="text-right">{{ formatNumber(line.reservationCount) }}</td>
            </tr>
          </tbody>
          <tfoot>
            <tr>
              <th>합계</th>
              <th class="text-right">{{ formatCurrency(settlementForm.report.totalGross) }}</th>
              <th class="text-right">{{ formatCurrency(settlementForm.report.totalVat) }}</th>
              <th class="text-right">{{ formatCurrency(settlementForm.report.totalPlatformFee) }}</th>
              <th class="text-right">{{ formatCurrency(settlementForm.report.totalNetPayout) }}</th>
              <th></th>
            </tr>
          </tfoot>
        </table>
      </div>
    </section>

    <aside v-if="filterDrawer" class="side-panel" role="dialog" aria-modal="true">
      <div class="side-panel-header">
        <strong>고급 필터</strong>
        <button class="btn" type="button" @click="filterDrawer = false">닫기</button>
      </div>
      <div class="side-panel-body">
        <label class="field">
          <span>호텔명</span>
          <input v-model="filters.hotelName" placeholder="호텔명" />
        </label>
        <label class="field">
          <span>호텔 ID</span>
          <input type="number" v-model="filters.hotelId" min="1" placeholder="호텔 ID" />
        </label>
        <label class="field">
          <span>고객명</span>
          <input v-model="filters.userName" placeholder="고객명" />
        </label>
        <label class="field">
          <span>거래번호</span>
          <input v-model="filters.transactionId" placeholder="거래번호 / 예약번호" />
        </label>
        <div class="field">
          <span>결제수단</span>
          <div class="checkbox-group vertical">
            <label v-for="method in paymentMethodOptions" :key="`drawer-${method.value}`">
              <input type="checkbox" :value="method.value" v-model="filters.methods" />
              {{ method.label }}
            </label>
          </div>
        </div>
        <div class="field">
          <span>금액 범위</span>
          <div class="range-inputs">
            <input type="number" v-model.number="filters.minAmount" placeholder="최소 금액" min="0" />
            <span>~</span>
            <input type="number" v-model.number="filters.maxAmount" placeholder="최대 금액" min="0" />
          </div>
        </div>
        <div class="field">
          <span>기간</span>
          <FlatPickr
            v-model="filters.dateRange"
            :config="flatpickrDateTimeConfig"
          />
        </div>
      </div>
      <div class="side-panel-footer">
        <div class="preset-save">
          <input v-model="newPresetName" placeholder="프리셋 이름" />
          <button class="btn btn-outline" type="button" @click="savePreset">프리셋 저장</button>
        </div>
        <div class="footer-actions">
          <button class="btn" type="button" @click="resetFilters">초기화</button>
          <button class="btn btn-primary" type="button" @click="applyFilters">적용</button>
        </div>
      </div>
    </aside>
  </div>
</template>

<script>
import { ref, reactive, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import FlatPickr from 'vue-flatpickr-component'
import { Korean } from 'flatpickr/dist/l10n/ko.js'
import 'flatpickr/dist/flatpickr.css'
import api from '../../api/http'

const presetStorageKey = 'admin-payment-filter-presets'

const paymentMethodOptions = [
  { value: 'CARD', label: '카드' },
  { value: 'TRANSFER', label: '계좌이체' },
  { value: 'VIRTUAL_ACCOUNT', label: '가상계좌' },
  { value: 'MOBILE', label: '모바일결제' },
  { value: 'KAKAO_PAY', label: '카카오페이' },
  { value: 'NAVER_PAY', label: '네이버페이' },
  { value: 'TOSS_PAY', label: '토스페이' }
]

const statusOptions = ['PENDING', 'COMPLETED', 'PARTIAL_REFUND', 'CANCELLED', 'FAILED']

const quickFilters = [
  { key: 'TODAY', label: '오늘' },
  { key: 'YESTERDAY', label: '어제' },
  { key: 'LAST_7_DAYS', label: '최근 7일' },
  { key: 'LAST_30_DAYS', label: '최근 30일' },
  { key: 'THIS_MONTH', label: '이번 달' },
  { key: 'LAST_MONTH', label: '지난 달' }
]

const exportFormats = [
  { value: 'csv', label: 'CSV' },
  { value: 'pdf', label: 'PDF' },
  { value: 'zip', label: 'ZIP' }
]

const exportScopes = [
  { value: 'PAGE', label: '현재 페이지' },
  { value: 'ALL', label: '전체 결과' },
  { value: 'SELECTION', label: '선택 항목' }
]

const allColumns = [
  { key: 'id', label: 'ID', align: 'left', sortable: true },
  { key: 'transactionId', label: '거래번호', align: 'left' },
  { key: 'reservationId', label: '예약 ID', align: 'left' },
  { key: 'hotelName', label: '호텔', align: 'left' },
  { key: 'userName', label: '고객명', align: 'left' },
  { key: 'totalPrice', label: '총 결제금액', type: 'currency', align: 'right', sortable: true },
  { key: 'amount', label: '실 결제 금액', type: 'currency', align: 'right', sortable: true },
  { key: 'paymentMethod', label: '결제수단', align: 'left' },
  { key: 'paymentStatus', label: '상태', type: 'status', align: 'left', sortable: true },
  { key: 'createdAt', label: '결제일시', type: 'datetime', align: 'left', sortable: true },
  { key: 'canceledAt', label: '취소일시', type: 'datetime', align: 'left' }
  // 영수증/메모 컬럼 제거 (작업 버튼에서 영수증 다운로드 가능)
]

const sortableColumns = new Set(['id', 'totalPrice', 'amount', 'paymentStatus', 'createdAt'])

const flatpickrDateTimeConfig = {
  mode: 'range',
  enableTime: true,
  time_24hr: true,
  locale: Korean,
  dateFormat: 'Y-m-d H:i'
}

const flatpickrDateConfig = {
  mode: 'range',
  enableTime: false,
  locale: Korean,
  dateFormat: 'Y-m-d'
}

const loadPresetsFromStorage = () => {
  if (typeof window === 'undefined') {
    return []
  }
  try {
    const raw = localStorage.getItem(presetStorageKey)
    return raw ? JSON.parse(raw) : []
  } catch (error) {
    console.warn('필터 프리셋을 불러오지 못했습니다.', error)
    return []
  }
}

const createPresetId = () => `preset-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 8)}`

const startOfDay = (date) => new Date(date.getFullYear(), date.getMonth(), date.getDate())
const endOfDay = (date) => new Date(date.getFullYear(), date.getMonth(), date.getDate(), 23, 59, 59)

const formatDateTimeForApi = (value, end = false) => {
  if (!value) return null
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return null
  if (end) {
    date.setHours(23, 59, 59, 999)
  }
  const pad = (n) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

const formatDateForApi = (value) => {
  if (!value) return null
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return null
  const pad = (n) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

const resolveQuickFilterRange = (key) => {
  const now = new Date()
  const todayStart = startOfDay(now)
  switch (key) {
    case 'TODAY':
      return [todayStart, now]
    case 'YESTERDAY': {
      const start = new Date(todayStart)
      start.setDate(start.getDate() - 1)
      const end = endOfDay(start)
      return [start, end]
    }
    case 'LAST_7_DAYS': {
      const start = new Date(todayStart)
      start.setDate(start.getDate() - 6)
      return [start, now]
    }
    case 'LAST_30_DAYS': {
      const start = new Date(todayStart)
      start.setDate(start.getDate() - 29)
      return [start, now]
    }
    case 'THIS_MONTH': {
      const start = new Date(now.getFullYear(), now.getMonth(), 1)
      return [start, now]
    }
    case 'LAST_MONTH': {
      const start = new Date(now.getFullYear(), now.getMonth() - 1, 1)
      const end = endOfDay(new Date(now.getFullYear(), now.getMonth(), 0))
      return [start, end]
    }
    default:
      return null
  }
}

export default {
  name: 'PaymentManagement',
  components: {
    FlatPickr
  },
  setup() {
    const loading = ref(false)
    const filterDrawer = ref(false)
    const columnMenuOpen = ref(false)
    const presetMenuOpen = ref(false)
    const exportColumnMenuOpen = ref(false)

    const filters = reactive({
      hotelName: '',
      hotelId: '',
      userName: '',
      status: '',
      transactionId: '',
      minAmount: '',
      maxAmount: '',
      methods: [],
      quickFilter: '',
      dateRange: []
    })

    const payments = reactive({
      items: [],
      page: 0,
      totalPages: 0,
      totalElements: 0,
      size: 20
    })

    const metrics = ref(null)
    const statusSummaryDefinition = [
      { key: 'COMPLETED', label: '완료', className: 'success' },
      { key: 'PENDING', label: '대기', className: 'warning' },
      { key: 'CANCELLED', label: '취소', className: 'danger' },
      { key: 'FAILED', label: '실패', className: 'danger' }
    ]
    const knownStatusKeys = new Set(statusSummaryDefinition.map((item) => item.key))
    const metricsStatusSummary = computed(() => {
      const items = Array.isArray(payments.items) ? payments.items : []
      const counts = items.reduce((acc, payment) => {
        const status = payment?.paymentStatus
        if (!status) {
          return acc
        }
        acc[status] = (acc[status] || 0) + 1
        return acc
      }, {})

      const summary = statusSummaryDefinition.map(({ key, label, className }) => ({
        label,
        className,
        value: counts[key] ?? 0
      }))

      const otherTotal = Object.entries(counts).reduce((total, [status, count]) => {
        if (knownStatusKeys.has(status)) {
          return total
        }
        return total + count
      }, 0)

      if (otherTotal > 0) {
        summary.push({
          label: '기타',
          className: 'default',
          value: otherTotal
        })
      }

      return summary
    })

    const savedPresets = ref(loadPresetsFromStorage())
    watch(savedPresets, (value) => {
      if (typeof window === 'undefined') return
      localStorage.setItem(presetStorageKey, JSON.stringify(value))
    }, { deep: true })

    const newPresetName = ref('')
    const exporting = ref(false)

    const exportForm = reactive({
      format: exportFormats[0].value,
      scope: exportScopes[0].value
    })

    const exportJob = reactive({
      status: null,
      jobId: null,
      processedRows: 0,
      totalRows: 0,
      message: '',
      format: '',
      polling: false
    })

    const exportColumns = ref(allColumns.map((column) => column.key))
    const visibleKeys = ref(allColumns.map((column) => column.key))
    const toggleableColumns = computed(() => allColumns)
  const visibleColumns = computed(() => allColumns.filter((column) => visibleKeys.value.includes(column.key)))

    const sort = ref({ prop: 'createdAt', order: 'descending' })

    const selectedRowIds = ref(new Set())
    const selectedRowCount = computed(() => selectedRowIds.value.size)
    const selectedPayment = ref(null)
  const settlementHotels = ref([])

    const settlementForm = reactive({
      dateRange: [],
      hotelId: '',
      loading: false,
      report: null
    })

    const exportProgress = computed(() => {
      if (exportJob.status === 'COMPLETED') {
        return 100
      }
      if (!exportJob.totalRows) {
        return exportJob.status ? 30 : 0
      }
      return Math.min(100, Math.round((exportJob.processedRows / exportJob.totalRows) * 100))
    })

    const exportStatusLabel = computed(() => {
      if (!exportJob.status) return ''
      const map = {
        PENDING: '대기 중',
        PROCESSING: '처리 중',
        COMPLETED: '완료',
        FAILED: '실패'
      }
      return map[exportJob.status] || exportJob.status
    })

    const allRowsSelected = computed(() => {
      return payments.items.length > 0 && payments.items.every((item) => selectedRowIds.value.has(item.id))
    })

    const activeFilters = computed(() => {
      const chips = []
      if (filters.quickFilter) {
        const quick = quickFilters.find((item) => item.key === filters.quickFilter)
        if (quick) chips.push(`빠른 필터: ${quick.label}`)
      }
      if (filters.hotelName) chips.push(`호텔: ${filters.hotelName}`)
      if (filters.hotelId) chips.push(`호텔 ID: ${filters.hotelId}`)
      if (filters.userName) chips.push(`고객: ${filters.userName}`)
      if (filters.status) chips.push(`상태: ${filters.status}`)
      if (filters.transactionId) chips.push(`거래번호: ${filters.transactionId}`)
      if (filters.minAmount || filters.maxAmount) {
        const min = filters.minAmount ? formatCurrency(filters.minAmount) : '0원'
        const max = filters.maxAmount ? formatCurrency(filters.maxAmount) : '∞'
        chips.push(`금액: ${min} ~ ${max}`)
      }
      if (filters.methods.length) {
        chips.push(`결제수단: ${filters.methods.map((value) => methodLabel(value)).join(', ')}`)
      }
      if (filters.dateRange.length === 2) {
        chips.push(`기간: ${formatDateTime(filters.dateRange[0])} ~ ${formatDateTime(filters.dateRange[1])}`)
      }
      return chips
    })

    let dashboardTimerId = null
    let exportPollTimerId = null

    const clearSelection = () => {
      selectedRowIds.value = new Set()
    }

    const isRowSelected = (id) => selectedRowIds.value.has(id)

    const toggleRowSelection = (id, checked) => {
      const next = new Set(selectedRowIds.value)
      if (checked) {
        next.add(id)
      } else {
        next.delete(id)
      }
      selectedRowIds.value = next
    }

    const toggleSelectAll = (checked) => {
      if (!checked) {
        clearSelection()
        return
      }
      const next = new Set(selectedRowIds.value)
      payments.items.forEach((item) => next.add(item.id))
      selectedRowIds.value = next
    }

    const selectPayment = (row) => {
      selectedPayment.value = row
    }

    const mapPayment = (payment) => ({
      id: payment.paymentId,
      reservationId: payment.reservationId,
      transactionId: payment.transactionId,
      hotelName: payment.hotelName,
      userName: payment.userName,
      userEmail: payment.userEmail,
      totalPrice: payment.totalPrice,
      amount: payment.amount,
      basePrice: payment.basePrice,
      tax: payment.tax,
      discount: payment.discount,
      paymentMethod: payment.paymentMethod,
      paymentStatus: payment.paymentStatus,
      createdAt: payment.createdAt,
      canceledAt: payment.canceledAt,
      receiptUrl: payment.receiptUrl,
      refundedAmount: payment.refundedAmount,
      memo: payment.memo
    })

    const loadDashboard = async () => {
      try {
        const res = await api.get('/admin/payments/dashboard')
        metrics.value = res.data?.data ?? res.data
      } catch (error) {
        console.warn('대시보드 데이터를 불러오지 못했습니다.', error)
      }
    }

    const buildListParams = () => {
      const params = {
        page: payments.page,
        size: payments.size
      }

      if (filters.status) params.status = filters.status
      if (filters.hotelName) params.hotelName = filters.hotelName
      if (filters.hotelId) params.hotelId = filters.hotelId
      if (filters.userName) params.userName = filters.userName
      if (filters.transactionId) params.transactionId = filters.transactionId
      if (filters.minAmount) params.minAmount = filters.minAmount
      if (filters.maxAmount) params.maxAmount = filters.maxAmount
      if (filters.methods.length) params.paymentMethods = filters.methods
      if (filters.quickFilter) params.quickFilter = filters.quickFilter

      if (filters.dateRange.length === 2) {
        params.from = formatDateTimeForApi(filters.dateRange[0], false)
        params.to = formatDateTimeForApi(filters.dateRange[1], true)
      }

      if (sortableColumns.has(sort.value.prop)) {
        params.sort = sort.value.prop
        params.direction = sort.value.order === 'ascending' ? 'asc' : 'desc'
      }

      return params
    }

    const loadPayments = async (page = payments.page) => {
      loading.value = true
      payments.page = page
      try {
        const params = { ...buildListParams(), page }
        const res = await api.get('/admin/payments', { params })
        const response = res.data?.data ?? res.data
        const content = Array.isArray(response?.content) ? response.content : []
        payments.items = content.map(mapPayment)
        payments.page = response?.page ?? response?.number ?? 0
        payments.totalPages = response?.totalPages ?? 0
        payments.totalElements = response?.totalElements ?? 0

        const currentIds = new Set(payments.items.map((item) => item.id))
        const nextSelected = new Set()
        selectedRowIds.value.forEach((id) => {
          if (currentIds.has(id)) {
            nextSelected.add(id)
          }
        })
        selectedRowIds.value = nextSelected

        if (selectedPayment.value) {
          selectedPayment.value = payments.items.find((item) => item.id === selectedPayment.value.id) || null
        }
      } catch (error) {
        console.error('결제 목록 로딩 실패', error)
        const message = error?.response?.data?.message || error.message
        alert(`결제 목록을 불러오지 못했습니다: ${message}`)
        payments.items = []
        payments.page = 0
        payments.totalPages = 0
        payments.totalElements = 0
      } finally {
        loading.value = false
      }
    }

    const sortIndicator = (prop) => {
      if (!sortableColumns.has(prop)) return ''
      if (sort.value.prop !== prop) return '↕'
      return sort.value.order === 'ascending' ? '▲' : '▼'
    }

    const sortBy = (prop) => {
      if (!sortableColumns.has(prop)) return
      if (sort.value.prop === prop) {
        sort.value.order = sort.value.order === 'ascending' ? 'descending' : 'ascending'
      } else {
        sort.value = { prop, order: 'ascending' }
      }
      loadPayments(0)
    }

    const applyFilters = () => {
      payments.page = 0
      filterDrawer.value = false
      loadPayments(0)
    }

    const resetFilters = () => {
      filters.hotelName = ''
      filters.hotelId = ''
      filters.userName = ''
      filters.status = ''
      filters.transactionId = ''
      filters.minAmount = ''
      filters.maxAmount = ''
      filters.methods = []
      filters.quickFilter = ''
      filters.dateRange = []
      payments.page = 0
      loadPayments(0)
    }

    const applyQuickFilter = (key) => {
      filters.quickFilter = key
      const range = resolveQuickFilterRange(key)
      filters.dateRange = range ? range.map((date) => new Date(date)) : []
      loadPayments(0)
    }

    const clearQuickFilter = () => {
      filters.quickFilter = ''
      filters.dateRange = []
      loadPayments(0)
    }

    const savePreset = () => {
      if (!newPresetName.value.trim()) {
        alert('프리셋 이름을 입력하세요.')
        return
      }
      const preset = {
        id: createPresetId(),
        name: newPresetName.value.trim(),
        createdAt: new Date().toISOString(),
        filters: {
          ...filters,
          methods: [...filters.methods],
          dateRange: filters.dateRange.map((value) => formatDateTimeForApi(value)),
          minAmount: filters.minAmount || '',
          maxAmount: filters.maxAmount || ''
        }
      }
      savedPresets.value = [...savedPresets.value, preset]
      newPresetName.value = ''
      alert('현재 필터가 프리셋으로 저장되었습니다.')
    }

    const applyPreset = (preset) => {
      if (!preset) return
      const data = preset.filters || {}
      filters.hotelName = data.hotelName || ''
      filters.hotelId = data.hotelId || ''
      filters.userName = data.userName || ''
      filters.status = data.status || ''
      filters.transactionId = data.transactionId || ''
      filters.minAmount = data.minAmount || ''
      filters.maxAmount = data.maxAmount || ''
      filters.methods = Array.isArray(data.methods) ? [...data.methods] : []
      filters.quickFilter = data.quickFilter || ''
      filters.dateRange = Array.isArray(data.dateRange)
        ? data.dateRange.filter(Boolean).map((value) => new Date(value))
        : []
      presetMenuOpen.value = false
      filterDrawer.value = false
      loadPayments(0)
    }

    const deletePreset = (id) => {
      savedPresets.value = savedPresets.value.filter((preset) => preset.id !== id)
    }

    const restoreDefaultColumns = () => {
      visibleKeys.value = allColumns.map((column) => column.key)
      exportColumns.value = allColumns.map((column) => column.key)
    }

    const loadSettlementHotels = async () => {
      try {
        const res = await api.get('/admin/hotels', { params: { page: 0, size: 200 } })
        const content = res.data?.data?.content ?? res.data?.content ?? []
        settlementHotels.value = content.map((hotel) => ({
          id: hotel.id,
          name: hotel.name
        }))
      } catch (error) {
        console.warn('정산용 호텔 목록을 불러오지 못했습니다.', error)
        settlementHotels.value = []
      }
    }

    const canRefund = (row) => row && row.paymentStatus === 'COMPLETED'

    const confirmRefund = async (row) => {
      if (!canRefund(row)) return
      const confirmMessage = `${row.userName || '고객'}님의 결제를 환불하시겠습니까?`
      const confirmed = window.confirm(confirmMessage)
      if (!confirmed) {
        return
      }
      try {
        await api.put(`/admin/payments/${row.id}/refund`)
        alert('환불이 완료되었습니다.')
        await loadPaymentsAndDashboard(payments.page)
      } catch (error) {
        const message = error?.response?.data?.message || error.message
        alert(`환불 처리에 실패했습니다: ${message}`)
      }
    }

    const generateSettlementReport = async () => {
      settlementForm.loading = true
      try {
        const params = {}
        if (settlementForm.dateRange.length === 2) {
          params.from = formatDateForApi(settlementForm.dateRange[0])
          params.to = formatDateForApi(settlementForm.dateRange[1])
        }
        const hotelId = settlementForm.hotelId ? Number(settlementForm.hotelId) : null
        if (hotelId) params.hotelId = hotelId
        const res = await api.get('/admin/payments/settlements', { params })
        settlementForm.report = res.data?.data ?? res.data
      } catch (error) {
        const message = error?.response?.data?.message || error.message
        alert(`정산 보고서를 불러오지 못했습니다: ${message}`)
      } finally {
        settlementForm.loading = false
      }
    }

    const startExport = async () => {
      if (exportForm.scope === 'SELECTION' && selectedRowIds.value.size === 0) {
        alert('내보낼 결제를 선택하세요.')
        return
      }
      if (!exportColumns.value || exportColumns.value.length === 0) {
        alert('적어도 하나의 컬럼을 선택하세요.')
        return
      }
      exporting.value = true
      try {
        const payload = {
          format: exportForm.format || 'csv',
          scope: exportForm.scope || 'PAGE',
          columns: exportColumns.value,
          async: true,
          filters: {
            status: filters.status || null,
            hotelName: filters.hotelName || null,
            hotelId: filters.hotelId ? Number(filters.hotelId) : null,
            userName: filters.userName || null,
            transactionId: filters.transactionId || null,
            minAmount: filters.minAmount ? Number(filters.minAmount) : null,
            maxAmount: filters.maxAmount ? Number(filters.maxAmount) : null,
            paymentMethods: (filters.methods && filters.methods.length > 0) ? filters.methods : null,
            quickFilter: filters.quickFilter || null,
            from: (filters.dateRange && filters.dateRange.length === 2) ? formatDateTimeForApi(filters.dateRange[0], false) : null,
            to: (filters.dateRange && filters.dateRange.length === 2) ? formatDateTimeForApi(filters.dateRange[1], true) : null
          }
        }
        if (exportForm.scope === 'SELECTION') {
          payload.selectedIds = Array.from(selectedRowIds.value)
        }
        // console.log('Export payload:', payload)
        const res = await api.post('/admin/payments/export/jobs', payload)
        const status = res.data?.data ?? res.data
        if (!status || !status.jobId) {
          alert('내보내기 작업 생성 응답이 올바르지 않습니다.')
          return
        }
        handleExportStatus(status, true)
        alert('내보내기 작업이 시작되었습니다. 완료되면 다운로드할 수 있습니다.')
      } catch (error) {
        console.error('내보내기 오류:', error)
        const message = error?.response?.data?.message || error.message || '알 수 없는 오류'
        alert(`내보내기 요청에 실패했습니다: ${message}`)
      } finally {
        exporting.value = false
      }
    }

    const stopExportPolling = () => {
      if (exportPollTimerId) {
        clearInterval(exportPollTimerId)
        exportPollTimerId = null
      }
    }

    const handleExportStatus = (status, shouldPoll = false) => {
      if (!status) return
      exportJob.status = status.status
      exportJob.jobId = status.jobId
      exportJob.processedRows = status.processedRows ?? 0
      exportJob.totalRows = status.totalRows ?? 0
      exportJob.message = status.message ?? ''
      exportJob.format = status.format ?? exportForm.format

      if (status.status === 'COMPLETED' || status.status === 'FAILED') {
        stopExportPolling()
      } else if (shouldPoll && status.jobId) {
        stopExportPolling()
        exportPollTimerId = setInterval(() => pollExportJob(status.jobId), 2000)
      }
    }

    const pollExportJob = async (jobId) => {
      if (!jobId) return
      try {
        const res = await api.get(`/admin/payments/export/jobs/${jobId}`)
        const status = res.data?.data ?? res.data
        handleExportStatus(status, false)
      } catch (error) {
        stopExportPolling()
        const message = error?.response?.data?.message || error.message
        alert(`내보내기 상태 확인에 실패했습니다: ${message}`)
      }
    }

    const checkExportStatus = () => {
      if (exportJob.jobId) {
        pollExportJob(exportJob.jobId)
      }
    }

    const downloadExport = async (jobId) => {
      if (!jobId) return
      try {
        const res = await api.get(`/admin/payments/export/jobs/${jobId}/download`, { responseType: 'blob' })
        const blob = new Blob([res.data])
        const disposition = res.headers?.['content-disposition'] || res.headers?.['Content-Disposition']
        let filename = `payments-${jobId}.${(exportJob.format || exportForm.format || 'csv')}`
        if (disposition) {
          const match = disposition.match(/filename\*?=([^;]+)/i)
          if (match && match[1]) {
            const raw = match[1].trim().replace(/^UTF-8''/, '').replace(/"/g, '')
            try {
              filename = decodeURIComponent(raw)
            } catch (error) {
              filename = raw
            }
          }
        }
        const url = window.URL.createObjectURL(blob)
        const link = document.createElement('a')
        link.href = url
        link.setAttribute('download', filename)
        document.body.appendChild(link)
        link.click()
        link.remove()
        window.URL.revokeObjectURL(url)
      } catch (error) {
        console.error('내보내기 다운로드 실패:', error)
        const message = error?.response?.data?.message || error.message || '알 수 없는 오류'
        alert(`내보내기 파일을 다운로드하지 못했습니다: ${message}`)
      }
    }

    const onPageChange = (page1Based) => {
      const target = Math.max(0, Math.min(page1Based - 1, payments.totalPages - 1))
      loadPayments(target)
    }

    const onSizeChange = () => {
      payments.page = 0
      loadPayments(0)
    }

    const formatCurrency = (value) => {
      if (value == null || Number.isNaN(Number(value))) return '-'
      return new Intl.NumberFormat('ko-KR', { style: 'currency', currency: 'KRW', maximumFractionDigits: 0 }).format(value)
    }

    const formatNumber = (value) => {
      if (value == null || Number.isNaN(Number(value))) return '-'
      return Number(value).toLocaleString('ko-KR')
    }

    const formatRate = (value) => {
      if (value == null || Number.isNaN(Number(value))) return '-'
      const numeric = Number(value)
      const percent = Math.abs(numeric) > 1 ? numeric : numeric * 100
      return `${percent.toFixed(1)}%`
    }

    const formatTrend = (value) => {
      if (value == null || Number.isNaN(Number(value))) return '변동 없음'
      const numeric = Number(value)
      const percentValue = Math.abs(numeric) > 1 ? numeric : numeric * 100
      if (percentValue > 0) return `▲ ${percentValue.toFixed(1)}%`
      if (percentValue < 0) return `▼ ${Math.abs(percentValue).toFixed(1)}%`
      return '변동 없음'
    }

    const trendClass = (value) => {
      if (value == null || Number.isNaN(Number(value))) return ''
      if (value > 0) return 'trend-positive'
      if (value < 0) return 'trend-negative'
      return ''
    }

    const formatDateTime = (value) => {
      if (!value) return '-'
      const date = new Date(value)
      if (Number.isNaN(date.getTime())) return '-'
      return date.toLocaleString('ko-KR')
    }

    const statusBadgeClass = (status) => {
      const map = {
        COMPLETED: 'badge-success',
        PENDING: 'badge-warning',
        PARTIAL_REFUND: 'badge-info',
        CANCELLED: 'badge-danger',
        FAILED: 'badge-danger'
      }
      return map[status] || 'badge-default'
    }

    const methodLabel = (value) => {
      const option = paymentMethodOptions.find((item) => item.value === value)
      return option ? option.label : value
    }

    const downloadReceipt = (row) => {
      if (!row.receiptUrl) {
        alert('영수증 정보가 없습니다.')
        return
      }
      window.open(row.receiptUrl, '_blank')
    }

    const handleGlobalClick = () => {
      columnMenuOpen.value = false
      presetMenuOpen.value = false
      exportColumnMenuOpen.value = false
    }

    // 최신 상태 반영: 결제 목록 새로고침 시 대시보드도 즉시 갱신
    onMounted(() => {
      const refreshAll = async () => {
        await loadPayments(0)
        await loadDashboard()
      }
      refreshAll()
      loadSettlementHotels()
      dashboardTimerId = setInterval(loadDashboard, 60000)
      document.addEventListener('click', handleGlobalClick)
    })

    // 결제 목록 새로고침 시 대시보드도 즉시 갱신
    const loadPaymentsAndDashboard = async (page = 0) => {
      await loadPayments(page)
      await loadDashboard()
    }
    onBeforeUnmount(() => {
      if (dashboardTimerId) clearInterval(dashboardTimerId)
      stopExportPolling()
      document.removeEventListener('click', handleGlobalClick)
    })

    return {
      // state
      filterDrawer,
      filters,
      statusOptions,
      quickFilters,
      paymentMethodOptions,
      payments,
      metrics,
      metricsStatusSummary,
      loading,
      exportForm,
      exporting,
      exportJob,
      exportProgress,
      exportStatusLabel,
      exportFormats,
      exportScopes,
      exportColumns,
      visibleColumns,
      visibleKeys,
      toggleableColumns,
      columnMenuOpen,
      presetMenuOpen,
      exportColumnMenuOpen,
      savedPresets,
      newPresetName,
      selectedPayment,
    settlementForm,
    settlementHotels,
      selectedRowCount,
      activeFilters,

      // computed/helpers
      formatCurrency,
      formatNumber,
      formatRate,
      formatTrend,
      trendClass,
      formatDateTime,
      sortIndicator,
      statusBadgeClass,
      methodLabel,
      allRowsSelected,
      isRowSelected,
    canRefund,
      flatpickrDateTimeConfig,
      flatpickrDateConfig,

      // actions
      loadPayments,
      applyFilters,
      resetFilters,
      applyQuickFilter,
      clearQuickFilter,
      savePreset,
      applyPreset,
      deletePreset,
      restoreDefaultColumns,
      sortBy,
      toggleRowSelection,
      toggleSelectAll,
      selectPayment,
      clearSelection,
    confirmRefund,
      generateSettlementReport,
    loadSettlementHotels,
      startExport,
      checkExportStatus,
      downloadExport,
      onPageChange,
      onSizeChange,
      downloadReceipt
    }
  }
}
</script>

<style scoped src="@/assets/css/admin/payment-management.css"></style>