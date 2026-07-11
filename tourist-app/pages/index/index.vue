<template>
  <view class="page" style="padding: 0 0 48rpx; background: var(--app-bg)">
    <view class="home-hero">
      <image class="home-hero__image" src="/static/images/home-hero-scenic.webp" mode="aspectFill" />
      <view class="home-hero__shade" />
      <view class="home-hero__inner">
        <text class="hero__eyebrow">欢迎回来</text>
        <text class="hero__title">{{ nickname }}，今天想看哪段风景？</text>
        <text class="hero__desc">用数字导游讲景点、识照片、规划路线。</text>
      </view>
    </view>

    <view class="home-body">
      <view class="feature-grid">
        <view class="feature-card feature-card--chat" @tap="startChat">
          <image class="feature-card__image" src="/static/images/feature-guide.webp" mode="aspectFill" />
          <view class="feature-card__shade" />
          <view class="feature-card__content">
            <text class="feature-card__label">开始导览</text>
            <text class="feature-card__desc">智能问答景点故事</text>
          </view>
        </view>
        <view class="feature-card feature-card--route" @tap="goRoute">
          <image class="feature-card__image" src="/static/images/feature-route.webp" mode="aspectFill" />
          <view class="feature-card__shade" />
          <view class="feature-card__content">
            <text class="feature-card__label">路线推荐</text>
            <text class="feature-card__desc">按偏好生成游览顺序</text>
          </view>
        </view>
        <view class="feature-card feature-card--vision" @tap="goVision">
          <image class="feature-card__image" src="/static/images/feature-vision.webp" mode="aspectFill" />
          <view class="feature-card__shade" />
          <view class="feature-card__content">
            <text class="feature-card__label">拍照讲解</text>
            <text class="feature-card__desc">拍下景点即刻解说</text>
          </view>
        </view>
        <view class="feature-card feature-card--history" @tap="goHistory">
          <image class="feature-card__image" src="/static/images/feature-history.webp" mode="aspectFill" />
          <view class="feature-card__shade" />
          <view class="feature-card__content">
            <text class="feature-card__label">历史对话</text>
            <text class="feature-card__desc">回看之前的问答记录</text>
          </view>
        </view>
      </view>

      <!-- 热门景点 -->
      <text class="section-title">热门景点</text>
      <view v-if="loading" class="empty">正在加载推荐内容…</view>
      <view v-else-if="hotSpots.length === 0" class="empty">暂无热门景点</view>
      <view v-else class="spot-list">
        <SpotCard
          v-for="spot in hotSpots"
          :key="spot.spotId"
          :spot="spot"
          subtitle="查看讲解词和问答入口"
          @select="goSpot(spot.spotId)"
        />
      </view>

      <!-- 推荐问题 -->
      <text class="section-title">大家都在问</text>
      <view class="question-list">
        <view
          v-for="item in questions"
          :key="item"
          class="question-chip"
          @tap="askQuestion(item)"
        >
          <text class="question-chip__text">{{ item }}</text>
          <text class="question-chip__arrow">›</text>
        </view>
      </view>

      <!-- 底部辅助入口 -->
      <view class="grid-2 bottom-actions">
        <button class="button--ghost" @tap="goProfile">个人中心</button>
        <button class="button--ghost" @tap="goFeedback">反馈体验</button>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';

import { getCurrentAvatar } from '../../api/avatar';
import { createSession } from '../../api/chat';
import { getHotScenic } from '../../api/scenic';
import SpotCard from '../../components/SpotCard/index.vue';
import { navigateTo } from '../../router';
import { useTouristStore } from '../../stores';
import type { HotSpotVO } from '../../types';
import { requireLogin } from '../../utils/auth';

const store    = useTouristStore();
const loading  = ref(false);
const hotSpots = ref<HotSpotVO[]>([]);
const questions = ref<string[]>([]);
const nickname  = computed(() => store.touristInfo?.nickname || '游客');

