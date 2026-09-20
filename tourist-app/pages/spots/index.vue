<template>
  <view class="spots-page">
    <AppHeader />
    <main><PageContainer wide>
      <section class="intro"><view><text class="eyebrow">SCENIC DISCOVERY · {{ cityName }}</text><text class="title">在风景里，遇见一座城</text></view><text class="summary">{{ spots.length ? `已收录 ${spots.length} 处当地景点` : '从当前城市上下文发现值得抵达的风景' }}</text></section>
      <section class="toolbar" aria-label="景点筛选工具栏">
        <scroll-view class="category-scroll" scroll-x :show-scrollbar="false"><view class="categories"><text v-for="item in categories" :key="item" class="category" :class="{ active: category === item }" @tap="category = item">{{ item }}</text></view></scroll-view>
        <view class="search-box"><text class="search-icon">⌕</text><input v-model="keyword" placeholder="输入关键字搜索景点..." confirm-type="search" /></view>
        <picker class="sort-picker" :range="sortOptions" range-key="label" @change="changeSort"><view class="sort-value"><text>{{ activeSort.label }}</text><text>⌄</text></view></picker>
      </section>
      <view v-if="loading" class="spot-grid" aria-label="景点加载中"><view v-for="item in 8" :key="item" class="spot-card skeleton"><view class="skeleton-shine" /></view></view>
      <section v-else-if="error" class="empty-state"><text class="empty-title">景点加载失败</text><text>{{ error }}</text><button @tap="reload">重新加载</button></section>
      <section v-else-if="!visibleSpots.length" class="empty-state"><text class="empty-title">暂未找到{{ keyword ? '符合条件的' : '当前城市的' }}景点</text><text>{{ cityName }}暂无可展示的景点资料，不会使用其他城市内容填充。</text><view class="empty-actions"><button @tap="resetFilters">查看全部</button><button class="button-ghost" @tap="goCities">切换城市</button></view></section>
      <section v-else class="spot-grid">
        <article v-for="spot in visibleSpots" :key="spot.id" class="spot-card" @tap="openSpot(spot)">
          <image class="spot-image" :src="imageFor(spot)" mode="aspectFill" :alt="`${spot.name}景点照片`" @load="markLoaded(spot.id)" @error="handleImageError(spot)" />
          <view v-if="!loadedImages.has(spot.id)" class="image-loading"><view class="skeleton-shine" /></view>
          <view class="card-shade" /><text class="category-badge">{{ spot.category }}</text>
          <view class="card-content"><text class="meta">☆ {{ levelLabel(spot.recommendationLevel) }} · 建议{{ spot.recommendedDuration }}分钟</text><text class="spot-name">{{ spot.name }}</text><text class="description">{{ spot.description }}</text><view class="card-bottom"><text class="source">{{ spot.source === 'amap' ? '高德 POI' : '景区资料' }} · {{ imageSourceLabel(spot) }}</text><button class="navigate" @tap.stop="navigate(spot)">◎ 导航</button></view></view>
        </article>
      </section>
    </PageContainer></main>
  </view>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import AppHeader from '../../components/layout/AppHeader.vue';
import PageContainer from '../../components/layout/PageContainer.vue';
import { initializeCityContext } from '../../composables/useCityContext';
import { APP_ROUTES, navigateTo } from '../../router';
import { loadScenicSpots, type ScenicSpot } from '../../services/scenicSpotService';
import { useTouristStore } from '../../stores';

