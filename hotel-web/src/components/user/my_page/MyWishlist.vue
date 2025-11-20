<template>
  <div class="wishlist-tab">
    <!-- <h2>찜 목록</h2> -->

    <ul v-if="wishlist.length > 0" class="wishlist-list">
      <li v-for="item in wishlist" :key="item.wishlistId" class="wishlist-item">
        <div class="hotel-info" @click="goToDetail(item.hotelId)">
          <img :src="item.hotelImageUrl || fallbackImage" class="hotel-image" />
          <div class="hotel-details">
            <h3>{{ item.hotelName }}</h3>
            <p>{{ item.hotelAddress }}</p>
          </div>
        </div>

        <div class="hotel-actions">
          <p class="hotel-price">{{ (item.hotelPrice ?? 0).toLocaleString() }}원</p>
          <p class="per-night">1박 최저가</p>
          <button class="delete-btn" @click.stop="removeFromWishlist(item.hotelId)">
            삭제
          </button>
        </div>
      </li>
    </ul>

    <p v-else class="empty">찜 목록이 비어 있습니다.</p>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import http, { resolveBackendUrl } from '@/api/http'

const wishlist = ref([])
const router = useRouter()
const fallbackImage = 'https://cdn-icons-png.flaticon.com/512/201/201623.png'

const resolveImageUrl = (url) => {
  if (!url) return fallbackImage
  if (/^https?:\/\//i.test(url)) return url
  const normalized = url.startsWith('/') ? url : `/${url}`
  return resolveBackendUrl(normalized)
}

// 내 찜 목록 불러오기
const fetchWishlist = async () => {
  try {
    const res = await http.get('/wishlists')
    wishlist.value = Array.isArray(res.data)
      ? res.data.map(item => ({
          ...item,
          hotelImageUrl: resolveImageUrl(item?.hotelImageUrl)
        }))
      : []
    console.log('[wishlist]', wishlist.value)
  } catch (err) {
    console.error('찜 목록 불러오기 실패:', err.response?.data || err.message)
  }
}

// 찜 삭제
const removeFromWishlist = async (hotelId) => {
  try {
    await http.delete(`/wishlists/${hotelId}`)
    wishlist.value = wishlist.value.filter(w => w.hotelId !== hotelId)
    alert('찜 삭제 완료')
  } catch (err) {
    console.error('찜 삭제 실패:', err.response?.data || err.message)
    alert(err.response?.data?.message || '찜 삭제 중 오류 발생')
  }
}

// 상세 페이지 이동 (hotelId 안전 체크)
const goToDetail = (hotelId) => {
  if (!hotelId) {
    console.warn('🚨 hotelId 없음:', hotelId)
    alert('호텔 정보를 불러올 수 없습니다.')
    return
  }
  router.push({ path: `/hotels/${hotelId}` })
}

onMounted(fetchWishlist)
</script>

<style scoped>
@import '@/assets/css/mypage/mywishlist.css';
</style>