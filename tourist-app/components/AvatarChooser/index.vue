<template>
  <view class="avatar-chooser">
    <view class="avatar-chooser__head">
      <text class="avatar-chooser__title">选择数字人形象</text>
      <text class="avatar-chooser__hint">音色会随形象自动匹配</text>
    </view>
    <view class="avatar-chooser__list">
      <view
        v-for="item in avatars"
        :key="item.id"
        :class="['avatar-card', selectedId === item.id && 'avatar-card--active']"
        @tap="$emit('select', item)"
      >
        <view class="avatar-card__media">
          <image
            v-if="item.avatarImage"
            class="avatar-card__image"
            :src="avatarImageSrc(item)"
            mode="aspectFit"
          />
          <view v-else class="avatar-card__empty">暂无图片</view>
        </view>
        <text class="avatar-card__name">{{ item.name }}</text>
        <text class="avatar-card__meta">{{ genderText(item.gender) }} · {{ item.outfit || '默认服饰' }}</text>
        <text class="avatar-card__voice">{{ voiceText(item) }}</text>
        <text v-if="selectedId === item.id" class="avatar-card__badge">当前使用</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import type { AvatarConfigVO } from '../../types';

defineProps<{
  avatars: AvatarConfigVO[];
  selectedId?: number | null;
}>();

defineEmits<{
  select: [avatar: AvatarConfigVO];
}>();

function genderText(value: string | null | undefined): string {
  if (value === 'MALE') {
    return '男性';
  }
  if (value === 'FEMALE') {
    return '女性';
  }
  return '数字人';
}

function voiceText(avatar: AvatarConfigVO): string {
  if (avatar.voice === 'xiaogang') {
    return '沉稳男声';
  }
  if (avatar.voice === 'xiaoyun') {
    return '温柔女声';
  }
  return avatar.voice || '默认音色';
}

function avatarImageSrc(avatar: AvatarConfigVO): string {
  const src = avatar.avatarImage || avatar.outfitImage || '';
  if (!src) {
    return '';
  }
  const version = encodeURIComponent(avatar.updateTime || String(avatar.id));
  return `${src}${src.includes('?') ? '&' : '?'}v=${version}`;
}
</script>

<style scoped lang="scss">
.avatar-chooser {
  display: grid;
  gap: 20rpx;
  min-width: 0;
}

.avatar-chooser__head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16rpx;
}

.avatar-chooser__title {
  font-size: 30rpx;
  font-weight: 800;
  color: var(--text-main);
}

.avatar-chooser__hint {
  font-size: 22rpx;
  color: var(--text-muted);
}

.avatar-chooser__list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 20rpx;
  min-width: 0;
  padding: 2rpx;
}

.avatar-card {
  position: relative;
  box-sizing: border-box;
  min-width: 0;
  min-height: 340rpx;
  display: grid;
  grid-template-rows: 188rpx auto auto auto;
  gap: 10rpx;
  padding: 18rpx;
  border-radius: var(--radius);
  border: 2rpx solid var(--line);
  background: var(--surface);
  box-shadow: var(--shadow-soft);
}

.avatar-card--active {
  border-color: var(--primary);
  box-shadow: 0 8rpx 24rpx rgba(21, 128, 61, 0.18);
}

.avatar-card__media,
.avatar-card__empty {
  position: relative;
  width: 100%;
  height: 188rpx;
  border-radius: 12rpx;
  background:
    radial-gradient(circle at 50% 40%, rgba(255, 255, 255, 0.92), transparent 40%),
    linear-gradient(145deg, #f8fffb 0%, #e8f5ee 58%, #eef4f8 100%);
  overflow: hidden;
}

.avatar-card__media::after {
  content: '';
  position: absolute;
  left: 18%;
  right: 18%;
  bottom: 8rpx;
  height: 16rpx;
  border-radius: 50%;
  background: rgba(15, 70, 52, 0.12);
  filter: blur(8rpx);
}

.avatar-card__image {
  position: relative;
  z-index: 1;
  width: 100%;
  height: 100%;
  filter: drop-shadow(0 8rpx 10rpx rgba(16, 54, 42, 0.13));
}

.avatar-card__empty {
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-muted);
  font-size: 24rpx;
}

.avatar-card__name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 26rpx;
  font-weight: 800;
  color: var(--text-main);
}

.avatar-card__meta,
.avatar-card__voice {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 22rpx;
  color: var(--text-muted);
}

.avatar-card__badge {
  position: absolute;
  right: 12rpx;
  top: 12rpx;
  max-width: calc(100% - 24rpx);
  box-sizing: border-box;
  padding: 6rpx 10rpx;
  border-radius: 999rpx;
  background: var(--primary);
  color: var(--on-primary);
  font-size: 20rpx;
  font-weight: 700;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 360px) {
  .avatar-chooser__list {
    grid-template-columns: 1fr;
  }
}

@media (min-width: 768px) {
  .avatar-chooser__list {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}
</style>
