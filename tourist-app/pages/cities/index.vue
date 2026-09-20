<template>
  <view class="entry-page">
    <AppHeader />
    <PageContainer>
      <view class="page-head">
        <text class="eyebrow">CITY DISCOVERY</text>
        <text class="title">探索一座城市</text>
        <text class="subtitle">当前城市：{{ currentCityName }}。可搜索并发现任意高德可验证的中国城市。</text>
        <view class="search-row">
          <input v-model="keyword" class="search" placeholder="例如：成都、西安、青岛" confirm-type="search" @confirm="searchCity" />
          <button class="search-button" :disabled="discovering" @tap="searchCity">{{ discovering ? '发现中…' : '搜索城市' }}</button>
          <button class="locate-button" :disabled="locating" @tap="locateCity">{{ locateButtonText }}</button>
        </view>
        <text v-if="store.locationError" class="location-error">{{ store.locationError }}</text>
      </view>
      <view v-if="loading" class="state">正在加载城市…</view>
      <view v-if="searchError" class="state state--error">{{ searchError }}</view>
      <view v-if="recommendedCitiesError" class="state state--warning">推荐城市暂时无法加载，仍可定位或手动搜索城市。</view>
      <view v-if="!visibleCities.length" class="state">输入城市名称开始动态发现</view>
      <template v-else>
        <view v-if="success" class="state state--success">{{ success }}</view>
        <view class="card-grid">
          <view v-for="city in visibleCities" :key="city.cityKey || city.cityCode" class="content-card" @tap="selectCity(city)">
            <text class="card-title">{{ city.cityName }}</text>
            <text class="card-copy">{{ city.slogan || city.description || city.province || '城市资料已配置' }}</text>
            <text class="card-copy">来源：{{ city.source === 'discovered' ? '高德动态发现' : '本地城市资料' }}</text>
            <text v-if="isCurrent(city)" class="card-status">当前城市</text>
          </view>
        </view>
      </template>
    </PageContainer>
  </view>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { getCityContext, listCities, resolveCityContext } from '../../api/scenic';
import AppHeader from '../../components/layout/AppHeader.vue';
import PageContainer from '../../components/layout/PageContainer.vue';
import { initializeCityContext, locateCurrentCity } from '../../composables/useCityContext';
import { useTouristStore } from '../../stores';
import type { CityVO } from '../../types';

const store = useTouristStore();
const cities = ref<CityVO[]>([]);
const keyword = ref('');
const loading = ref(true);
const discovering = ref(false);
const locating = ref(false);
const searchError = ref('');
const recommendedCitiesError = ref('');
const success = ref('');
const locateButtonText = computed(() => store.locationStatus === 'resolving' ? '解析城市中…'
  : store.locationStatus === 'locating' ? '定位中…' : '定位当前位置');
const currentCityName = computed(() => store.currentCity?.cityName || '待选择');
const visibleCities = computed(() => {
  const query = keyword.value.trim();
  return query ? cities.value.filter((city) => city.cityName.includes(query)) : cities.value;
});

async function selectCity(city: CityVO): Promise<void> {
  searchError.value = '';
  success.value = '';
  try {
    const context = city.id ? await getCityContext(city.id) : await resolveCityContext(city.cityName, city.cityCode);
    store.setCityContext(context);
    success.value = `已切换到${context.city?.cityName || city.cityName}`;
  } catch (value: unknown) {
    searchError.value = value instanceof Error ? value.message : '城市上下文加载失败';
  }
}

function isCurrent(city: CityVO): boolean {
  return Boolean((city.cityKey && city.cityKey === store.cityContext?.cityKey)
    || (city.id && city.id === store.currentCityId));
}

async function searchCity(): Promise<void> {
  const query = keyword.value.trim();
  if (!query || discovering.value) return;
  const normalized = query.replace(/市$/, '');
  const local = cities.value.find((city) => city.cityName.replace(/市$/, '') === normalized);
  if (local) {
    await selectCity(local);
    return;
  }
  discovering.value = true;
  searchError.value = '';
  success.value = '';
  try {
    const context = await resolveCityContext(query);
    if (!context.city) throw new Error('未找到该城市');
    store.setCityContext(context);
    success.value = `已通过高德发现并切换到${context.city.cityName}`;
    const key = context.city.cityKey || context.city.cityCode;
    if (!cities.value.some((city) => (city.cityKey || city.cityCode) === key)) cities.value.unshift(context.city);
  } catch (value: unknown) {
    console.error('[CitySearch] result=', value);
    searchError.value = value instanceof Error ? value.message : '未找到该城市，请检查名称后重试';
  } finally {
    discovering.value = false;
  }
}

async function locateCity(): Promise<void> {
  locating.value = true;
  success.value = '';
  try {
    const context = await locateCurrentCity();
    success.value = `已定位并切换到${context.city?.cityName || '当前城市'}`;
    addCity(context.city);
  } catch {
    // The current fallback city remains usable when location is unavailable.
  } finally {
    locating.value = false;
  }
}

function addCity(city: CityVO | null): void {
  if (!city) return;
  const key = city.cityKey || city.cityCode;
  if (!cities.value.some((item) => (item.cityKey || item.cityCode) === key)) cities.value.unshift(city);
}

onMounted(async () => {
  await initializeCityContext();
  try {
    cities.value = await listCities();
  } catch (value: unknown) {
    recommendedCitiesError.value = value instanceof Error ? value.message : '城市加载失败';
  } finally {
    [...store.recentCities].reverse().forEach(addCity);
    addCity(store.currentCity);
    loading.value = false;
  }
});
</script>

<style scoped lang="scss">
@import '../primary-page.scss';
.search-row { display: flex; gap: 12px; max-width: 700px; }
.search { flex: 1; margin-top: 0; }
.search-button { margin: 0; padding: 0 22px; color: #fff; background: var(--jade-700); border-radius: 14px; font-size: 14px; line-height: 48px; }
.locate-button { margin: 0; padding: 0 18px; color: var(--jade-700); background: transparent; border: 1px solid rgba(94, 140, 126, .35); border-radius: 14px; font-size: 14px; line-height: 46px; }
.location-error { display: block; margin-top: 10px; color: #9b5f44; font-size: 13px; }
.state--success { color: var(--jade-700); background: rgba(94, 140, 126, .10); }
.state--warning { color: #7b6847; background: rgba(198, 154, 84, .10); }
@media (max-width: 560px) { .search-row { flex-direction: column; } .search-button { width: 100%; } }
</style>
