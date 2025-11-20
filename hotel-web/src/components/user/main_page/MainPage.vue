<template>
  <div class="main-page">
    <!-- Hero 배너 -->
    <div class="hero-section hero-bg">
      <!-- <div class="hero-overlay"></div>
      <div class="hero-text-container">
        <div class="hero-text">
          <p class="hero-subtitle">
            호텔 예약 접속자 수 5위!!
          </p>
        </div>
      </div> -->
    </div>

    <!-- 검색 폼 -->
    <SearchForm />

    <!-- 추천 국내 여행지 -->
    <section class="recommended-section" v-if="showDomesticSection">
      <h2 class="section-title">추천 국내 여행지</h2>
      <div class="swiper-container">
        <button 
          v-show="domesticSwiper && !domesticSwiper.isBeginning"
          @click="prevDomestic"
          class="nav-button nav-button-left"
        >‹</button>

        <swiper
          :modules="modules"
          :slides-per-view="4"
          :space-between="20"
          :loop="false"
          :speed="600"
          :observer="true"
          :observe-parents="true"
          :update-on-window-resize="true"
          :watch-overflow="true"
          :pagination="{ clickable: true }"
          :breakpoints="{ 0: { slidesPerView: 1 }, 640: { slidesPerView: 2 }, 1024: { slidesPerView: 4 } }"
          class="destination-swiper"
          @swiper="(s) => domesticSwiper = s"
        >
          <swiper-slide
            v-for="(place, i) in destinationsDomestic"
            :key="i"
            class="destination-card"
            @click="searchDestination(place.name)"
            role="button"
            tabindex="0"
          >
            <img
              :src="place.image"
              :alt="place.name"
              class="destination-image"
              loading="lazy"
              decoding="async"
              fetchpriority="low"
              width="400"
              height="180"
              sizes="(max-width:640px) 100vw, (max-width:1024px) 50vw, 25vw"
            />
            <div class="destination-info">
              <h3>{{ place.name }}</h3>
              <p>{{ place.description }}</p>
            </div>
          </swiper-slide>
        </swiper>

        <button 
          v-show="domesticSwiper && !domesticSwiper.isEnd"
          @click="nextDomestic"
          class="nav-button nav-button-right"
        >›</button>
      </div>
    </section>

    <!-- 추천 해외 여행지 -->
    <section class="recommended-section" v-if="showOverseasSection">
      <h2 class="section-title">추천 해외 여행지</h2>
      <div class="swiper-container">
        <button 
          v-show="overseasSwiper && !overseasSwiper.isBeginning"
          @click="prevOverseas"
          class="nav-button nav-button-left"
        >‹</button>

        <swiper
          :modules="modules"
          :slides-per-view="4"
          :space-between="20"
          :loop="false"
          :speed="600"
          :observer="true"
          :observe-parents="true"
          :update-on-window-resize="true"
          :watch-overflow="true"
          :pagination="{ clickable: true }"
          :breakpoints="{ 0: { slidesPerView: 1 }, 640: { slidesPerView: 2 }, 1024: { slidesPerView: 4 } }"
          class="destination-swiper"
          @swiper="(s) => overseasSwiper = s"
        >
          <swiper-slide
            v-for="(place, i) in destinationsOverseas"
            :key="i"
            class="destination-card"
            @click="searchDestination(place.name)"
            role="button"
            tabindex="0"
          >
            <img
              :src="place.image"
              :alt="place.name"
              class="destination-image"
              loading="lazy"
              decoding="async"
              fetchpriority="low"
              width="400"
              height="180"
              sizes="(max-width:640px) 100vw, (max-width:1024px) 50vw, 25vw"
            />
            <div class="destination-info">
              <h3>{{ place.name }}</h3>
              <p>{{ place.description }}</p>
            </div>
          </swiper-slide>
        </swiper>

        <button 
          v-show="overseasSwiper && !overseasSwiper.isEnd"
          @click="nextOverseas"
          class="nav-button nav-button-right"
        >›</button>
      </div>
    </section>

    <!-- 인기 호텔 -->
    <div ref="hotelSentinel" style="height:1px;"></div>
    <section class="hotel-section" v-if="showHotelSection">
      <h2 class="section-title">인기 호텔 추천</h2>
      <div v-if="loadingPopular" class="hotel-list">
        <div class="hotel-card" v-for="n in 4" :key="'skeleton-'+n">
          <div class="hotel-image" style="background:#f2f3f5"></div>
        </div>
      </div>

      <div v-else class="hotel-list">
        <div
          v-for="(hotel, i) in popularHotels"
          :key="hotel.id || i"
          class="hotel-card"
          @click="goHotelDetail(hotel.id)"
        >
          <img
            :src="hotel.image"
            :alt="hotel.name"
            class="hotel-image"
            loading="lazy"
            decoding="async"
            fetchpriority="low"
            width="400"
            height="160"
            sizes="(max-width:640px) 100vw, (max-width:1024px) 50vw, 33vw"
            @error="onHotelImageError($event, hotel)"
          />
          <div class="hotel-info">
            <h3>{{ hotel.name }}</h3>
            <p>{{ hotel.city }}</p>
            <p class="hotel-price">{{ hotel.price }}</p>
            <p class="hotel-rating" v-if="hotel.rating">⭐ {{ hotel.rating }}</p>
          </div>
        </div>
      </div>
    </section>

    <!-- 여행 꿀팁 -->
    <div ref="tipsSentinel" style="height:1px;"></div>
    <section class="tips-section" v-if="showTipsSection">
      <h2 class="section-title">여행 꿀팁</h2>
      <div class="tips-list">
        <div v-for="(tip, i) in travelTips" :key="i" class="tip-card">
          <h3>{{ tip.title }}</h3>
          <p>{{ tip.description }}</p>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped src="@/assets/css/homepage/mainpage.css"></style>

