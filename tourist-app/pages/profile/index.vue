<template>
  <view class="page">
    <view class="hero">
      <text class="hero__eyebrow">个人中心</text>
      <text class="hero__title">{{ form.nickname || '游客资料' }}</text>
      <text class="hero__desc">完善兴趣标签后，系统会优先推荐更合适的路线。</text>
    </view>

    <view class="avatar-panel">
      <view v-if="avatarLoading" class="empty">正在加载数字人形象…</view>
      <AvatarChooser
        v-else-if="store.avatarOptions.length > 0"
        :avatars="store.avatarOptions"
        :selected-id="store.selectedAvatar?.id || form.avatarConfigId || null"
        @select="chooseAvatar"
      />
      <view v-else class="empty">暂无可选数字人形象</view>
    </view>

    <view class="card form-stack">
      <view class="field-group">
        <text class="field-label">昵称</text>
        <input v-model="form.nickname" class="input" placeholder="请输入昵称" />
      </view>
      <view class="field-group">
        <text class="field-label">性别</text>
        <view class="gender-tabs">
          <button
            v-for="item in genderOptions"
            :key="item.value"
            :class="form.gender === item.value ? 'gender-tab gender-tab--active' : 'gender-tab'"
            @tap="form.gender = item.value"
          >
            {{ item.label }}
          </button>
        </view>
      </view>
      <view class="profile-avatar">
        <view class="profile-avatar__preview">
          <image
            v-if="form.avatar"
            class="profile-avatar__image"
            :src="form.avatar"
            mode="aspectFill"
          />
          <text v-else class="profile-avatar__initial">{{ avatarInitial }}</text>
        </view>
        <view class="profile-avatar__body">
          <text class="profile-avatar__label">头像</text>
          <text class="profile-avatar__hint">支持 jpg、png、webp，建议使用清晰正方形图片。</text>
          <button
            class="button--ghost profile-avatar__button"
            :loading="avatarUploading"
            @tap="chooseProfileAvatar"
          >
            {{ form.avatar ? '更换头像' : '上传头像' }}
          </button>
        </view>
      </view>
      <view class="field-group">
        <text class="field-label">兴趣标签</text>
        <input v-model="form.interestTags" class="input" placeholder="如历史、自然、摄影" />
      </view>
      <button class="button" :loading="loading" @tap="saveProfile">保存资料</button>
      <button class="button--ghost" @tap="logout">退出登录</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';

import { getCurrentAvatar, listAvatars, selectAvatar } from '../../api/avatar';
import { logoutTourist } from '../../api/auth';
import { getProfile, updateProfile, uploadAvatar } from '../../api/user';
import AvatarChooser from '../../components/AvatarChooser/index.vue';
import { useTouristStore } from '../../stores';
import type { AvatarConfigVO, TouristProfileVO } from '../../types';
import { requireLogin } from '../../utils/auth';

const store = useTouristStore();
const loading = ref(false);
const avatarLoading = ref(false);
const avatarUploading = ref(false);
const genderOptions = [
  { label: '保密', value: 0 },
  { label: '男', value: 1 },
  { label: '女', value: 2 }
];
const form = reactive({
  nickname: '',
  avatar: '',
  gender: 0,
  interestTags: '',
  avatarConfigId: null as number | null
});
const avatarInitial = computed(() => (form.nickname || '游').slice(0, 1));

onMounted(() => {
  if (requireLogin()) {
    void loadProfile();
    void loadAvatarState();
  }
});

async function loadProfile(): Promise<void> {
  loading.value = true;
  try {
    const data = await getProfile();
    applyProfile(data);
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : '资料加载失败';
    uni.showToast({ title: message, icon: 'none' });
  } finally {
    loading.value = false;
  }
}

async function loadAvatarState(): Promise<void> {
  avatarLoading.value = true;
  try {
    const [avatars, current] = await Promise.all([listAvatars(), getCurrentAvatar()]);
    store.setAvatarOptions(avatars);
    store.setSelectedAvatar(current || avatars[0] || null);
    form.avatarConfigId = store.selectedAvatar?.id || null;
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : '数字人形象加载失败';
    uni.showToast({ title: message, icon: 'none' });
  } finally {
    avatarLoading.value = false;
  }
}

function chooseProfileAvatar(): void {
  if (avatarUploading.value) return;
  uni.chooseImage({
    count: 1,
    sizeType: ['compressed'],
    sourceType: ['album', 'camera'],
    success: (result) => {
      const filePath = result.tempFilePaths?.[0];
      const fileInfo = result.tempFiles?.[0] as { size?: number } | undefined;
      if (!filePath) {
        uni.showToast({ title: '未选择头像图片', icon: 'none' });
        return;
      }
      if (fileInfo?.size && fileInfo.size > 5 * 1024 * 1024) {
        uni.showToast({ title: '头像图片不能超过 5MB', icon: 'none' });
        return;
      }
      void submitProfileAvatar(filePath);
    }
  });
}

