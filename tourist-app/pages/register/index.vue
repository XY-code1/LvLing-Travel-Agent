<template>
  <view class="register-page">
    <view class="register-hero">
      <image class="register-hero__image" src="/static/images/auth-guide-visual.webp" mode="aspectFill" />
      <view class="register-hero__copy">
        <text class="register-hero__eyebrow">创建游客账号</text>
        <text class="register-hero__title">让导览记住你的偏好</text>
        <text class="register-hero__desc">保存历史对话、兴趣标签和推荐路线。</text>
      </view>
    </view>

    <view class="card form-stack">
      <view class="field-group">
        <text class="field-label">手机号</text>
        <input v-model="form.phone" class="input" type="number" placeholder="请输入手机号" />
      </view>
      <view class="field-group">
        <text class="field-label">昵称</text>
        <input v-model="form.nickname" class="input" placeholder="可选，用于个性化称呼" />
      </view>
      <view class="field-group">
        <text class="field-label">密码</text>
        <input v-model="form.password" class="input" password placeholder="请输入 6-20 位密码" />
      </view>
      <button class="button" :loading="loading" @tap="submit">注册并去登录</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue';

import { registerTourist } from '../../api/auth';

const loading = ref(false);
const form = reactive({
  phone: '',
  password: '',
  nickname: ''
});

function validate(): boolean {
  if (!/^1[3-9]\d{9}$/.test(form.phone)) {
    uni.showToast({ title: '请输入正确手机号', icon: 'none' });
    return false;
  }
  if (form.password.length < 6 || form.password.length > 20) {
    uni.showToast({ title: '密码长度 6-20 位', icon: 'none' });
    return false;
  }
  return true;
}

async function submit(): Promise<void> {
  if (!validate()) {
    return;
  }
  loading.value = true;
  try {
    await registerTourist(form);
    uni.showToast({ title: '注册成功', icon: 'success' });
    uni.redirectTo({ url: '/pages/login/index' });
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : '注册失败';
    uni.showToast({ title: message, icon: 'none' });
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped lang="scss">
.register-page {
  min-height: 100vh;
  padding: 32rpx;
  background:
    radial-gradient(circle at 80% 8%, rgba(217, 119, 6, 0.12), transparent 30%),
    linear-gradient(180deg, #f8fff9 0%, #edf8f0 100%);
}

.register-hero {
  position: relative;
  min-height: 420rpx;
  overflow: hidden;
  border-radius: 40rpx;
  box-shadow: var(--shadow-card);
}

.register-hero__image {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}

.register-hero__copy {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  padding: 36rpx;
  background: linear-gradient(180deg, rgba(6, 78, 59, 0.05), rgba(6, 78, 59, 0.68));
  color: #ffffff;
}

.register-hero__eyebrow,
.register-hero__title,
.register-hero__desc {
  display: block;
}

.register-hero__eyebrow {
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.78);
  font-weight: 700;
}

.register-hero__title {
  margin-top: 12rpx;
  font-size: 44rpx;
  font-weight: 900;
  line-height: 1.18;
}

.register-hero__desc {
  margin-top: 14rpx;
  font-size: 26rpx;
  color: rgba(255, 255, 255, 0.82);
}

.form-stack {
  display: grid;
  gap: 24rpx;
}

.field-group {
  display: grid;
  gap: 12rpx;
}

.field-label {
  font-size: 24rpx;
  font-weight: 700;
  color: var(--text-secondary);
}
</style>
