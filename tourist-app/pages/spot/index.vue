<template>
  <view class="page">
    <view v-if="loading" class="empty">正在加载景点详情...</view>
    <view v-else-if="!spot" class="empty">景点不存在或未启用</view>
    <view v-else>
      <view class="hero">
        <text class="hero__eyebrow">景点详情</text>
        <text class="hero__title">{{ spot.name }}</text>
        <text class="hero__desc">{{ spot.intro || '暂无简介' }}</text>
      </view>

      <view class="card">
        <view class="row">
          <text class="tag">{{ spot.canGuide ? '可讲解' : '暂无讲解词' }}</text>
          <text class="muted">建议停留 {{ spot.stayMinutes || 0 }} 分钟</text>
        </view>
        <text class="section-title">讲解词</text>
        <text class="content">{{ spot.guideText || '暂无讲解词，可在问答中继续提问。' }}</text>
      </view>

      <view class="grid-2">
        <button class="button" @tap="askSpot">向导游提问</button>
        <button class="button--ghost" @tap="goRoute">路线推荐</button>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { onLoad } from '@dcloudio/uni-app';

import { getSpotDetail } from '../../api/scenic';
import { navigateTo } from '../../router';
import type { TouristSpotDetailVO } from '../../types';
import { requireLogin } from '../../utils/auth';

const loading = ref(false);
const spot = ref<TouristSpotDetailVO | null>(null);

onLoad((query) => {
  if (!requireLogin()) {
    return;
  }
  const id = Number(query.id);
  if (Number.isInteger(id) && id > 0) {
    void loadSpot(id);
  }
});

async function loadSpot(id: number): Promise<void> {
  loading.value = true;
  try {
    spot.value = await getSpotDetail(id);
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : '景点加载失败';
    uni.showToast({ title: message, icon: 'none' });
  } finally {
    loading.value = false;
  }
}

function askSpot(): void {
  if (!spot.value) {
    return;
  }
  const question = `介绍一下${spot.value.name}`;
  navigateTo(`/pages/chat/index?question=${encodeURIComponent(question)}`);
}

function goRoute(): void {
  navigateTo('/pages/route/index');
}
</script>

<style scoped lang="scss">
.content {
  display: block;
  margin-top: 18rpx;
  line-height: 1.8;
  color: var(--text-main);
}

.grid-2 {
  margin-top: 24rpx;
}
</style>
