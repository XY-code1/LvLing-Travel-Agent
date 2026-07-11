<template>
  <view class="spot-card" @tap="emit('select')">
    <!-- 封面图 / 占位 -->
    <view class="spot-card__media">
      <image
        v-if="spot.coverImage"
        class="spot-card__image"
        :src="spot.coverImage"
        mode="aspectFill"
        lazy-load
      />
      <view v-else class="spot-card__placeholder">
        <image class="spot-card__placeholder-image" src="/static/images/vision-placeholder.webp" mode="aspectFill" />
        <view class="spot-card__placeholder-shade" />
        <text class="spot-card__placeholder-char">{{ spot.name.slice(0, 1) }}</text>
      </view>
    </view>

    <!-- 文字区 -->
    <view class="spot-card__body">
      <text class="spot-card__title">{{ spot.name }}</text>
      <text class="spot-card__sub">{{ subtitle }}</text>
      <view class="spot-card__arrow">
        <text class="spot-card__arrow-text">查看详情 ›</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
defineProps<{
  spot: { spotId: number; name: string; coverImage?: string | null };
  subtitle: string;
}>();

const emit = defineEmits<{ (e: 'select'): void }>();
</script>

<style scoped lang="scss">
.spot-card {
  display: flex;
  gap: 24rpx;
  padding: 24rpx;
  border-radius: var(--radius);
  background: var(--surface);
  border: 1rpx solid var(--line);
  box-shadow: var(--shadow-soft);
  transition: transform 0.15s ease, box-shadow 0.15s ease;

  &:active {
    transform: scale(0.98);
    box-shadow: none;
  }
}

/* 封面媒体区 */
.spot-card__media {
  width: 160rpx;
  height: 128rpx;
  border-radius: 16rpx;
  overflow: hidden;
  flex-shrink: 0;
}

.spot-card__image {
  width: 100%;
  height: 100%;
}

.spot-card__placeholder {
  width: 100%;
  height: 100%;
  position: relative;
  background: var(--primary-surface);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.spot-card__placeholder-image,
.spot-card__placeholder-shade {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}

.spot-card__placeholder-shade {
  background: rgba(6, 78, 59, 0.28);
}

.spot-card__placeholder-char {
  position: relative;
  z-index: 1;
  font-size: 52rpx;
  font-weight: 800;
  color: #ffffff;
  text-shadow: 0 3rpx 12rpx rgba(6, 78, 59, 0.38);
}

/* 文字区 */
.spot-card__body {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  overflow: hidden;
}

.spot-card__title {
  font-size: 30rpx;
  font-weight: 700;
  color: var(--text-main);
  line-height: 1.3;
}

.spot-card__sub {
  font-size: 24rpx;
  color: var(--text-muted);
  line-height: 1.5;
  margin-top: 8rpx;
}

.spot-card__arrow {
  margin-top: 16rpx;
}

.spot-card__arrow-text {
  font-size: 24rpx;
  font-weight: 600;
  color: var(--primary);
}
</style>
