<template>
  <view class="page">
    <view class="hero">
      <text class="hero__eyebrow">拍照识别景点</text>
      <text class="hero__title">把镜头对准眼前的风景</text>
      <text class="hero__desc">多模态模型只负责看图转文字，讲解由知识库和文本大模型完成。</text>
    </view>

    <AvatarBox
      :connected="Boolean(store.currentSessionNo)"
      :audio-url="result?.audioUrl || null"
      :avatar="store.selectedAvatar"
      :text="result?.answer || ''"
    />

    <view class="card preview-card" @tap="chooseImage">
      <image v-if="imagePath" class="preview-card__image" :src="imagePath" mode="aspectFill" />
      <view v-else class="preview-card__placeholder">
        <image class="preview-card__placeholder-image" src="/static/images/vision-placeholder.webp" mode="aspectFill" />
        <view class="preview-card__placeholder-shade" />
        <view class="preview-card__placeholder-copy">
          <text class="preview-card__placeholder-title">选择或拍摄景点照片</text>
          <text class="preview-card__placeholder-desc">系统先看图转文字，再交给文本大模型生成讲解。</text>
        </view>
      </view>
    </view>

    <button class="button" :loading="loading" @tap="submit">识别并讲解</button>

    <view v-if="result" class="card result-card">
      <text class="section-title">{{ result.recognizedSpot?.spotName || result.recognizedSpot?.name || '未识别出明确景点' }}</text>
      <text class="muted">{{ result.answer }}</text>
      <view class="row">
        <text class="tag">{{ result.hitKb === 1 ? '知识库命中' : '兜底讲解' }}</text>
        <text class="muted">{{ result.costMs }} ms</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';

import { getCurrentAvatar } from '../../api/avatar';
import { createSession } from '../../api/chat';
import { recognizeSpot } from '../../api/vision';
import AvatarBox from '../../components/AvatarBox/index.vue';
import { useTouristStore } from '../../stores';
import type { VisionRecognizeVO } from '../../types';
import { requireLogin } from '../../utils/auth';

const store = useTouristStore();
const imagePath = ref('');
const loading = ref(false);
const result = ref<VisionRecognizeVO | null>(null);

onMounted(() => {
  requireLogin();
});

async function ensureSession(): Promise<string> {
  if (!store.selectedAvatar) {
    store.setSelectedAvatar(await getCurrentAvatar());
  }
  await store.ensureScenicId();
  const avatarId = store.selectedAvatar?.id;
  if (store.currentSessionNo) {
    if (!avatarId || store.currentSessionAvatarId === avatarId) {
      return store.currentSessionNo;
    }
  }
  const session = await createSession(store.currentScenicId, avatarId);
  const selected = session.selectedAvatar || session.defaultAvatar || store.selectedAvatar;
  store.setSession(session.sessionNo, session.scenicId, selected?.id || null);
  store.setSelectedAvatar(selected || null);
  return session.sessionNo;
}

function chooseImage(): void {
  uni.chooseImage({
    count: 1,
    sourceType: ['camera', 'album'],
    success: (response) => {
      imagePath.value = response.tempFilePaths[0];
      result.value = null;
    }
  });
}

async function submit(): Promise<void> {
  if (!imagePath.value) {
    uni.showToast({ title: '请先选择图片', icon: 'none' });
    return;
  }
  loading.value = true;
  try {
    result.value = await recognizeSpot(await ensureSession(), imagePath.value);
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : '识别失败';
    uni.showToast({ title: message, icon: 'none' });
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped lang="scss">
.preview-card {
  min-height: 420rpx;
  display: grid;
  place-items: center;
  padding: 0;
  overflow: hidden;
}

.preview-card__image {
  width: 100%;
  height: 420rpx;
  border-radius: var(--radius);
}

.preview-card__placeholder {
  position: relative;
  width: 100%;
  height: 420rpx;
  overflow: hidden;
  border-radius: var(--radius);
}

.preview-card__placeholder-image,
.preview-card__placeholder-shade {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}

.preview-card__placeholder-shade {
  background: linear-gradient(180deg, rgba(6, 78, 59, 0.04), rgba(6, 78, 59, 0.72));
}

.preview-card__placeholder-copy {
  position: absolute;
  left: 28rpx;
  right: 28rpx;
  bottom: 28rpx;
  display: grid;
  gap: 10rpx;
  color: #ffffff;
}

.preview-card__placeholder-title {
  font-size: 34rpx;
  font-weight: 900;
}

.preview-card__placeholder-desc {
  font-size: 24rpx;
  line-height: 1.55;
  color: rgba(255, 255, 255, 0.82);
}

.result-card {
  display: grid;
  gap: 18rpx;
}
</style>