const store = useTouristStore();
const categories = ['全部', '核心景观', '历史文化', '摄影打卡', '建筑艺术', '自然风光', '文化演艺'] as const;
const sortOptions = [{ key: 'recommended', label: '推荐指数排序' }, { key: 'distance', label: '距离优先' }, { key: 'duration', label: '游览时长' }, { key: 'popularity', label: '热度优先' }] as const;
const loading = ref(true); const error = ref(''); const category = ref<(typeof categories)[number]>('全部');
const keyword = ref(''); const sortKey = ref<(typeof sortOptions)[number]['key']>('recommended'); const spots = ref<ScenicSpot[]>([]);
const imageIndexes = ref(new Map<string, number>()); const loadedImages = ref(new Set<string>());
const pageContext = computed(() => store.cityContext || store.featuredCityContext);
const cityName = computed(() => pageContext.value?.city?.cityName || '当前城市');
const contextKey = computed(() => pageContext.value?.city?.cityKey || pageContext.value?.adcode || cityName.value);
const activeSort = computed(() => sortOptions.find(item => item.key === sortKey.value) || sortOptions[0]);
const visibleSpots = computed(() => { const query = keyword.value.trim().toLocaleLowerCase(); const filtered = spots.value.filter(spot => (category.value === '全部' || spot.category === category.value) && (!query || `${spot.name}${spot.description}${spot.address}`.toLocaleLowerCase().includes(query))); return [...filtered].sort((a, b) => sortKey.value === 'distance' ? (a.distanceMeters ?? Number.MAX_SAFE_INTEGER) - (b.distanceMeters ?? Number.MAX_SAFE_INTEGER) : sortKey.value === 'duration' ? a.recommendedDuration - b.recommendedDuration : sortKey.value === 'popularity' ? b.popularity - a.popularity : b.recommendationLevel - a.recommendationLevel || b.popularity - a.popularity); });
watch(contextKey, () => { void reload(); resetFilters(); });
onMounted(() => { void reload(); });
async function reload(): Promise<void> { loading.value = true; error.value = ''; try { await initializeCityContext(); spots.value = await loadScenicSpots(pageContext.value); } catch (value) { console.error('[DataSource] domain=poi source=demo unavailable', value); spots.value = []; error.value = ''; } finally { loading.value = false; } }
function resetFilters(): void { category.value = '全部'; keyword.value = ''; }
function changeSort(event: { detail: { value: number } }): void { sortKey.value = sortOptions[event.detail.value]?.key || 'recommended'; }
function imageFor(spot: ScenicSpot): string { return spot.images[imageIndexes.value.get(spot.id) || 0] || '/static/images/vision-placeholder.webp'; }
function markLoaded(id: string): void { loadedImages.value.add(id); loadedImages.value = new Set(loadedImages.value); }
function handleImageError(spot: ScenicSpot): void { const current = imageIndexes.value.get(spot.id) || 0; console.warn('[ScenicImage] load failed', { spotId: spot.id, city: cityName.value, source: spot.imageSource, imageIndex: current }); if (current >= spot.images.length - 1) return; imageIndexes.value.set(spot.id, current + 1); imageIndexes.value = new Map(imageIndexes.value); }
function levelLabel(level: number): string { return level >= 5 ? '必看景观' : level >= 4 ? '人气推荐' : '值得一游'; }
function imageSourceLabel(spot: ScenicSpot): string { return spot.imageSource === 'provider' ? '真实景点图片' : spot.imageSource === 'local-asset' ? '本地景点素材' : spot.imageSource === 'city-fallback' ? '城市备用图' : '图片待补充'; }
function navigate(spot: ScenicSpot): void { if (spot.latitude == null || spot.longitude == null) { uni.showToast({ title: '该景点暂无定位信息', icon: 'none' }); return; } uni.openLocation({ latitude: spot.latitude, longitude: spot.longitude, name: spot.name, address: spot.address }); }
function openSpot(spot: ScenicSpot): void { if (spot.source === 'local' && /^\d+$/.test(spot.id)) navigateTo(`/pages/spot/index?id=${spot.id}`); }
function goCities(): void { navigateTo(APP_ROUTES.cities); }
</script>