<script>
import SearchForm from "@/components/user/main_page/SearchForm.vue";
import { Swiper, SwiperSlide } from "swiper/vue";
import { Navigation, Pagination } from "swiper/modules";
import HotelApi from "@/api/HotelApi";
import { getAuthUser } from "@/utils/auth-storage";
import { resolveBackendUrl } from "@/api/http";
import hotel1Image from "@/images/hotel1.png";
import hotel2Image from "@/images/hotel2.png";
import hotel3Image from "@/images/hotel3.png";
import hotel4Image from "@/images/hotel4.png";
import hotel5Image from "@/images/hotel5.png";
import "swiper/css";
import "swiper/css/navigation";
import "swiper/css/pagination";

const PUBLIC_UPLOAD_BASE = "https://hwiyeong.shop";
const LEGACY_MEDIA_HOSTS = new Set(["images.example.com", "localhost", "127.0.0.1"]);

const normalizeUploadPath = (pathname) => {
  if (!pathname) {
    return "/uploads";
  }

  const idx = pathname.indexOf("/uploads");
  if (idx >= 0) {
    const slice = pathname.slice(idx);
    return slice === "" ? "/uploads" : slice;
  }

  let normalized = pathname.startsWith("/") ? pathname : `/${pathname}`;

  if (normalized === "/uploads" || normalized.startsWith("/uploads/")) {
    return normalized;
  }

  return normalized === "/" ? "/uploads" : `/uploads${normalized}`;
};

const composePublicUploadUrl = (pathname, search = "") => {
  const path = normalizeUploadPath(pathname);
  const query = search && typeof search === "string" ? search : "";
  return `${PUBLIC_UPLOAD_BASE}${path}${query}`;
};

