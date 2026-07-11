<template>
  <view class="login-bg">
    <view class="auth-hero">
      <image class="auth-visual" src="/static/images/auth-guide-visual.webp" mode="aspectFill" />
      <view class="auth-hero__badge">
        <text class="auth-hero__badge-text">景区智能导游</text>
      </view>
      <text class="auth-hero__title">探索每一处风景的故事</text>
      <text class="auth-hero__desc">登录后保存兴趣偏好、历史对话和个性化路线</text>
    </view>

    <view class="auth-card">
      <text class="auth-card__title">游客登录</text>

      <view class="field-group">
        <view class="field-label">手机号</view>
        <input
          v-model="form.phone"
          class="input"
          type="number"
          placeholder="请输入手机号"
          placeholder-class="placeholder"
        />
      </view>

      <view class="field-group">
        <view class="field-label">密码</view>
        <input
          v-model="form.password"
          class="input"
          password
          placeholder="请输入密码（至少 6 位）"
          placeholder-class="placeholder"
        />
      </view>

      <button class="button" :loading="loading" @tap="submit">
        {{ loading ? '登录中…' : '立即登录' }}
      </button>

      <view class="auth-divider">
        <view class="auth-divider__line" /><text class="auth-divider__text">还没有账号</text><view class="auth-divider__line" />
      </view>

      <button class="button--ghost" @tap="goRegister">注册新账号</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue';

import { loginTourist } from '../../api/auth';
import { useTouristStore } from '../../stores';

const store   = useTouristStore();
const loading = ref(false);
const form    = reactive({ phone: '', password: '' });

function validate(): boolean {
  if (!/^1[3-9]\d{9}$/.test(form.phone)) {
    uni.showToast({ title: '请输入正确的 11 位手机号', icon: 'none' });
    return false;
  }
  if (form.password.length < 6) {
    uni.showToast({ title: '密码不能少于 6 位', icon: 'none' });
    return false;
  }
  return true;
}

async function submit(): Promise<void> {
  if (!validate()) return;
  loading.value = true;
  try {
    const data = await loginTourist(form);
    store.setLogin(data.token, data.touristInfo);
    uni.showToast({ title: '登录成功', icon: 'success' });
    uni.redirectTo({ url: '/pages/index/index' });
  } catch (error: unknown) {
    uni.showToast({ title: error instanceof Error ? error.message : '登录失败', icon: 'none' });
  } finally {
    loading.value = false;
  }
}

function goRegister(): void {
  uni.navigateTo({ url: '/pages/register/index' });
}
</script>

<style scoped lang="scss">
.login-bg {
  min-height: 100vh;
  padding: 0 32rpx 48rpx;
  background:
    radial-gradient(circle at 18% 6%, rgba(217, 119, 6, 0.13), transparent 32%),
    linear-gradient(180deg, #f8fff9 0%, #edf8f0 100%);
  position: relative;
  overflow: hidden;
}

.auth-hero {
  padding: 64rpx 0 28rpx;
  color: var(--text-main);
  position: relative;
}

.auth-visual {
  width: 100%;
  height: 420rpx;
  border-radius: 40rpx;
  box-shadow: var(--shadow-card);
  border: 1rpx solid rgba(255, 255, 255, 0.72);
  overflow: hidden;
  margin-bottom: 28rpx;
}

.auth-hero__badge {
  display: inline-flex;
  align-items: center;
  padding: 10rpx 22rpx;
  margin-bottom: 18rpx;
  border-radius: 999rpx;
  background: var(--primary-surface);
  border: 1rpx solid var(--line);
  backdrop-filter: blur(8px);
}

.auth-hero__badge-text {
  font-size: 24rpx;
  color: var(--primary-dark);
  font-weight: 800;
}

.auth-hero__title {
  display: block;
  font-size: 46rpx;
  font-weight: 800;
  color: var(--text-main);
  line-height: 1.2;
}

.auth-hero__desc {
  display: block;
  margin-top: 16rpx;
  font-size: 26rpx;
  color: var(--text-muted);
  line-height: 1.65;
}

.auth-card {
  position: relative;
  z-index: 1;
  padding: 40rpx 36rpx 36rpx;
  border-radius: 36rpx;
  background: rgba(255, 255, 255, 0.94);
  border: 1rpx solid rgba(220, 235, 226, 0.90);
  box-shadow: var(--shadow-float);
  backdrop-filter: blur(18px);
}
.auth-card__title {
  display: block;
  font-size: 36rpx;
  font-weight: 800;
  color: var(--text-main);
  margin-bottom: 32rpx;
}

.field-group { margin-bottom: 24rpx; }
.field-label {
  font-size: 24rpx;
  font-weight: 600;
  color: var(--text-muted);
  margin-bottom: 12rpx;
}
.placeholder { color: var(--text-placeholder); }

.button { margin-top: 8rpx; }

/* 分割线 */
.auth-divider {
  display: flex;
  align-items: center;
  gap: 20rpx;
  margin: 28rpx 0;
}
.auth-divider__line {
  flex: 1;
  height: 1rpx;
  background: var(--line);
}
.auth-divider__text {
  font-size: 24rpx;
  color: var(--text-muted);
  white-space: nowrap;
}
</style>