async function submitProfileAvatar(filePath: string): Promise<void> {
  avatarUploading.value = true;
  try {
    const data = await uploadAvatar(filePath);
    applyProfile(data);
    uni.showToast({ title: '头像上传成功', icon: 'success' });
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : '头像上传失败';
    uni.showToast({ title: message, icon: 'none' });
  } finally {
    avatarUploading.value = false;
  }
}

async function chooseAvatar(avatar: AvatarConfigVO): Promise<void> {
  avatarLoading.value = true;
  try {
    const selected = await selectAvatar(avatar.id);
    store.setSelectedAvatar(selected);
    store.clearSession();
    form.avatarConfigId = selected.id;
    if (store.profile) {
      store.setProfile({ ...store.profile, avatarConfigId: selected.id });
    }
    uni.showToast({ title: '数字人已切换', icon: 'success' });
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : '切换失败';
    uni.showToast({ title: message, icon: 'none' });
  } finally {
    avatarLoading.value = false;
  }
}

async function saveProfile(): Promise<void> {
  loading.value = true;
  try {
    const data = await updateProfile({
      nickname: form.nickname,
      gender: form.gender,
      interestTags: form.interestTags
    });
    applyProfile(data);
    uni.showToast({ title: '保存成功', icon: 'success' });
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : '保存失败';
    uni.showToast({ title: message, icon: 'none' });
  } finally {
    loading.value = false;
  }
}

async function logout(): Promise<void> {
  try {
    await logoutTourist();
  } finally {
    store.logoutLocal();
    uni.redirectTo({ url: '/pages/login/index' });
  }
}

function applyProfile(data: TouristProfileVO): void {
  store.setProfile(data);
  Object.assign(form, {
    nickname: data.nickname || '',
    avatar: data.avatar || '',
    gender: data.gender || 0,
    interestTags: data.interestTags || '',
    avatarConfigId: data.avatarConfigId || null
  });
}
</script>

<style scoped lang="scss">
.form-stack {
  display: grid;
  gap: 24rpx;
}

.field-group {
  display: grid;
  gap: 12rpx;
}

.field-label {
  color: var(--text-secondary);
  font-size: 25rpx;
  font-weight: 700;
}

.gender-tabs {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14rpx;
}

.gender-tab {
  min-height: 84rpx;
  margin: 0;
  border-radius: var(--radius-sm);
  border: 1.5rpx solid var(--line);
  background: var(--surface);
  color: var(--text-secondary);
  font-size: 27rpx;
  font-weight: 700;
  transition: background 0.15s ease, border-color 0.15s ease, color 0.15s ease;

  &::after {
    border: 0;
  }

  &:active {
    background: var(--surface-soft);
  }
}

.gender-tab--active {
  border-color: var(--primary);
  background: var(--primary-surface);
  color: var(--primary-dark);
}

.avatar-panel {
  margin-top: 24rpx;
  padding: 28rpx;
  border-radius: var(--radius);
  border: 1rpx solid var(--line);
  background: var(--surface);
  box-shadow: var(--shadow-soft);
}

.profile-avatar {
  display: flex;
  align-items: center;
  gap: 24rpx;
  padding: 24rpx;
  border-radius: var(--radius);
  border: 1rpx solid var(--line);
  background: var(--surface-soft);
}

.profile-avatar__preview {
  width: 120rpx;
  height: 120rpx;
  flex: 0 0 120rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border-radius: 50%;
  background: linear-gradient(145deg, #e0f2fe, #f8fafc);
  border: 2rpx solid #ffffff;
  box-shadow: var(--shadow-soft);
}

.profile-avatar__image {
  width: 100%;
  height: 100%;
}

.profile-avatar__initial {
  color: var(--primary-dark);
  font-size: 44rpx;
  font-weight: 800;
}

.profile-avatar__body {
  min-width: 0;
  flex: 1;
  display: grid;
  gap: 10rpx;
}

.profile-avatar__label {
  color: var(--text-main);
  font-size: 30rpx;
  font-weight: 800;
}

.profile-avatar__hint {
  color: var(--text-muted);
  font-size: 24rpx;
  line-height: 1.5;
}

.profile-avatar__button {
  width: fit-content;
  min-width: 180rpx;
  margin: 4rpx 0 0;
}
</style>