export default {
  name: "MainPage",
  components: { SearchForm, Swiper, SwiperSlide },
  data() {
    return {
      isLoggedIn: false,
      user: { name: "홍길동" },
      destinationsDomestic: [
        { name: "서울", description: "한국의 수도, 쇼핑과 문화의 중심지", image: "/images/seoul.webp" },
        { name: "부산", description: "바다와 해운대, 맛있는 해산물", image: "/images/busan.webp" },
        { name: "제주", description: "힐링 여행지, 한라산과 올레길", image: "/images/jeju.webp" },
        { name: "강릉", description: "동해안의 바다와 커피 거리", image: "/images/gangreung.webp" },
        { name: "여수", description: "밤바다로 유명한 낭만 여행지", image: "/images/yeosu.webp" },
        { name: "전주", description: "한옥마을과 맛의 고장", image: "/images/jeonju.webp" },
        { name: "경주", description: "천년 고도의 역사 여행", image: "/images/gyeongju.webp" },
        { name: "인천", description: "국제공항과 차이나타운", image: "/images/incheon.webp" }
      ],
      destinationsOverseas: [
        { name: "도쿄", description: "일본의 수도, 전통과 현대의 조화", image: "/images/tokyo.webp" },
        { name: "파리", description: "낭만의 도시, 에펠탑과 루브르", image: "/images/paris.webp" },
        { name: "방콕", description: "태국의 활기찬 수도", image: "/images/bangkok.webp" },
        { name: "뉴욕", description: "세계의 중심, 자유의 여신상", image: "/images/newyork.webp" },
        { name: "런던", description: "영국의 수도, 빅벤과 타워브리지", image: "/images/london.webp" },
        { name: "로마", description: "고대 유적과 이탈리아 감성", image: "/images/roma.webp" },
        { name: "시드니", description: "오페라하우스와 아름다운 항구", image: "/images/sydney.webp" },
        { name: "하와이", description: "천국 같은 휴양지", image: "/images/hawaii.webp" }
      ],
      domesticSwiper: null,
      overseasSwiper: null,
      popularHotels: [],
      loadingPopular: true,
      travelTips: [
        { title: "제주도 여행 전 필수 체크리스트", description: "렌트카 예약, 숙소, 맛집 예약까지! 미리 준비하면 편리해요." },
        { title: "유럽 여행 시 꿀팁", description: "유레일 패스로 교통비 절약하고 인기 명소는 사전 예약 필수!" },
        { title: "해외여행 짐 싸기 노하우", description: "짐은 최소화! 멀티어댑터, 보조배터리는 필수 아이템." }
      ],
      showDomesticSection: true,
      showOverseasSection: true,
      showHotelSection: false,
      showTipsSection: false,
      defaultHotelImage: "/images/paradiseHotel.webp"
    };
  },
  mounted() {
    this.checkAuthStatus();
    this.initSectionObservers();
  },
  methods: {
    checkAuthStatus() {
      const userInfo = getAuthUser();
      if (userInfo) {
        this.isLoggedIn = true;
        this.user = userInfo;
      }
    },
    handleLogout() {
      this.isLoggedIn = false;
      this.user = { name: "홍길동" };
    },
    async fetchPopularHotels() {
      try {
        this.loadingPopular = true;
        const page = await HotelApi.search({ page: 0, size: 20 });
        let items = Array.isArray(page?.content) ? page.content : [];
        items = items.sort((a, b) => (b.starRating || 0) - (a.starRating || 0)).slice(0, 4);
        this.popularHotels = items.map(it => ({
          id: it.id,
          name: it.name,
          city: it.address || "",
          lowestPrice: this.normalizePrice(it?.lowestPrice ?? it?.hotelPrice),
          price: "요금 정보 없음",
          rating: it?.starRating ?? null,
          image: this.pickCoverImage(it, this.hotelThumb(it?.id))
        }));
        this.popularHotels.forEach(hotel => {
          if (typeof hotel.lowestPrice === "number") {
            hotel.price = this.formatPrice(hotel.lowestPrice);
          }
        });
        await this.fillMissingHotelPrices(this.popularHotels);
      } finally {
        this.loadingPopular = false;
      }
    },
    async fillMissingHotelPrices(list) {
      const targets = list.filter(h => (h.lowestPrice == null || Number.isNaN(h.lowestPrice)) && h.id);
      if (!targets.length) return;
      await Promise.all(targets.map(async (hotel) => {
        try {
          const detail = await HotelApi.getDetail(hotel.id);
          const rooms = Array.isArray(detail?.rooms) ? detail.rooms : [];
          const min = rooms
            .map(r => this.normalizePrice(r?.price))
            .filter(v => typeof v === "number" && v > 0)
            .reduce((acc, v) => (acc == null ? v : Math.min(acc, v)), null);
          if (min != null) {
            hotel.lowestPrice = min;
            hotel.price = this.formatPrice(min);
          }
          const detailHotel = detail?.hotel ?? detail;
          hotel.image = this.pickCoverImage(detailHotel, this.hotelThumb(hotel.id));
        } catch (err) {
          console.debug("[MainPage] failed to load hotel detail for price", hotel.id, err?.message);
        }
      }));
    },
    normalizePrice(raw) {
      const n = raw == null ? null : Number(raw);
      return Number.isFinite(n) && n > 0 ? n : null;
    },
    formatPrice(value) {
      return `₩${Number(value).toLocaleString()} / 1박`;
    },
    pickCoverImage(source, fallbackOverride) {
      const fallback = fallbackOverride ?? this.defaultHotelImage;

      const candidates = [];
      const pushCandidates = (value) => {
        const extracted = this.extractImageCandidates(value);
        if (extracted.length) {
          candidates.push(...extracted);
        }
      };

      if (typeof source === "string") {
        pushCandidates(source);
      } else if (Array.isArray(source)) {
        source.forEach(pushCandidates);
      } else if (source && typeof source === "object") {
        const primaryKeys = [
          "coverImage",
          "cover",
          "coverImageUrl",
          "mainImage",
          "mainImageUrl",
          "thumbnail",
          "thumbnailUrl",
          "previewImage",
          "image",
          "imageUrl",
          "primaryImage",
        ];
        primaryKeys.forEach(k => {
          if (k in source) pushCandidates(source[k]);
        });

        const collectionKeys = [
          "imageUrls",
          "images",
          "hotelImages",
          "photos",
          "gallery",
          "media",
          "thumbnails",
          "assets",
          "coverImages",
        ];
        collectionKeys.forEach(k => {
          if (k in source) pushCandidates(source[k]);
        });
      }

      if (!candidates.length) {
        return fallback;
      }

      for (const candidate of candidates) {
        const absolute = this.ensureAbsoluteImage(candidate, fallback);
        if (absolute) {
          return absolute;
        }
      }

      return fallback;
    },
    hotelThumb(id) {
      const map = {
        1: hotel1Image,
        2: hotel2Image,
        3: hotel3Image,
        4: hotel4Image,
        5: hotel5Image
      };
      const key = Number(id);
      if (map[key]) {
        return map[key];
      }
      if (Number.isFinite(key) && key > 0) {
        return `https://picsum.photos/seed/hotel${key}/400/300`;
      }
      return this.defaultHotelImage;
    },
    ensureAbsoluteImage(url, fallback = this.defaultHotelImage) {
      const normalizeFallback = () => (
        fallback !== undefined ? fallback : this.defaultHotelImage
      );

      if (url == null) {
        return normalizeFallback();
      }

      if (typeof url === "object") {
        const nested = url.url ?? url.imageUrl ?? url.src ?? url.path ?? url.fileUrl;
        if (nested != null) {
          return this.ensureAbsoluteImage(nested, fallback);
        }
        return normalizeFallback();
      }

      let raw = String(url).trim();
      if (!raw) return normalizeFallback();

      const stripQuotes = (value) => value.replace(/^['"]|['"]$/g, "");
      raw = stripQuotes(raw);

      const looksJson = (value) =>
        (value.startsWith("[") && value.endsWith("]")) ||
        (value.startsWith("{") && value.endsWith("}"));

      if (looksJson(raw)) {
        try {
          const parsed = JSON.parse(raw);
          const viaParsed = this.ensureAbsoluteImage(parsed, fallback);
          if (viaParsed) {
            return viaParsed;
          }
        } catch (_) {
          // ignore malformed JSON
        }
        const firstSegment = raw.replace(/^\[|\]$/g, "").split(",")[0] ?? "";
        raw = stripQuotes(firstSegment.trim());
      }

      if (!raw) {
        return normalizeFallback();
      }

      if (/^https?:\/\//i.test(raw)) {
        try {
          const parsed = new URL(raw);
          const host = parsed.hostname?.toLowerCase?.();

          if (host && LEGACY_MEDIA_HOSTS.has(host)) {
            return composePublicUploadUrl(parsed.pathname, parsed.search);
          }

          if (host === "hwiyeong.shop" && parsed.protocol === "http:") {
            return `https://${parsed.host}${parsed.pathname || ""}${parsed.search || ""}`;
          }

          return raw;
        } catch (_) {
          // fall through to relative handling
        }
      }

      if (raw.startsWith("//")) {
        return `https:${raw}`;
      }

      const stripLeading = (value) => value.replace(/^\.?\/+/, "");
      const cleaned = stripLeading(raw);

      if (cleaned.startsWith("//")) {
        return `https:${cleaned}`;
      }

      let normalized = cleaned;
      if (cleaned.startsWith("uploads/")) {
        normalized = `/${cleaned}`;
      } else if (!cleaned.startsWith("/")) {
        normalized = `/${cleaned}`;
      }

      return resolveBackendUrl(normalized);
    },
    extractImageCandidates(value) {
      if (value == null) return [];

      if (typeof value === "string") {
        const trimmed = value.trim();
        if (!trimmed) return [];

        const deQuoted = trimmed.replace(/^['"]|['"]$/g, "");

        const looksJson = (str) =>
          (str.startsWith("[") && str.endsWith("]")) ||
          (str.startsWith("{") && str.endsWith("}"));

        if (looksJson(deQuoted)) {
          try {
            const parsed = JSON.parse(deQuoted);
            return this.extractImageCandidates(parsed);
          } catch (_) {
            // ignore malformed JSON
          }
        }

        if (deQuoted.includes(",")) {
          return deQuoted.split(",").map(part => part.trim()).filter(Boolean);
        }

        return [deQuoted];
      }

      if (Array.isArray(value)) {
        return value.flatMap(item => this.extractImageCandidates(item));
      }

      if (typeof value === "object") {
        const preferredKeys = ["url", "imageUrl", "src", "path", "fileUrl", "value", "cover"];
        for (const key of preferredKeys) {
          if (key in value && value[key] != null) {
            return this.extractImageCandidates(value[key]);
          }
        }
      }

      return [];
    },
    resolveImage(url, fallback = this.defaultHotelImage) {
      return this.ensureAbsoluteImage(url, fallback);
    },
    onHotelImageError(evt, hotel) {
      const fallback = hotel ? this.hotelThumb(hotel.id) : this.defaultHotelImage;
      const target = evt?.target;
      if (target && target.getAttribute("data-fallback") !== "true") {
        target.setAttribute("data-fallback", "true");
        target.src = fallback;
      }
      if (hotel) {
        hotel.image = fallback;
      }
    },
    goHotelDetail(id) {
      if (id) this.$router.push({ name: "HotelDetail", params: { id } });
    },
    initSectionObservers() {
      const opts = { root: null, rootMargin: "200px 0px", threshold: 0.01 };
      const observer = new IntersectionObserver((entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            if (entry.target === this.$refs.hotelSentinel) {
              this.showHotelSection = true;
              this.fetchPopularHotels();
            }
            if (entry.target === this.$refs.tipsSentinel) {
              this.showTipsSection = true;
            }
            observer.unobserve(entry.target);
          }
        });
      }, opts);
      if (this.$refs.hotelSentinel) observer.observe(this.$refs.hotelSentinel);
      if (this.$refs.tipsSentinel) observer.observe(this.$refs.tipsSentinel);
    },
    prevDomestic() { this.domesticSwiper?.slidePrev(); },
    nextDomestic() { this.domesticSwiper?.slideNext(); },
    prevOverseas() { this.overseasSwiper?.slidePrev(); },
    nextOverseas() { this.overseasSwiper?.slideNext(); },
    searchDestination(name) {
      const q = (name || '').toString().trim();
      if (!q) return;
      this.$router.push({ path: '/search', query: { q } });
    }
  },
  setup() {
    return { modules: [Navigation, Pagination] };
  }
};
</script>