<style scoped lang="scss">
.spots-page{min-height:100vh;padding-top:16px;overflow-x:hidden;color:#173f35;background:radial-gradient(circle at 85% 8%,rgba(144,179,155,.22),transparent 30%),linear-gradient(180deg,#f0f3eb 0,#f8f5ed 52%,#ecefe7 100%)}main{padding:52px 0 96px}.intro{display:flex;align-items:end;justify-content:space-between;gap:24px;margin-bottom:28px}.intro>view{display:grid;gap:9px}.eyebrow{color:#54806f;font-size:11px;font-weight:800;letter-spacing:.2em}.title{font-family:Georgia,'Songti SC',serif;font-size:clamp(32px,4vw,54px);font-weight:800;line-height:1.15}.summary{padding-bottom:7px;color:#71877e;font-size:13px}.toolbar{display:grid;grid-template-columns:minmax(0,1fr) minmax(260px,360px) 170px;align-items:center;gap:14px;margin-bottom:28px;padding:10px 12px;background:rgba(255,255,255,.7);border:1px solid rgba(35,80,65,.09);border-radius:18px;box-shadow:0 12px 35px rgba(28,70,56,.07);backdrop-filter:blur(15px)}.category-scroll{min-width:0;white-space:nowrap}.categories{display:inline-flex;gap:4px}.category{padding:10px 13px;color:#61796f;border-radius:10px;font-size:12px;white-space:nowrap;transition:.2s ease}.category.active{color:#fff;background:#1e5143;font-weight:700}.search-box{display:flex;align-items:center;gap:8px;height:42px;padding:0 13px;background:#f4f5ef;border:1px solid rgba(35,80,65,.1);border-radius:12px;box-sizing:border-box}.search-icon{color:#648276;font-size:20px}.search-box input{width:100%;color:#234e42;font-size:12px}.sort-picker{border-left:1px solid rgba(35,80,65,.1)}.sort-value{display:flex;justify-content:space-between;gap:10px;padding:12px;color:#355f52;font-size:12px}.spot-grid{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:20px}.spot-card{position:relative;aspect-ratio:3/4;min-width:0;overflow:hidden;background:#dce6de;border:1px solid rgba(255,255,255,.7);border-radius:19px;box-shadow:0 14px 34px rgba(25,62,51,.12);cursor:pointer;transform:translateZ(0);transition:transform .25s ease,box-shadow .25s ease}.spot-card:hover{transform:translateY(-5px);box-shadow:0 23px 48px rgba(25,62,51,.2)}.spot-image{position:absolute;inset:0;width:100%;height:100%;transition:transform .28s ease}.spot-card:hover .spot-image{transform:scale(1.03)}.card-shade{position:absolute;inset:0;background:linear-gradient(180deg,rgba(4,27,21,.05) 20%,rgba(7,25,20,.15) 46%,rgba(5,20,16,.88) 100%)}.category-badge{position:absolute;top:16px;left:16px;padding:7px 11px;color:#fff;background:rgba(18,65,52,.62);border:1px solid rgba(255,255,255,.3);border-radius:999px;font-size:10px;font-weight:700;backdrop-filter:blur(12px)}.card-content{position:absolute;right:0;bottom:0;left:0;display:grid;gap:7px;padding:22px;color:#fff}.meta{color:rgba(255,255,255,.78);font-size:10px;letter-spacing:.04em}.spot-name{font-family:Georgia,'Songti SC',serif;font-size:clamp(22px,2vw,30px);font-weight:800}.description{display:-webkit-box;overflow:hidden;color:rgba(255,255,255,.78);font-size:12px;line-height:1.55;-webkit-box-orient:vertical;-webkit-line-clamp:2}.card-bottom{display:flex;align-items:center;justify-content:space-between;gap:8px;margin-top:5px}.source{overflow:hidden;color:rgba(255,255,255,.58);font-size:9px;text-overflow:ellipsis;white-space:nowrap}.navigate{flex:0 0 auto;margin:0;padding:7px 11px;color:#173f35;background:#f7f4e9;border:0;border-radius:999px;font-size:10px;line-height:1.3}.image-loading{position:absolute;inset:0;overflow:hidden;background:#dbe4dc}.skeleton{border:0}.skeleton-shine{width:100%;height:100%;background:linear-gradient(105deg,transparent 35%,rgba(255,255,255,.55) 47%,transparent 59%);animation:shine 1.4s infinite linear}@keyframes shine{from{transform:translateX(-100%)}to{transform:translateX(100%)}}.empty-state{display:grid;justify-items:center;gap:12px;padding:90px 24px;color:#71877e;text-align:center;background:rgba(255,255,255,.58);border:1px solid rgba(35,80,65,.08);border-radius:22px}.empty-title{color:#214f41;font-family:Georgia,'Songti SC',serif;font-size:24px;font-weight:800}.empty-actions{display:flex;gap:10px;margin-top:8px}.empty-state button{margin:0;padding:11px 18px;color:#fff;background:#235e4e;border:0;border-radius:11px;font-size:12px}.empty-state .button-ghost{color:#315f51;background:#e6eee8}@media(max-width:1199px){.toolbar{grid-template-columns:1fr minmax(230px,320px)}.sort-picker{border-left:0}.spot-grid{grid-template-columns:repeat(2,minmax(0,1fr))}}@media(max-width:767px){.spots-page{padding-top:8px}main{padding:36px 0 65px}.intro{display:grid;margin-bottom:20px}.summary{padding:0}.toolbar{grid-template-columns:1fr;gap:8px;padding:9px}.category-scroll{width:100%}.search-box{height:46px}.sort-picker{border-top:1px solid rgba(35,80,65,.08)}.spot-grid{grid-template-columns:1fr;gap:18px}.spot-card{aspect-ratio:4/5}.card-content{padding:22px}.spot-name{font-size:28px}.empty-actions{width:100%;flex-direction:column}.empty-state button{width:100%}}
</style>
