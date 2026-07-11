<template>
  <view class="page">
    <view class="hero">
      <text class="hero__eyebrow">游客反馈</text>
      <text class="hero__title">这次导览体验如何？</text>
      <text class="hero__desc">你的评价会进入感受度分析，帮助景区优化知识库和服务。</text>
    </view>

    <view class="card form-stack">
      <view v-if="submitted" class="submit-state">
        <text class="submit-state__title">反馈已进入后台处理</text>
        <text class="submit-state__desc">管理员处理或回复后，会显示在下方记录里。</text>
      </view>
      <view class="field-group">
        <text class="field-label">本次体验评分</text>
        <view class="score-row">
          <button
            v-for="item in scoreOptions"
            :key="item.value"
            :class="score === item.value ? 'score score--active' : 'score'"
            @tap="score = item.value"
          >
            <text class="score__value">{{ item.value }}</text>
            <text class="score__label">{{ item.label }}</text>
          </button>
        </view>
      </view>
      <view class="field-group">
        <text class="field-label">快捷问题</text>
        <view class="tag-row">
          <button v-for="item in quickTags" :key="item" class="feedback-tag" @tap="appendTag(item)">
            {{ item }}
          </button>
        </view>
      </view>
      <view class="field-group">
        <text class="field-label">具体说明</text>
        <textarea v-model="content" class="textarea" placeholder="写下你的建议或问题，后台可以看到并处理" />
      </view>
      <button class="button" :loading="loading" @tap="submit">提交反馈</button>
    </view>

    <view class="card feedback-history">
      <view class="history-head">
        <text class="history-head__title">我的反馈记录</text>
        <button class="history-head__refresh" :loading="historyLoading" @tap="loadFeedbacks">刷新</button>
      </view>
      <view v-if="historyLoading" class="empty">正在加载处理记录…</view>
      <view v-else-if="feedbacks.length === 0" class="empty">还没有提交过反馈</view>
      <view v-else class="feedback-list">
        <view v-for="item in feedbacks" :key="item.id" class="feedback-item">
          <view class="feedback-item__head">
            <text class="feedback-item__score">{{ item.score || '-' }} 分</text>
            <text :class="statusClass(item.handleStatus)">{{ statusText(item.handleStatus) }}</text>
          </view>
          <text class="feedback-item__content">{{ item.content }}</text>
          <text class="feedback-item__time">{{ formatTime(item.createTime) }}</text>
          <view v-if="item.replyContent" class="feedback-reply">
            <text class="feedback-reply__label">后台回复</text>
            <text class="feedback-reply__content">{{ item.replyContent }}</text>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';

import { listMyFeedback, submitFeedback } from '../../api/route';
import { useTouristStore } from '../../stores';
import type { TouristFeedbackVO } from '../../types';
import { requireLogin } from '../../utils/auth';

const store = useTouristStore();
const scoreOptions = [
  { value: 1, label: '很差' },
  { value: 2, label: '较差' },
  { value: 3, label: '一般' },
  { value: 4, label: '满意' },
  { value: 5, label: '很好' }
];
const quickTags = ['排队太久', '回答不准', '语音不好听', '路线不合理', '图片识别错误'];
const score = ref(5);
const content = ref('');
const loading = ref(false);
const submitted = ref(false);
const historyLoading = ref(false);
const feedbacks = ref<TouristFeedbackVO[]>([]);

onMounted(() => {
  if (requireLogin()) void loadFeedbacks();
});

async function loadFeedbacks(): Promise<void> {
  historyLoading.value = true;
  try {
    feedbacks.value = await listMyFeedback();
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : '反馈记录加载失败';
    uni.showToast({ title: message, icon: 'none' });
  } finally {
    historyLoading.value = false;
  }
}

function appendTag(tag: string): void {
  const text = content.value.trim();
  content.value = text ? `${text}，${tag}` : tag;
}

async function submit(): Promise<void> {
  if (!content.value.trim()) {
    uni.showToast({ title: '请填写反馈内容', icon: 'none' });
    return;
  }
  loading.value = true;
  try {
    await submitFeedback({
      sessionNo: store.currentSessionNo || undefined,
      score: score.value,
      content: content.value.trim()
    });
    submitted.value = true;
    uni.showToast({ title: '已提交后台处理', icon: 'success' });
    content.value = '';
    await loadFeedbacks();
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : '提交失败';
    uni.showToast({ title: message, icon: 'none' });
  } finally {
    loading.value = false;
  }
}

