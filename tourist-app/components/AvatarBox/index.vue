<template>
  <view :class="['avatar-box', speaking && 'avatar-box--speaking']" @tap="playAudio">
    <view class="avatar-box__stage">
      <view class="avatar-box__scenery">
        <view class="avatar-box__mountain avatar-box__mountain--back" />
        <view class="avatar-box__mountain avatar-box__mountain--front" />
      </view>
      <view class="avatar-box__halo" />
      <image
        v-if="avatarImage"
        class="avatar-box__image"
        :src="avatarImage"
        mode="aspectFit"
        @error="imageFailed = true"
      />
      <view v-else-if="!imageOverride" class="avatar-box__fallback">
        <image
          class="avatar-box__fallback-image"
          src="/static/images/auth-guide-visual.webp"
          mode="aspectFill"
        />
        <view class="avatar-box__fallback-shade" />
      </view>
      <view v-else class="avatar-box__image-error">
        灵灵形象暂时无法加载
      </view>
      <view v-if="speaking" class="avatar-box__voice">
        <view
          v-for="bar in voiceBars"
          :key="bar"
          class="avatar-box__voice-bar"
          :style="{ animationDelay: `${bar * 0.08}s` }"
        />
      </view>
    </view>
    <view class="avatar-box__hud">
      <view class="avatar-box__name">{{ avatarName }}</view>
      <view class="avatar-box__meta">{{ avatarMeta }}</view>
      <view v-if="speaking" class="avatar-box__hint">正在讲解</view>
      <view v-if="audioBlocked" class="avatar-box__hint">点击播放语音</view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue';

import type { AvatarConfigVO } from '../../types';

const props = defineProps<{
  streamUrl?: string | null;
  audioUrl?: string | null;
  connected: boolean;
  avatar?: AvatarConfigVO | null;
  text?: string | null;
  imageOverride?: string | null;
  nameOverride?: string | null;
  metaOverride?: string | null;
}>();

type AudioPlayer = {
  play(): Promise<void>;
  pause(): void;
  stop(): void;
  onPlay(handler: () => void): void;
  onPause(handler: () => void): void;
  onEnded(handler: () => void): void;
};

const audio = ref<AudioPlayer | null>(null);
const audioBlocked = ref(false);
const speaking = ref(false);
const progress = ref(0);
const imageFailed = ref(false);
const voiceBars = [0, 1, 2, 3, 4];
let progressTimer = 0;

const avatarName = computed(() => props.nameOverride || props.avatar?.name || '数字导游');
const avatarMeta = computed(() => {
  if (props.metaOverride) {
    return props.metaOverride;
  }
  if (props.avatar?.outfit) {
    return `${genderText(props.avatar.gender)} / ${props.avatar.outfit}`;
  }
  return props.connected ? '会话已连接' : '等待创建会话';
});

const avatarImage = computed(() => {
  if (imageFailed.value) return '';
  const src = props.imageOverride || props.avatar?.avatarImage || props.avatar?.outfitImage || '';
  return props.imageOverride ? src : versionedImage(src, props.avatar);
});

onBeforeUnmount(() => {
  stopAudio();
});

watch(
  () => [props.avatar?.avatarImage, props.imageOverride],
  () => {
    imageFailed.value = false;
  }
);

watch(
  () => props.audioUrl,
  async (url) => {
    await loadAudio(url || '');
  }
);

async function loadAudio(url: string): Promise<void> {
  stopAudio();
  audioBlocked.value = false;
  progress.value = 0;
  if (!url) {
    speaking.value = false;
    return;
  }
  const nextAudio = createAudioPlayer(url);
  nextAudio.onPlay(() => {
    speaking.value = true;
    startProgressTimer();
  });
  nextAudio.onPause(() => {
    speaking.value = false;
    stopProgressTimer();
  });
  nextAudio.onEnded(() => {
    speaking.value = false;
    progress.value = 1;
    stopProgressTimer();
  });
  audio.value = nextAudio;
  try {
    await nextAudio.play();
  } catch {
    audioBlocked.value = true;
    speaking.value = false;
  }
}

function createAudioPlayer(url: string): AudioPlayer {
  if (typeof uni !== 'undefined' && typeof uni.createInnerAudioContext === 'function') {
    const ctx = uni.createInnerAudioContext();
    ctx.autoplay = false;
    ctx.src = url;
    return {
      play: () => {
        ctx.play();
        return Promise.resolve();
      },
      pause: () => ctx.pause(),
      stop: () => ctx.stop(),
      onPlay: (handler) => { ctx.onPlay(handler); },
      onPause: (handler) => { ctx.onPause(handler); },
      onEnded: (handler) => { ctx.onEnded(handler); }
    };
  }
  const el = new Audio(url);
  el.preload = 'auto';
  return {
    play: () => el.play(),
    pause: () => el.pause(),
    stop: () => { el.pause(); el.src = ''; },
    onPlay: (handler) => { el.onplay = handler; },
    onPause: (handler) => { el.onpause = handler; },
    onEnded: (handler) => { el.onended = handler; }
  };
}

async function playAudio(): Promise<void> {
  if (!audio.value || !audioBlocked.value) {
    return;
  }
  try {
    await audio.value.play();
    audioBlocked.value = false;
  } catch {
    uni.showToast({ title: '音频暂时无法播放', icon: 'none' });
  }
}

function startProgressTimer(): void {
  stopProgressTimer();
  const startedAt = Date.now();
  progressTimer = setInterval(() => {
    progress.value = Math.min(1, (Date.now() - startedAt) / 20000);
  }, 50);
}

function stopProgressTimer(): void {
  if (progressTimer) {
    clearInterval(progressTimer);
    progressTimer = 0;
  }
}

