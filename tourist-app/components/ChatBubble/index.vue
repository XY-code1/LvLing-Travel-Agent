<template>
  <view :class="['bubble-row', role === 'user' ? 'bubble-row--user' : 'bubble-row--ai']">
    <view v-if="role === 'ai'" class="bubble-avatar bubble-avatar--ai">
      <image
        v-if="avatarImage"
        class="bubble-avatar__image"
        :src="avatarImage"
        mode="aspectFill"
      />
      <image
        v-else
        class="bubble-avatar__image"
        src="/static/images/auth-guide-visual.webp"
        mode="aspectFill"
      />
    </view>

    <view :class="['chat-bubble', role === 'user' ? 'chat-bubble--user' : 'chat-bubble--ai']">
      <text class="bubble-text">{{ text }}</text>
    </view>

    <view v-if="role === 'user'" class="bubble-avatar bubble-avatar--user">
      <text class="bubble-avatar__icon">{{ userInitial || '我' }}</text>
    </view>
  </view>
</template>

<script setup lang="ts">
defineProps<{
  role: 'user' | 'ai';
  text: string;
  avatarImage?: string | null;
  userInitial?: string | null;
}>();
</script>

<style scoped lang="scss">
.bubble-row {
  display: flex;
  align-items: flex-end;
  gap: 16rpx;
  margin-bottom: 24rpx;
  padding: 0 8rpx;
}

.bubble-row--user {
  flex-direction: row-reverse;
}

/* 头像 */
.bubble-avatar {
  width: 64rpx;
  height: 64rpx;
  border-radius: 50%;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24rpx;
  font-weight: 900;
  overflow: hidden;
}

.bubble-avatar--ai {
  background: #f0fdf4;
  color: var(--primary-dark);
  box-shadow: 0 4rpx 12rpx rgba(21, 128, 61, 0.18);
}

.bubble-avatar__image {
  width: 100%;
  height: 100%;
}

.bubble-avatar--user {
  background: linear-gradient(135deg, #fff7ed, #fed7aa);
  color: #92400e;
  box-shadow: 0 4rpx 12rpx rgba(217, 119, 6, 0.18);
}

/* 气泡 */
.chat-bubble {
  max-width: 75%;
  padding: 24rpx 28rpx;
  border-radius: 28rpx;
  line-height: 1.65;
  position: relative;
}

.chat-bubble--user {
  background: linear-gradient(135deg, var(--primary) 0%, var(--primary-dark) 100%);
  color: var(--on-primary);
  border-bottom-right-radius: 8rpx;
  box-shadow: 0 4rpx 16rpx rgba(21, 128, 61, 0.24);
}

.chat-bubble--ai {
  background: var(--surface);
  color: var(--text-main);
  border-bottom-left-radius: 8rpx;
  border: 1rpx solid var(--line);
  box-shadow: var(--shadow-soft);
}

.bubble-text {
  font-size: 28rpx;
  line-height: 1.65;
}
</style>
