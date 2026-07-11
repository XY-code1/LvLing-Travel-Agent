<template>
  <view class="page">
    <view class="hero">
      <text class="hero__eyebrow">个性化路线</text>
      <text class="hero__title">按兴趣推荐游览顺序</text>
      <text class="hero__desc">不填兴趣时，系统会使用个人中心里的兴趣标签。</text>
    </view>

    <view class="card form-stack">
      <input v-model="interest" class="input" placeholder="兴趣，如历史、自然、亲子、摄影" />
      <button class="button" :loading="loading" @tap="loadRoutes">生成路线</button>
    </view>

    <view class="card locate-card">
      <view class="row">
        <view>
          <text class="section-title">当前位置辅助</text>
          <text class="muted">{{ locateText }}</text>
        </view>
        <button class="button button--ghost" :loading="locating" @tap="locateByGps">GPS</button>
      </view>
      <view v-if="nearbySpots.length" class="spot-chain">
        <text v-for="spot in nearbySpots" :key="spot.spotId" class="tag" @tap="goSpot(spot.spotId)">
          {{ spot.name }} · {{ formatDistance(spot.distanceMeters) }}
        </text>
      </view>
      <view v-else class="fallback-actions">
        <button class="button button--ghost" :loading="photoLocating" @tap="locateByPhoto">拍照定位</button>
        <button class="button button--ghost" @tap="showManualSpots">手动选景点</button>
      </view>
    </view>

    <view v-if="manualSpots.length" class="card locate-card">
      <text class="section-title">手动选择景点</text>
      <view class="spot-chain">
        <text v-for="spot in manualSpots" :key="spot.spotId" class="tag" @tap="goSpot(spot.spotId)">
          {{ spot.name }}
        </text>
      </view>
    </view>

    <view v-if="routes.length === 0 && !loading" class="empty">暂无路线推荐</view>
    <view v-for="route in routes" :key="route.routeId" class="card route-card">
      <view class="row">
        <text class="route-card__title">{{ route.name }}</text>
        <text class="tag">{{ route.estimateMinutes || 0 }} 分钟</text>
      </view>
      <text class="muted">{{ route.recommendReason }}</text>
      <view class="spot-chain">
        <text v-for="spot in route.spots" :key="spot.spotId" class="tag" @tap="goSpot(spot.spotId)">
          {{ spot.sortOrder }}. {{ spot.name }}
        </text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';

import { getCurrentAvatar } from '../../api/avatar';
import { recommendRoute } from '../../api/route';
import { createSession } from '../../api/chat';
import { getHotScenic, getNearbySpots } from '../../api/scenic';
import { recognizeSpot } from '../../api/vision';
import { navigateTo } from '../../router';
import { useTouristStore } from '../../stores';
import type { HotSpotVO, NearbySpotVO, RouteRecommendVO } from '../../types';
import { requireLogin } from '../../utils/auth';

const store = useTouristStore();
const interest = ref('');
const loading = ref(false);
const locating = ref(false);
const photoLocating = ref(false);
const locateText = ref('先尝试 GPS，失败时可拍照定位；仍不可用时手动选景点。');
const routes = ref<RouteRecommendVO[]>([]);
const nearbySpots = ref<NearbySpotVO[]>([]);
const manualSpots = ref<HotSpotVO[]>([]);

onMounted(() => {
  if (requireLogin()) {
    void loadRoutes();
    void locateByGps();
  }
});

async function loadRoutes(): Promise<void> {
  loading.value = true;
  try {
    routes.value = await recommendRoute(store.currentScenicId, interest.value || undefined);
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : '路线加载失败';
    uni.showToast({ title: message, icon: 'none' });
  } finally {
    loading.value = false;
  }
}

async function locateByGps(): Promise<void> {
  locating.value = true;
  manualSpots.value = [];
  uni.getLocation({
    type: 'gcj02',
    success: (response: { longitude: number; latitude: number }) => {
      void loadNearby(response.longitude, response.latitude);
    },
    fail: () => {
      locateText.value = 'GPS 不可用，请使用拍照定位；如果图片识别也不可用，可以手动选景点。';
      nearbySpots.value = [];
      locating.value = false;
    }
  });
}

async function loadNearby(longitude: number, latitude: number): Promise<void> {
  try {
    nearbySpots.value = await getNearbySpots(store.currentScenicId, longitude, latitude);
    locateText.value = nearbySpots.value.length
      ? '已按当前位置推荐附近景点。'
      : '当前位置附近暂无可讲解景点，可拍照定位或手动选择。';
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : '附近景点加载失败';
    locateText.value = `${message}，可拍照定位或手动选择。`;
    nearbySpots.value = [];
  } finally {
    locating.value = false;
  }
}

async function locateByPhoto(): Promise<void> {
  photoLocating.value = true;
  try {
    const imagePath = await chooseLocationImage();
    const sessionNo = await ensureSession();
    const result = await recognizeSpot(sessionNo, imagePath);
    if (result.recognizedSpot) {
      const spotName = result.recognizedSpot.spotName || result.recognizedSpot.name;
      locateText.value = `照片识别为 ${spotName}，已进入对应景点。`;
      goSpot(result.recognizedSpot.spotId);
      return;
    }
    locateText.value = '图片未识别出明确景点，请手动选择景点。';
    await showManualSpots();
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : '拍照定位失败';
    locateText.value = `${message}，请手动选择景点。`;
    await showManualSpots();
  } finally {
    photoLocating.value = false;
  }
}

function chooseLocationImage(): Promise<string> {
  return new Promise((resolve, reject) => {
    uni.chooseImage({
      count: 1,
      sourceType: ['camera', 'album'],
      success: (response: { tempFilePaths: string[] }) => resolve(response.tempFilePaths[0]),
      fail: () => reject(new Error('未选择图片'))
    });
  });
}

async function ensureSession(): Promise<string> {
  if (!store.selectedAvatar) {
    store.setSelectedAvatar(await getCurrentAvatar());
  }
  const avatarId = store.selectedAvatar?.id;
  if (store.currentSessionNo && (!avatarId || store.currentSessionAvatarId === avatarId)) {
    return store.currentSessionNo;
  }
  const session = await createSession(store.currentScenicId, avatarId);
  const selected = session.selectedAvatar || session.defaultAvatar || store.selectedAvatar;
  store.setSession(session.sessionNo, session.scenicId, selected?.id || null);
  store.setSelectedAvatar(selected || null);
  return session.sessionNo;
}

async function showManualSpots(): Promise<void> {
  const home = await getHotScenic();
  manualSpots.value = home.hotSpots;
  locateText.value = home.hotSpots.length ? '请选择当前位置附近的景点继续讲解。' : '暂无可手动选择的热门景点。';
}

function formatDistance(distanceMeters: number): string {
  if (distanceMeters >= 1000) {
    return `${(distanceMeters / 1000).toFixed(1)}km`;
  }
  return `${distanceMeters}m`;
}

function goSpot(id: number): void {
  navigateTo(`/pages/spot/index?id=${id}`);
}
</script>

<style scoped lang="scss">
.form-stack,
.route-card,
.locate-card {
  display: grid;
  gap: 20rpx;
}

.route-card__title {
  font-size: 32rpx;
  font-weight: 700;
}

.spot-chain {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
}

.fallback-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16rpx;
}
</style>
