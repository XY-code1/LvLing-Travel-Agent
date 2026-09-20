<template>
  <view class="entry-page">
    <AppHeader />
    <PageContainer>
      <view class="entry-hero">
        <text class="eyebrow">AI TRAVEL</text>
        <text class="title">告诉旅灵，你想去哪里？</text>
        <text class="subtitle">当前城市：{{ cityName }}。输入一句话，旅灵会进入路线规划工作台整理需求。</text>
        <view class="prompt-box">
          <textarea v-model="question" class="prompt-input" placeholder="例如：玩两天，预算1500，不想太赶" />
          <button class="primary-button" @tap="openPlanner">开始规划</button>
        </view>
      </view>
    </PageContainer>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import AppHeader from '../../components/layout/AppHeader.vue';
import PageContainer from '../../components/layout/PageContainer.vue';
import { initializeCityContext } from '../../composables/useCityContext';
import { APP_ROUTES, navigateTo } from '../../router';
import { useTouristStore } from '../../stores';

const store = useTouristStore();
const question = ref('');
const cityName = computed(() => store.travelTaskContext?.destinationCity?.cityName
  || store.currentCity?.cityName || store.featuredCityContext?.city?.cityName || '待选择');
void initializeCityContext().catch(() => undefined);

function openPlanner(): void {
  const text = question.value.trim();
  if (text) store.startTravelTask(text);
  navigateTo(text ? `${APP_ROUTES.aiGuide}?question=${encodeURIComponent(text)}` : APP_ROUTES.aiGuide);
}
</script>

<style scoped lang="scss">
@import '../primary-page.scss';
.entry-hero { max-width: 820px; padding: 112px 0 96px; }
.prompt-box { display: grid; gap: 16px; margin-top: 32px; padding: 20px; background: rgba(255,255,255,.72); border-radius: 22px; box-shadow: var(--shadow-card); }
.prompt-input { width: 100%; min-height: 120px; box-sizing: border-box; color: var(--text-main); }
.primary-button { justify-self: end; margin: 0; color: var(--ivory); background: var(--jade-700); border-radius: 12px; }
</style>
