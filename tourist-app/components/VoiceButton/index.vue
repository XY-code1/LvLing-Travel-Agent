<template>
  <view
    :class="['voice-btn', recording && 'voice-btn--recording', loading && 'voice-btn--loading']"
    @tap="emit('toggle')"
  >
    <!-- 录音时的脉冲环 -->
    <view v-if="recording" class="pulse-ring" />

    <!-- 图标 -->
    <view class="voice-btn__icon">
      <text v-if="loading">…</text>
      <text v-else-if="recording">■</text>
      <text v-else>声</text>
    </view>
    <text class="voice-btn__label">
      {{ loading ? '识别中' : recording ? '停止录音' : '语音提问' }}
    </text>
  </view>
</template>

<script setup lang="ts">
defineProps<{ recording: boolean; loading: boolean }>();
const emit = defineEmits<{ (e: 'toggle'): void }>();
</script>

<style scoped lang="scss">
.voice-btn {
  position: relative;
  min-height: 88rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12rpx;
  border-radius: var(--radius);
  border: 2rpx solid var(--primary);
  background: var(--primary-surface);
  color: var(--primary);
  font-size: 28rpx;
  font-weight: 700;
  overflow: visible;
  transition: background 0.2s ease, border-color 0.2s ease;
}

.voice-btn--recording {
  background: #fef2f2;
  border-color: var(--danger);
  color: var(--danger);
}

.voice-btn--loading {
  opacity: 0.7;
}

.voice-btn__icon {
  font-size: 32rpx;
  line-height: 1;
}

.voice-btn__label {
  font-size: 26rpx;
  font-weight: 700;
}

/* 录音时脉冲扩散动画 */
.pulse-ring {
  position: absolute;
  inset: -8rpx;
  border-radius: calc(var(--radius) + 8rpx);
  border: 2rpx solid var(--danger);
  opacity: 0;
  animation: pulse-expand 1.4s ease-out infinite;
  pointer-events: none;
}

@keyframes pulse-expand {
  0%   { transform: scale(1);    opacity: 0.6; }
  100% { transform: scale(1.12); opacity: 0;   }
}
</style>