function stopAudio(): void {
  stopProgressTimer();
  if (audio.value) {
    audio.value.stop();
    audio.value = null;
  }
  speaking.value = false;
}

function genderText(value?: string | null): string {
  if (value === 'MALE') {
    return '男性';
  }
  if (value === 'FEMALE') {
    return '女性';
  }
  return '数字人';
}

function versionedImage(src: string, avatar?: AvatarConfigVO | null): string {
  if (!src) {
    return '';
  }
  const version = encodeURIComponent(avatar?.updateTime || String(avatar?.id || ''));
  if (!version) {
    return src;
  }
  return `${src}${src.includes('?') ? '&' : '?'}v=${version}`;
}
</script>

<style scoped lang="scss">
.avatar-box {
  position: relative;
  height: 480rpx;
  overflow: hidden;
  border-radius: 28rpx;
  background: #edf7f2;
  border: 1rpx solid rgba(255, 255, 255, 0.72);
  box-shadow: 0 18rpx 42rpx rgba(24, 72, 55, 0.16);
}

.avatar-box__stage {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar-box__stage::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 74% 18%, rgba(255, 255, 255, 0.72), transparent 22%),
    radial-gradient(circle at 24% 92%, rgba(21, 128, 61, 0.14), transparent 36%),
    linear-gradient(145deg, #f8fffb 0%, #eaf6ee 48%, #e1ecf4 100%);
}

.avatar-box__scenery {
  position: absolute;
  inset: 0;
  z-index: 0;
  overflow: hidden;
}

.avatar-box__mountain {
  position: absolute;
  left: -12%;
  right: -12%;
  bottom: -10rpx;
  height: 142rpx;
  background: rgba(36, 100, 74, 0.13);
  clip-path: polygon(0 78%, 12% 54%, 24% 72%, 38% 38%, 52% 70%, 66% 44%, 80% 70%, 100% 48%, 100% 100%, 0 100%);
}

.avatar-box__mountain--back {
  bottom: 44rpx;
  height: 170rpx;
  background: rgba(38, 92, 80, 0.08);
  filter: blur(1rpx);
}

.avatar-box__mountain--front {
  background: rgba(21, 128, 61, 0.11);
}

.avatar-box__halo {
  position: absolute;
  z-index: 0;
  left: 50%;
  top: 48%;
  width: 330rpx;
  height: 330rpx;
  transform: translate(-50%, -50%);
  border-radius: 50%;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.86) 0%, rgba(255, 255, 255, 0.34) 42%, transparent 70%);
  filter: blur(2rpx);
}

.avatar-box__image {
  position: absolute;
  z-index: 1;
  left: 50%;
  bottom: 10rpx;
  width: 62%;
  height: 94%;
  transform: translateX(-50%);
  filter: drop-shadow(0 20rpx 22rpx rgba(16, 54, 42, 0.18));
  transition: transform 0.22s ease, filter 0.22s ease;
}

.avatar-box--speaking .avatar-box__image {
  transform: translateX(-50%) translateY(-2rpx) scale(1.012);
  filter: drop-shadow(0 24rpx 26rpx rgba(16, 54, 42, 0.23));
}

.avatar-box__voice {
  position: absolute;
  z-index: 2;
  right: 22rpx;
  bottom: 22rpx;
  display: flex;
  align-items: flex-end;
  gap: 6rpx;
  height: 46rpx;
  padding: 10rpx 14rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.72);
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.72);
  box-shadow: 0 10rpx 26rpx rgba(21, 128, 61, 0.16);
  backdrop-filter: blur(14rpx);
}

.avatar-box__voice-bar {
  width: 7rpx;
  height: 14rpx;
  border-radius: 999rpx;
  background: linear-gradient(180deg, #16a34a, #d97706);
  animation: avatar-voice 0.68s ease-in-out infinite;
}

.avatar-box__fallback {
  position: absolute;
  inset: 0;
  z-index: 1;
  overflow: hidden;
}

.avatar-box__fallback-image,
.avatar-box__fallback-shade {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}

.avatar-box__image-error {
  position: absolute;
  z-index: 1;
  inset: 0;
  display: grid;
  place-items: center;
  padding: 24rpx;
  color: rgba(16, 35, 30, .68);
  font-size: 24rpx;
  text-align: center;
}

.avatar-box__fallback-shade {
  background:
    linear-gradient(180deg, rgba(239, 248, 243, 0.12) 0%, rgba(239, 248, 243, 0.18) 42%, rgba(9, 42, 31, 0.42) 100%),
    radial-gradient(circle at 16% 24%, rgba(255, 255, 255, 0.62), transparent 28%);
}

.avatar-box__hud {
  position: absolute;
  z-index: 3;
  left: 24rpx;
  right: 24rpx;
  bottom: 26rpx;
  display: grid;
  gap: 6rpx;
  color: #10231e;
  pointer-events: none;
}

.avatar-box__name {
  display: block;
  width: fit-content;
  max-width: 100%;
  padding: 4rpx 10rpx;
  border-radius: 10rpx;
  background: rgba(255, 255, 255, 0.72);
  font-size: 30rpx;
  font-weight: 800;
  line-height: 1.25;
  overflow-wrap: anywhere;
}

.avatar-box__meta,
.avatar-box__hint {
  width: fit-content;
  max-width: 100%;
  padding: 6rpx 12rpx;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.72);
  color: rgba(16, 35, 30, 0.72);
  font-size: 22rpx;
}

.avatar-box__hint {
  color: #6b3f00;
}

@keyframes avatar-voice {
  0%, 100% {
    height: 12rpx;
    opacity: 0.55;
  }
  50% {
    height: 34rpx;
    opacity: 1;
  }
}
</style>