async function loadHome(): Promise<void> {
  loading.value = true;
  try {
    const data = await getHotScenic();
    hotSpots.value  = data.hotSpots;
    questions.value = data.recommendQuestions;
  } catch (error: unknown) {
    uni.showToast({ title: error instanceof Error ? error.message : '首页加载失败', icon: 'none' });
  } finally {
    loading.value = false;
  }
}

async function ensureSession(): Promise<string> {
  if (!store.selectedAvatar) {
    store.setSelectedAvatar(await getCurrentAvatar());
  }
  const avatarId = store.selectedAvatar?.id;
  if (store.currentSessionNo && (!avatarId || store.currentSessionAvatarId === avatarId)) return store.currentSessionNo;
  const session = await createSession(store.currentScenicId, avatarId);
  const selected = session.selectedAvatar || session.defaultAvatar || store.selectedAvatar;
  store.setSession(session.sessionNo, session.scenicId, selected?.id || null);
  store.setSelectedAvatar(selected || null);
  return session.sessionNo;
}

async function startChat(): Promise<void> {
  try {
    await ensureSession();
    navigateTo('/pages/chat/index');
  } catch (error: unknown) {
    uni.showToast({ title: error instanceof Error ? error.message : '创建会话失败', icon: 'none' });
  }
}

function askQuestion(question: string): void {
  navigateTo(`/pages/chat/index?question=${encodeURIComponent(question)}`);
}

function goSpot(id: number): void     { navigateTo(`/pages/spot/index?id=${id}`); }
function goRoute(): void              { navigateTo('/pages/route/index'); }
function goVision(): void             { navigateTo('/pages/vision/index'); }
function goHistory(): void            { navigateTo('/pages/history/index'); }
function goProfile(): void            { navigateTo('/pages/profile/index'); }
function goFeedback(): void           { navigateTo('/pages/feedback/index'); }

onMounted(() => { if (requireLogin()) void loadHome(); });
</script>

<style scoped lang="scss">
.home-hero {
  min-height: 560rpx;
  padding: 0 32rpx 42rpx;
  border-radius: 0 0 48rpx 48rpx;
  position: relative;
  overflow: hidden;
  box-shadow: var(--shadow-float);
}

.home-hero__image,
.home-hero__shade {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}

.home-hero__image {
  z-index: 0;
}

.home-hero__shade {
  z-index: 1;
  background:
    linear-gradient(180deg, rgba(6, 78, 59, 0.18) 0%, rgba(6, 78, 59, 0.72) 78%, rgba(6, 78, 59, 0.88) 100%),
    linear-gradient(90deg, rgba(6, 78, 59, 0.70) 0%, rgba(6, 78, 59, 0.22) 100%);
}

.home-hero__inner {
  position: relative;
  z-index: 2;
  padding-top: 92rpx;
  display: grid;
  gap: 12rpx;
}

.home-body {
  padding: 0 32rpx;
  margin-top: -24rpx;
  position: relative;
  z-index: 3;
}

/* 问题 chip */
.question-list { display: grid; gap: 16rpx; margin-top: 16rpx; }
.question-chip {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 28rpx 32rpx;
  border-radius: var(--radius);
  background: rgba(255, 255, 255, 0.92);
  border: 1rpx solid var(--line);
  box-shadow: var(--shadow-soft);
  transition: background 0.15s ease, transform 0.15s ease;

  &:active {
    background: var(--surface-soft);
    transform: scale(0.99);
  }
}
.question-chip__text {
  flex: 1;
  font-size: 28rpx;
  color: var(--text-main);
  line-height: 1.5;
}
.question-chip__arrow {
  font-size: 36rpx;
  color: var(--text-muted);
  margin-left: 16rpx;
}

.spot-list { display: grid; gap: 16rpx; margin-top: 16rpx; }

.bottom-actions { margin-top: 40rpx; }
</style>