function statusText(value: number | null): string {
  if (value === 1) return '已处理';
  if (value === 2) return '无需处理';
  return '待处理';
}

function statusClass(value: number | null): string {
  if (value === 1) return 'feedback-status feedback-status--done';
  if (value === 2) return 'feedback-status feedback-status--muted';
  return 'feedback-status';
}

function formatTime(value: string | null): string {
  return value ? value.slice(0, 16).replace('T', ' ') : '';
}
</script>

<style scoped lang="scss">
.form-stack {
  display: grid;
  gap: 24rpx;
}

.field-group {
  display: grid;
  gap: 14rpx;
}

.field-label {
  color: var(--text-secondary);
  font-size: 25rpx;
  font-weight: 700;
}

.submit-state {
  display: grid;
  gap: 8rpx;
  padding: 22rpx 24rpx;
  border-radius: var(--radius-sm);
  border: 1rpx solid #bbf7d0;
  background: #f0fdf4;
}

.submit-state__title {
  color: #14532d;
  font-size: 28rpx;
  font-weight: 800;
}

.submit-state__desc {
  color: #166534;
  font-size: 24rpx;
  line-height: 1.5;
}

.score-row {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 12rpx;
}

.score {
  display: grid;
  gap: 4rpx;
  min-height: 96rpx;
  margin: 0;
  border: 1rpx solid var(--line);
  border-radius: var(--radius-sm);
  background: var(--surface);
  color: var(--text-main);
  font-weight: 700;

  &::after {
    border: 0;
  }
}

.score--active {
  border-color: var(--primary);
  background: var(--primary-surface);
  color: var(--primary-dark);
}

.score__value {
  display: block;
  font-size: 32rpx;
  font-weight: 900;
}

.score__label {
  display: block;
  font-size: 20rpx;
  color: var(--text-muted);
}

.score--active .score__label {
  color: var(--primary-dark);
}

.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 14rpx;
}

.feedback-tag {
  min-height: 64rpx;
  margin: 0;
  padding: 0 22rpx;
  border-radius: 999rpx;
  border: 1rpx solid var(--line);
  background: var(--surface-soft);
  color: var(--text-secondary);
  font-size: 24rpx;

  &::after {
    border: 0;
  }
}

.feedback-history {
  display: grid;
  gap: 22rpx;
}

.history-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
}

.history-head__title {
  color: var(--text-main);
  font-size: 32rpx;
  font-weight: 800;
}

.history-head__refresh {
  min-height: 64rpx;
  margin: 0;
  padding: 0 24rpx;
  border-radius: 999rpx;
  border: 1rpx solid var(--line);
  background: var(--surface-soft);
  color: var(--primary);
  font-size: 24rpx;
  font-weight: 700;

  &::after {
    border: 0;
  }
}

.feedback-list {
  display: grid;
  gap: 16rpx;
}

.feedback-item {
  display: grid;
  gap: 12rpx;
  padding: 24rpx;
  border-radius: var(--radius-sm);
  border: 1rpx solid var(--line);
  background: var(--surface-soft);
}

.feedback-item__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
}

.feedback-item__score {
  color: var(--primary-dark);
  font-size: 28rpx;
  font-weight: 900;
}

.feedback-status {
  padding: 7rpx 16rpx;
  border-radius: 999rpx;
  background: #fffbeb;
  color: #92400e;
  font-size: 22rpx;
  font-weight: 700;
}

.feedback-status--done {
  background: #ecfdf5;
  color: #065f46;
}

.feedback-status--muted {
  background: #f1f5f9;
  color: #475569;
}

.feedback-item__content {
  color: var(--text-main);
  font-size: 27rpx;
  line-height: 1.58;
}

.feedback-item__time {
  color: var(--text-muted);
  font-size: 22rpx;
}

.feedback-reply {
  display: grid;
  gap: 8rpx;
  padding: 18rpx;
  border-radius: var(--radius-sm);
  background: #ffffff;
  border: 1rpx solid var(--line);
}

.feedback-reply__label {
  color: var(--primary-dark);
  font-size: 23rpx;
  font-weight: 800;
}

.feedback-reply__content {
  color: var(--text-secondary);
  font-size: 25rpx;
  line-height: 1.55;
}
</style>
