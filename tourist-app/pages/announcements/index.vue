<template>
  <view class="entry-page">
    <AppHeader />
    <PageContainer>
      <view class="page-head">
        <text class="eyebrow">CITY NOTICE</text>
        <text class="title">{{ cityName }}公告</text>
        <text class="subtitle">仅展示当前城市上下文中的真实公告；动态城市无公告时保持空状态。</text>
      </view>
      <view v-if="loading" class="state">正在加载公告…</view>
      <view v-else-if="error" class="state state--error">{{ error }}</view>
      <view v-else-if="!announcements.length" class="state">当前城市暂无公告</view>
      <view v-else class="card-grid">
        <view v-for="(notice, index) in announcements" :key="noticeKey(notice, index)" class="content-card">
          <text class="card-title">{{ noticeTitle(notice) }}</text>
          <text class="card-copy">{{ noticeContent(notice) }}</text>
        </view>
      </view>
    </PageContainer>
  </view>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import AppHeader from '../../components/layout/AppHeader.vue';
import PageContainer from '../../components/layout/PageContainer.vue';
import { initializeCityContext } from '../../composables/useCityContext';
import { useTouristStore } from '../../stores';

const store = useTouristStore();
const loading = ref(true);
const error = ref('');
const cityName = computed(() => store.experienceCityContext?.city?.cityName || '当前城市');
const announcements = computed(() => store.experienceCityContext?.announcements || []);
function record(value: unknown): Record<string, unknown> { return value && typeof value === 'object' ? value as Record<string, unknown> : {}; }
function noticeKey(value: unknown, index: number): string { return String(record(value).id || index); }
function noticeTitle(value: unknown): string { return String(record(value).title || record(value).name || '城市公告'); }
function noticeContent(value: unknown): string { return String(record(value).content || record(value).description || '暂无详细内容'); }
onMounted(async () => {
  try { await initializeCityContext(); }
  catch (value: unknown) { error.value = value instanceof Error ? value.message : '公告加载失败'; }
  finally { loading.value = false; }
});
</script>

<style scoped lang="scss">@import '../primary-page.scss';</style>
