<template>
  <view class="page">
    <view class="hero">
      <text class="hero__eyebrow">历史对话</text>
      <text class="hero__title">回看你的导览记录</text>
      <text class="hero__desc">选择会话后可以继续追问或查看消息。</text>
    </view>

    <view v-if="loading" class="empty">正在加载历史记录...</view>
    <view v-else-if="sessions.length === 0" class="empty">暂无历史会话，先去文本问答里问一个问题。</view>
    <view v-for="item in sessions" :key="item.sessionNo" class="history-card" @tap="openSession(item)">
      <view class="row">
        <text class="session-title">{{ item.title }}</text>
        <text class="tag">{{ item.messageCount }} 条</text>
      </view>
      <text class="history-card__time">{{ item.lastTime }}</text>
      <text class="history-card__action">继续查看</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { onShow } from '@dcloudio/uni-app';

import { getSessionHistory } from '../../api/chat';
import { navigateTo } from '../../router';
import { useTouristStore } from '../../stores';
import type { SessionVO } from '../../types';
import { requireLogin } from '../../utils/auth';

const store = useTouristStore();
const loading = ref(false);
const sessions = ref<SessionVO[]>([]);

onShow(() => {
  if (requireLogin()) {
    void loadHistory();
  }
});

async function loadHistory(): Promise<void> {
  loading.value = true;
  try {
    sessions.value = (await getSessionHistory()).filter((item) => (item.messageCount || 0) > 0);
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : '历史记录加载失败';
    uni.showToast({ title: message, icon: 'none' });
  } finally {
    loading.value = false;
  }
}

function openSession(item: SessionVO): void {
  store.setSession(item.sessionNo, item.scenicId);
  navigateTo('/pages/chat/index');
}
</script>

<style scoped lang="scss">
.history-card {
  position: relative;
  display: grid;
  gap: 12rpx;
  margin-top: 16rpx;
  padding: 24rpx;
  border: 1rpx solid var(--line);
  border-radius: var(--radius);
  background: rgba(255, 255, 255, 0.94);
  box-shadow: var(--shadow-soft);
}

.history-card:active {
  border-color: var(--primary);
  background: var(--surface-soft);
}

.session-title {
  min-width: 0;
  flex: 1;
  font-size: 32rpx;
  font-weight: 800;
  color: var(--text-main);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.history-card__time {
  color: var(--text-muted);
  font-size: 24rpx;
}

.history-card__action {
  justify-self: flex-start;
  color: var(--primary-dark);
  font-size: 24rpx;
  font-weight: 700;
}
</style>
