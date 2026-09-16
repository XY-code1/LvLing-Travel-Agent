<template>
  <view class="home-page">
    <AppHeader active="home" :current-city="cityName" :cities="cities" @city-change="changeCity" />

    <!-- ══ Hero 区 ══ -->
    <view class="home-hero" :class="{ 'home-hero--switching': switching }">
      <image :key="heroImage" class="home-hero__image" :src="heroImage" mode="aspectFill" />
      <view class="home-hero__shade" />

      <view class="home-hero__inner">
        <text class="hero__eyebrow">AI TRAVEL EMPLOYEE</text>
        <text class="hero__title">想去哪座城市？</text>
        <text class="hero__title hero__title--accent">让灵灵替你规划</text>
        <text class="hero__slogan">你的 AI 旅行数字员工</text>
        <text class="hero__desc">告诉我城市、时间、预算和同行人，从景点筛选到路线调整，我帮你安排好。</text>

        <view class="hero-task">
          <input v-model="taskText" placeholder="告诉灵灵你想怎么玩……" placeholder-class="hero-task__placeholder" confirm-type="search" @confirm="submitTask" />
          <view class="hero-task__submit" @tap="submitTask">让灵灵规划 →</view>
        </view>

        <view class="hero__actions">
          <view class="button button--hero" @tap="startChat">告诉灵灵我的计划</view>
          <view class="button--hero-secondary" @tap="scrollCities">探索热门城市</view>
        </view>
        <view class="hero__capabilities"><text>自主规划</text><text>实时调整</text><text>全程陪伴</text></view>
      </view>

      <view class="hero-guide">
        <view class="guide-bubble">你好，我是旅灵。<text>我可以帮你找景点、规划路线。</text></view>
        <DigitalHuman2D />
      </view>

      <view class="hero-meta">
        <view><text class="hero-meta__label">今日天气</text><text class="hero-meta__value">{{ store.currentCity?.weatherCode || '城市天气待接入' }}</text></view>
        <view><text class="hero-meta__label">城市公告</text><text class="hero-meta__value">{{ cityContext?.announcements.length ? `${cityContext.announcements.length} 条待查看` : '暂无新公告' }}</text></view>
        <view><text class="hero-meta__label">导览状态</text><text class="hero-meta__value">{{ cityContext?.fallback ? '正在探索' : '本地资料已同步' }}</text></view>
      </view>
    </view>

    <view class="home-body">
      <!-- 能力网格 -->
      <view class="ability-section">
        <view class="section-heading">
          <view>
            <text class="section-kicker">02 · HOW IT WORKS</text>
            <text class="section-title">灵灵能帮你做什么</text>
          </view>
        </view>
        <view class="ability-grid">
          <view class="ability-card ability-card--plan" @tap="startChat">
            <view class="ability-icon"><text>✦</text></view>
            <text class="ability-card__name">AI旅行规划</text>
            <text class="ability-card__desc">一句话生成完整行程</text>
          </view>
          <view class="ability-card ability-card--route" @tap="goRoute">
            <view class="ability-icon"><text>⌁</text></view>
            <text class="ability-card__name">路线推荐</text>
            <text class="ability-card__desc">结合天气实时调整</text>
          </view>
          <view class="ability-card ability-card--vision" @tap="goVision">
            <view class="ability-icon"><text>◉</text></view>
            <text class="ability-card__name">拍照识别</text>
            <text class="ability-card__desc">识别景点并自动讲解</text>
          </view>
          <view class="ability-card ability-card--service" @tap="startChat">
            <view class="ability-icon"><text>＋</text></view>
            <text class="ability-card__name">附近服务</text>
            <text class="ability-card__desc">餐饮、停车、卫生间</text>
          </view>
        </view>
      </view>

      <!-- 热门城市 -->
      <view id="hot-cities" class="editorial-section cities-section">
        <view class="section-heading">
          <view>
            <text class="section-kicker">01 · EXPLORE</text>
            <text class="section-title">探索下一座城市</text>
            <text class="section-subtitle">每座城市，都有不同的旅行节奏。</text>
          </view>
          <text class="section-link" @tap="scrollCities">查看全部 ›</text>
        </view>
        <view class="city-scroll">
          <view
            v-for="city in cities.slice(0, 6)"
            :key="city.id"
            class="city-card"
            @tap="changeCity({ detail: { value: cities.findIndex((item) => item.id === city.id) } })"
          >
            <image :src="city.coverImage || '/static/images/home-hero-scenic.webp'" mode="aspectFill" />
            <view class="city-card__shade" />
            <view class="city-card__copy">
              <text>{{ city.cityName }}</text>
              <text>{{ city.slogan || city.description || '山水 · 人文 · 慢游' }}</text>
            </view>
            <text v-if="city.id === store.currentCityId" class="city-card__badge">当前城市</text>
          </view>
        </view>
      </view>

      <!-- 示例行程 -->
      <view class="itinerary-section">
        <view class="section-heading">
          <view>
            <text class="section-kicker">03 · SAMPLE JOURNEY</text>
            <text class="section-title">一份刚刚好的行程</text>
          </view>
          <text class="section-subtitle">不赶路，也不浪费好风景。</text>
        </view>
        <view class="itinerary-card">
          <view class="itinerary-day">
            <view class="itinerary-day__head">
              <text class="itinerary-day__tag">DAY 01</text>
              <text class="itinerary-city">山水与人文之间</text>
            </view>
            <view class="timeline">
              <view class="timeline__item"><text class="timeline__time">09:00</text><text class="timeline__text">灵隐寺</text></view>
              <view class="timeline__item"><text class="timeline__time">12:30</text><text class="timeline__text">西湖边午餐</text></view>
              <view class="timeline__item"><text class="timeline__time">15:00</text><text class="timeline__text">湖畔慢行</text></view>
            </view>
          </view>
          <view class="itinerary-map">
            <text class="itinerary-map__label">旅灵为你整理</text>
            <text class="map-route">灵隐寺　—　西湖　—　河坊街</text>
            <view class="itinerary-map__foot">
              <text class="itinerary-map__price">¥1268</text>
              <text class="itinerary-map__meta">步行 4.2 km · 预计 8 小时</text>
            </view>
          </view>
        </view>
      </view>

      <!-- 自适应旅程 -->
      <view class="replan-section">
        <view class="replan-copy-block">
          <text class="section-kicker">04 · REPLANNING</text>
          <text class="editorial-title">计划赶不上变化？<text>灵灵会重新规划。</text></text>
          <text class="replan-copy">当一场雨改变下午的安排，灵灵会保留旅程的心情，只替你换一条更舒服的路。</text>
        </view>
        <view class="replan-flow">
          <view class="replan-node"><text class="replan-node__label">原行程</text><text class="replan-node__value">西湖漫步</text></view>
          <text class="replan-arrow">→</text>
          <view class="replan-node weather-change"><text class="replan-node__label">天气变化</text><text class="replan-node__value">14:00 · 暴雨</text></view>
          <text class="replan-arrow">→</text>
          <view class="replan-node replan-node--new"><text class="replan-node__label">新行程</text><text class="replan-node__value">茶馆与室内展览</text></view>
        </view>
      </view>

    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { onLoad } from '@dcloudio/uni-app';

import { getCurrentAvatar } from '../../api/avatar';
import { createSession } from '../../api/chat';
import { getCityContext, listCities } from '../../api/scenic';
import DigitalHuman2D from '../../components/DigitalHuman2D.vue';
import AppHeader from '../../components/layout/AppHeader.vue';
import { navigateTo } from '../../router';
import { useTouristStore } from '../../stores';
import type { AvatarConfigVO, CityVO } from '../../types';
import { isLoggedIn } from '../../utils/auth';

const store    = useTouristStore();
const cities   = ref<CityVO[]>([]);
const switching = ref(false);
const taskText  = ref('');
const avatar    = ref<AvatarConfigVO | null>(null);
const cityName  = computed(() => store.currentCity?.cityName || '探索城市');
const heroImage = computed(() => store.currentCity?.coverImage || '/static/images/home-hero-scenic.webp');
const cityContext = computed(() => store.cityContext);
const requestedSection = ref('');

async function loadHome(): Promise<void> {
  try {
    // 游客首页不应请求受保护的头像接口；DigitalHuman2D 自带本地 PNG 降级资源。
    // 否则 request.ts 的 401 处理会把正常游客入口重定向到登录页。
    if (isLoggedIn() && !avatar.value) { try { avatar.value = await getCurrentAvatar(); } catch { /* 数字人未配置时保留降级界面 */ } }
    cities.value = await listCities();
    const selected = cities.value.find((item) => item.id === store.currentCityId) || cities.value[0];
    if (selected) {
      const context = await getCityContext(selected.id); store.setCityContext(context);
    }
  } catch (error: unknown) {
    uni.showToast({ title: error instanceof Error ? error.message : '首页加载失败', icon: 'none' });
  }
}
async function changeCity(event: { detail: { value: number } }): Promise<void> {
  const city = cities.value[event.detail.value];
  if (!city || city.id === store.currentCityId) return;
  switching.value = true;
  try {
    store.setCityContext(await getCityContext(city.id));
  } finally {
    setTimeout(() => { switching.value = false; }, 560);
  }
}

async function ensureSession(): Promise<string> {
  if (!store.selectedAvatar) {
    store.setSelectedAvatar(await getCurrentAvatar());
  }
  const avatarId = store.selectedAvatar?.id;
  if (store.currentSessionNo && (!avatarId || store.currentSessionAvatarId === avatarId)) return store.currentSessionNo;
  const session = await createSession(store.currentScenicId, avatarId);
  const selected = session.selectedAvatar || session.defaultAvatar || store.selectedAvatar;
  store.setSession(session.sessionNo, session.scenicId, selected?.id || null);
  store.setSelectedAvatar(selected || null);
  return session.sessionNo;
}

async function startChat(): Promise<void> {
  try {
    await ensureSession();
    navigateTo('/pages/chat/index');
  } catch (error: unknown) {
    uni.showToast({ title: error instanceof Error ? error.message : '创建会话失败', icon: 'none' });
  }
}

function submitTask(): void { const text = taskText.value.trim(); if (text) navigateTo(`/pages/chat/index?question=${encodeURIComponent(text)}`); else startChat(); }
function scrollCities(): void { uni.pageScrollTo({ selector: '#hot-cities', duration: 260 }); }

function goRoute(): void              { navigateTo('/pages/route/index'); }
function goVision(): void             { navigateTo('/pages/vision/index'); }

onLoad((query) => {
  requestedSection.value = typeof query.section === 'string' ? query.section : '';
});

onMounted(() => {
  void loadHome().finally(() => {
    if (requestedSection.value) {
      setTimeout(() => uni.pageScrollTo({ selector: '#hot-cities', duration: 260 }), 120);
    }
  });
});
</script>

<style scoped lang="scss">
/* ═══════════════════════════════════════════════════════
   首页 — 移动优先布局
   断点：默认(移动) → ≥901px(桌面)
   ═══════════════════════════════════════════════════════ */

.home-page {
  min-height: 100vh;
  padding: 16px 0 48rpx;
  background:
    radial-gradient(circle at 12% 0%, rgba(217, 119, 6, 0.08), transparent 34%),
    linear-gradient(180deg, #f8fff9 0%, var(--app-bg) 42%, #eef8f1 100%);
}

/* ── 顶部导航 ── */
.home-page :deep(.app-header) {
  position: absolute;
  top: 12px; left: 12px; right: 12px;
  z-index: 20;
}

/* ═══ Hero ═══ */
.home-hero {
  position: relative;
  min-height: 1060rpx;
  display: flex;
  flex-direction: column;
  padding: 0 28rpx 148rpx;
  overflow: hidden;
  border-radius: 0 0 28rpx 28rpx;
  box-shadow: var(--shadow-float);
  isolation: isolate;
}

.home-hero__image,
.home-hero__shade {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}

.home-hero__image { z-index: 0; }

.home-hero__shade {
  z-index: 1;
  background:
    linear-gradient(180deg, rgba(9, 31, 24, 0.10) 0%, rgba(9, 31, 24, 0.30) 46%, rgba(6, 45, 35, 0.86) 100%),
    linear-gradient(90deg, rgba(9, 31, 24, 0.62) 0%, rgba(9, 31, 24, 0.16) 78%);
}

.home-hero__inner {
  position: relative;
  z-index: 2;
  padding-top: 96rpx;
}

.hero__eyebrow {
  display: block;
  font-size: 22rpx;
  font-weight: 600;
  letter-spacing: 2px;
  color: rgba(255, 255, 255, 0.72);
  margin-bottom: 16rpx;
}

.hero__title {
  display: block;
  font-size: 48rpx;
  font-weight: 700;
  line-height: 1.14;
  letter-spacing: -0.02em;
  color: #fff;
}

.hero__title--accent { color: #f0d59a; margin-top: 4rpx; }

.hero__slogan {
  display: block;
  margin-top: 14rpx;
  font-size: 28rpx;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.92);
}

.hero__desc {
  display: block;
  margin-top: 12rpx;
  font-size: 24rpx;
  line-height: 1.65;
  color: rgba(255, 255, 255, 0.78);
  max-width: 560px;
}

/* 输入框 */
.hero-task {
  display: flex;
  align-items: center;
  max-width: 640px;
  margin-top: 26rpx;
  padding: 8px 8px 8px 18px;
  border: 1px solid rgba(255, 255, 255, 0.55);
  border-radius: 22px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.72), rgba(255, 255, 255, 0.38));
  backdrop-filter: blur(18px) saturate(140%);
  box-shadow: var(--shadow-premium);
  box-sizing: border-box;
}

.hero-task input {
  flex: 1;
  min-width: 0;
  height: 44px;
  color: var(--text-primary);
  font-size: 14px;
}

.hero-task__placeholder { color: rgba(21, 37, 31, 0.58); }

.hero-task__submit {
  flex-shrink: 0;
  padding: 12px 16px;
  border-radius: 14px;
  background: var(--jade);
  color: #fff;
  font-size: 14px;
  font-weight: 700;
}

/* 按钮 / 能力标签 */
.hero__actions {
  display: flex;
  align-items: center;
  gap: 18rpx;
  margin-top: 26rpx;
}

.button--hero {
  min-height: 72rpx;
  padding: 0 28rpx;
  background: rgba(255, 255, 255, 0.94);
  color: var(--primary-deeper);
  box-shadow: none;
  font-size: 26rpx;
}

.button--hero-secondary {
  min-height: 68rpx;
  display: flex;
  align-items: center;
  padding: 0 24rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.55);
  border-radius: var(--radius);
  color: #fff;
  font-size: 25rpx;
}

.hero__capabilities {
  display: none;
}

/* 数字人 */
.hero-guide {
  position: relative;
  z-index: 2;
  width: 100%;
  margin-top: 30rpx;
  align-self: stretch;
}

.guide-bubble {
  max-width: 70%;
  margin-bottom: 6rpx;
  padding: 16rpx 20rpx;
  border-radius: 22rpx 22rpx 22rpx 6rpx;
  background: rgba(255, 255, 255, 0.92);
  color: var(--text-main);
  font-size: 24rpx;
  line-height: 1.5;
  box-shadow: 0 12rpx 30rpx rgba(0, 0, 0, 0.12);
}

.guide-bubble text { display: block; color: var(--text-muted); font-size: 21rpx; }

.hero-guide :deep(.digital-human-2d) {
  min-height: 340rpx;
  background: transparent;
  box-shadow: none;
}

/* 底部信息栏 */
.hero-meta {
  position: absolute;
  z-index: 3;
  right: 28rpx; bottom: 22rpx; left: 28rpx;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12rpx;
  padding: 18rpx 20rpx;
  border-top: 1rpx solid rgba(255, 255, 255, 0.3);
  border-radius: 16rpx;
  color: #fff;
  background: rgba(3, 44, 34, 0.28);
  backdrop-filter: blur(12px);
}

.hero-meta__label,
.hero-meta__value { display: block; }

.hero-meta__label { color: rgba(255, 255, 255, 0.62); font-size: 20rpx; }

.hero-meta__value {
  margin-top: 6rpx;
  font-size: 24rpx;
  font-weight: 700;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 切换城市动画 */
.home-hero--switching .home-hero__inner { animation: hero-context-in 0.56s ease both; }
.home-hero--switching .home-hero__image { animation: hero-image-in 0.72s ease both; }
@keyframes hero-context-in { 0% { opacity: 0.45; transform: translateY(12rpx); } 100% { opacity: 1; transform: translateY(0); } }
@keyframes hero-image-in { 0% { opacity: 0; transform: scale(1.025); } 100% { opacity: 1; transform: scale(1); } }

/* ═══ 页面主体 ═══ */
.home-body {
  position: relative;
  z-index: 3;
  margin-top: -20rpx;
  padding: 0 28rpx;
}

.ability-section { margin-top: 56rpx; }

.section-heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24rpx;
  margin-bottom: 24rpx;
}

.section-kicker {
  display: block;
  color: var(--jade);
  font-size: 22rpx;
  font-weight: 600;
  letter-spacing: 2px;
}

.section-title {
  display: block;
  margin-top: 8rpx;
  font-size: 36rpx;
  font-weight: 700;
  line-height: 1.2;
  color: var(--text-primary);
}

.section-subtitle {
  display: block;
  margin-top: 8rpx;
  font-size: 24rpx;
  color: var(--text-secondary);
}

.section-link {
  flex-shrink: 0;
  color: var(--jade);
  font-size: 24rpx;
}

/* ── 能力网格 ── */
.ability-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20rpx;
}

.ability-card {
  padding: 28rpx 24rpx;
  border: 1px solid rgba(40, 95, 82, 0.12);
  border-radius: 20rpx;
  background: #fff;
  box-shadow: 0 12rpx 34rpx rgba(24, 40, 34, 0.06);
  transition: transform 0.15s ease, box-shadow 0.15s ease;

  &:active {
    transform: translateY(-2rpx) scale(0.99);
    box-shadow: 0 18rpx 40rpx rgba(24, 40, 34, 0.1);
  }
}

.ability-icon {
  display: grid;
  place-items: center;
  width: 72rpx;
  height: 72rpx;
  margin-bottom: 20rpx;
  border-radius: 20rpx;
  color: #fff;
  font-size: 34rpx;
}

.ability-card--plan .ability-icon    { background: linear-gradient(135deg, #15803d, #166534); }
.ability-card--route .ability-icon   { background: linear-gradient(135deg, #d97706, #b45309); }
.ability-card--vision .ability-icon  { background: linear-gradient(135deg, #0e7490, #155e75); }
.ability-card--service .ability-icon { background: linear-gradient(135deg, #7c3aed, #6d28d9); }

.ability-card__name,
.ability-card__desc { display: block; }

.ability-card__name {
  font-size: 30rpx;
  font-weight: 700;
  color: var(--text-primary);
}

.ability-card__desc {
  margin-top: 8rpx;
  font-size: 23rpx;
  color: var(--text-muted);
  line-height: 1.5;
}

/* ── 区块通用 ── */
.editorial-section,
.itinerary-section,
.replan-section {
  margin-top: 72rpx;
}

/* ── 城市横滑 ── */
.city-scroll {
  display: flex;
  gap: 20rpx;
  overflow-x: auto;
  padding-bottom: 12rpx;
  scroll-snap-type: x mandatory;
  -webkit-overflow-scrolling: touch;
}

.city-card {
  position: relative;
  flex: 0 0 72%;
  aspect-ratio: 4 / 3;
  overflow: hidden;
  border-radius: 28rpx;
  scroll-snap-align: start;
}

.city-card image {
  width: 100%;
  height: 100%;
  display: block;
  transition: transform 0.5s ease;
}

.city-card:active image { transform: scale(1.04); }

.city-card__shade {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, transparent 35%, rgba(9, 31, 24, 0.78));
}

.city-card__copy {
  position: absolute;
  right: 22px; bottom: 18px; left: 22px;
  color: #fff;
}

.city-card__copy text { display: block; }
.city-card__copy text:first-child { font-size: 28px; font-weight: 700; }
.city-card__copy text:last-child {
  margin-top: 6px;
  color: rgba(255, 255, 255, 0.78);
  font-size: 13px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.city-card__badge {
  position: absolute;
  top: 14px; left: 14px;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(6, 78, 59, 0.78);
  color: #fff;
  font-size: 11px;
  backdrop-filter: blur(8px);
}

/* ── 热门景点横滑 ── */
.spot-scroll {
  width: 100%;
  white-space: nowrap;
}

.spot-scroll__inner {
  display: inline-flex;
  gap: 20rpx;
  padding: 4rpx 4rpx 16rpx;
}

.spot-chip {
  position: relative;
  flex: 0 0 250rpx;
  height: 330rpx;
  border-radius: 24rpx;
  overflow: hidden;
  box-shadow: var(--shadow-soft);
  transition: transform 0.15s ease;

  &:active { transform: scale(0.98); }
}

.spot-chip image {
  width: 100%;
  height: 100%;
  display: block;
}

.spot-chip__shade {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, transparent 40%, rgba(9, 31, 24, 0.82));
}

.spot-chip__copy {
  position: absolute;
  right: 18rpx; bottom: 16rpx; left: 18rpx;
}

.spot-chip__name,
.spot-chip__hint { display: block; color: #fff; }

.spot-chip__name {
  font-size: 28rpx;
  font-weight: 700;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.spot-chip__hint {
  margin-top: 6rpx;
  font-size: 21rpx;
  color: rgba(255, 255, 255, 0.78);
}

/* ── 示例行程 ── */
.itinerary-card {
  display: grid;
  grid-template-columns: 1fr;
  overflow: hidden;
  border-radius: 28rpx;
  background: #fff;
  border: 1px solid rgba(40, 95, 82, 0.12);
  box-shadow: var(--shadow-soft);
}

.itinerary-day {
  padding: 32rpx;
}

.itinerary-day__head {
  display: flex;
  align-items: center;
  gap: 14rpx;
  margin-bottom: 26rpx;
}

.itinerary-day__tag {
  padding: 8rpx 18rpx;
  border-radius: 999rpx;
  background: var(--primary-surface);
  color: var(--primary-dark);
  font-size: 22rpx;
  font-weight: 700;
  letter-spacing: 1px;
}

.itinerary-city {
  font-size: 30rpx;
  font-weight: 700;
  color: var(--text-primary);
}

.timeline {
  position: relative;
  padding-left: 30rpx;
}

.timeline::before {
  content: '';
  position: absolute;
  left: 6rpx; top: 8rpx; bottom: 8rpx;
  width: 2rpx;
  background: linear-gradient(180deg, var(--primary), rgba(21, 128, 61, 0.15));
}

.timeline__item {
  position: relative;
  display: flex;
  align-items: baseline;
  gap: 18rpx;
  padding: 10rpx 0;
}

.timeline__item::before {
  content: '';
  position: absolute;
  left: -28rpx;
  top: 50%;
  transform: translateY(-50%);
  width: 14rpx;
  height: 14rpx;
  border-radius: 50%;
  background: var(--primary);
  border: 4rpx solid var(--primary-surface);
  box-sizing: content-box;
}

.timeline__time {
  flex-shrink: 0;
  width: 88rpx;
  color: var(--primary-dark);
  font-size: 24rpx;
  font-weight: 700;
}

.timeline__text {
  color: var(--text-secondary);
  font-size: 27rpx;
}

.itinerary-map {
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  min-height: 220rpx;
  padding: 32rpx;
  background:
    radial-gradient(circle at 85% 15%, rgba(217, 119, 6, 0.16), transparent 46%),
    linear-gradient(135deg, var(--primary-deeper), #0b5d4a);
  color: #fff;
}

.itinerary-map__label {
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.7);
  letter-spacing: 1px;
}

.map-route {
  display: block;
  margin-top: 12rpx;
  font-size: 30rpx;
  font-weight: 700;
  line-height: 1.5;
  color: #f0d59a;
}

.itinerary-map__foot {
  display: flex;
  align-items: baseline;
  gap: 16rpx;
  margin-top: 18rpx;
}

.itinerary-map__price {
  font-size: 34rpx;
  font-weight: 800;
}

.itinerary-map__meta {
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.72);
}

/* ── 自适应旅程 ── */
.replan-section {
  display: grid;
  grid-template-columns: 1fr;
  gap: 34rpx;
  padding: 40rpx 32rpx;
  border-radius: 28rpx;
  background:
    radial-gradient(circle at 90% 0%, rgba(217, 119, 6, 0.12), transparent 48%),
    linear-gradient(160deg, #f0faf4, #e7f5ec);
  border: 1px solid rgba(40, 95, 82, 0.12);
}

.editorial-title {
  display: block;
  margin-top: 16rpx;
  font-size: 40rpx;
  font-weight: 700;
  line-height: 1.2;
  color: var(--text-primary);
}

.editorial-title text { display: block; color: var(--jade); }

.replan-copy {
  display: block;
  margin-top: 16rpx;
  font-size: 25rpx;
  line-height: 1.7;
  color: var(--text-secondary);
}

.replan-flow {
  display: flex;
  align-items: center;
  gap: 12rpx;
  flex-wrap: wrap;
}

.replan-node {
  flex: 1 1 120rpx;
  min-width: 120rpx;
  padding: 20rpx 16rpx;
  border-radius: 18rpx;
  background: rgba(255, 255, 255, 0.92);
  border: 1rpx solid rgba(40, 95, 82, 0.14);
  box-shadow: var(--shadow-soft);
}

.replan-node__label,
.replan-node__value { display: block; }

.replan-node__label {
  font-size: 21rpx;
  color: var(--text-muted);
}

.replan-node__value {
  margin-top: 8rpx;
  font-size: 25rpx;
  font-weight: 700;
  color: var(--text-primary);
}

.weather-change {
  border-color: rgba(217, 119, 6, 0.35);
  background: #fffaf0;
}

.weather-change .replan-node__value { color: #b45309; }

.replan-node--new {
  background: var(--primary-surface);
  border-color: rgba(21, 128, 61, 0.3);
}

.replan-node--new .replan-node__value { color: var(--primary-dark); }

.replan-arrow {
  flex-shrink: 0;
  color: var(--jade);
  font-size: 30rpx;
  font-weight: 700;
}

/* ── 功能卡片 ── */
.feature-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20rpx;
  margin-top: 72rpx;
}

.feature-card {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  justify-content: flex-end;
  padding: 0;
  min-height: 210rpx;
  border-radius: var(--radius);
  background: var(--surface);
  border: 1rpx solid rgba(255, 255, 255, 0.76);
  box-shadow: var(--shadow-soft);
  position: relative;
  overflow: hidden;
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;

  &:active {
    transform: scale(0.97);
    box-shadow: var(--shadow-soft);
    border-color: var(--primary);
  }
}

.feature-card__image,
.feature-card__shade {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}

.feature-card__image { z-index: 0; }

.feature-card__shade {
  z-index: 1;
  background:
    linear-gradient(180deg, rgba(6, 78, 59, 0.02) 0%, rgba(6, 78, 59, 0.18) 42%, rgba(6, 78, 59, 0.82) 100%),
    linear-gradient(90deg, rgba(15, 23, 42, 0.38) 0%, rgba(15, 23, 42, 0.04) 68%);
}

.feature-card__content {
  position: relative;
  z-index: 2;
  box-sizing: border-box;
  width: 100%;
  padding: 22rpx;
}

.feature-card__label {
  display: block;
  font-size: 30rpx;
  font-weight: 800;
  color: #fff;
  line-height: 1.3;
  text-shadow: 0 4rpx 14rpx rgba(6, 78, 59, 0.36);
}

.feature-card__desc {
  display: block;
  margin-top: 6rpx;
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.86);
  line-height: 1.45;
  text-shadow: 0 3rpx 10rpx rgba(6, 78, 59, 0.32);
}

/* ═══ 桌面端增强 (≥901px) ═══ */
@media (min-width: 901px) {
  .home-page { max-width: 1440px; margin: 0 auto; padding: 16px 0 48px; }

  .home-page :deep(.app-header) {
    top: 32px; left: 32px; right: 32px;
    max-width: 1376px;
    margin: 0 auto;
  }

  .home-hero {
    min-height: 640px;
    display: grid;
    grid-template-columns: minmax(0, 1.1fr) minmax(380px, 0.9fr);
    gap: 40px;
    align-items: center;
    padding: 0 48px 96px;
    border-radius: 0 0 36px 36px;
    max-width: 1440px;
    margin: 0 auto;
  }

  .home-hero__inner {
    grid-column: 1;
    padding-top: 110px;
    max-width: 640px;
  }

  .hero__title { font-size: clamp(46px, 4.6vw, 68px); font-weight: 600; line-height: 1.06; }
  .hero__desc { font-size: 17px; }
  .hero-task { margin-top: 28px; }
  .hero-questions { margin-top: 20px; }

  .hero__actions { gap: 14px; margin-top: 26px; }
  .hero__capabilities {
    display: flex;
    gap: 26px;
    margin-top: 22px;
    color: rgba(255, 255, 255, 0.76);
    font-size: 14px;
  }
  .hero__capabilities text::before { content: '·'; margin-right: 8px; color: #f0d59a; }

  .hero-guide {
    grid-column: 2;
    align-self: end;
    margin-top: 0;
  }

  .hero-guide :deep(.digital-human-2d) { min-height: 480px; }

  .hero-meta {
    right: 48px; bottom: 24px; left: 48px;
    padding: 16px 24px;
  }

  .home-body {
    max-width: 1376px;
    margin: -24px auto 0;
    padding: 0 48px;
  }

  .ability-grid { grid-template-columns: repeat(4, 1fr); }
  .ability-card { padding: 26px 24px; }

  .city-scroll {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 20px;
    overflow: visible;
    padding-bottom: 0;
  }

  .city-card { flex: none; width: auto; }

  .spot-scroll { overflow: visible; white-space: normal; }
  .spot-scroll__inner {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 20px;
    padding: 0;
  }

  .spot-chip { flex: none; width: auto; height: 300px; }

  .itinerary-card {
    grid-template-columns: 1.1fr 0.9fr;
    border-radius: 32px;
  }

  .itinerary-day { padding: 40px 44px; }
  .itinerary-map { min-height: 260px; padding: 40px 44px; }

  .replan-section {
    grid-template-columns: 1fr 1fr;
    align-items: center;
    padding: 48px 52px;
  }

  .editorial-title { font-size: 40px; }

  .feature-grid { gap: 24px; }
  .feature-card { min-height: 240px; border-radius: var(--radius-lg); }
}

@media (prefers-reduced-motion: reduce) {
  .home-hero--switching .home-hero__inner,
  .home-hero--switching .home-hero__image { animation: none; }
}
/* 回归修复：Header 独立覆盖在 Hero 顶部，正文和人物使用同一行网格。 */
.home-page {
  padding: 0 0 48px;
  overflow-x: hidden;
}

.home-page :deep(.app-header) {
  position: absolute;
  top: 20px;
  right: auto;
  left: 50%;
  width: calc(100% - 48px);
  max-width: 1376px;
  transform: translateX(-50%);
  box-sizing: border-box;
  background: rgba(245, 243, 238, .64);
  border-color: rgba(255, 255, 255, .48);
  backdrop-filter: blur(18px) saturate(140%);
}

.home-hero {
  box-sizing: border-box;
  min-height: 760px;
  height: 85vh;
  max-height: 900px;
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(360px, .9fr);
  gap: 48px;
  align-items: center;
  padding: 128px 48px 96px;
}

.home-hero__inner {
  align-self: center;
  min-width: 0;
  padding-top: 0;
  /* 为底部状态栏留出空间，避免行动按钮和能力标签被覆盖。 */
  transform: translateY(-40px);
}

.hero-guide {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-end;
  align-self: stretch;
  width: 100%;
  min-width: 0;
  height: 100%;
  margin-top: 0;
  padding: 64px 0 0;
  box-sizing: border-box;
  overflow: visible;
}

.guide-bubble {
  width: min(100%, 480px);
  max-width: 100%;
  margin: 0 0 12px;
  box-sizing: border-box;
}

.hero-guide :deep(.digital-human-2d) {
  width: 100%;
  height: min(600px, 70vh);
  max-height: 600px;
  min-height: 0;
  transform: none;
  transform-origin: center bottom;
}

.hero-meta {
  right: 48px;
  bottom: 24px;
  left: 48px;
}

.home-body {
  max-width: 1376px;
  margin: 0 auto;
  padding: 0 48px;
  box-sizing: border-box;
}

.ability-section { margin-top: 72px; }

.ability-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0;
  overflow: hidden;
  border: 1px solid rgba(40, 95, 82, .12);
  border-radius: 22px;
  background: rgba(255, 255, 255, .74);
}

.ability-card {
  min-height: 152px;
  padding: 24px;
  border: 0;
  border-right: 1px solid rgba(40, 95, 82, .10);
  border-radius: 0;
  box-shadow: none;
  background: transparent;
}

.ability-card:last-child { border-right: 0; }

@media (max-width: 1100px) and (min-width: 901px) {
  .home-hero {
    grid-template-columns: minmax(0, 1.05fr) minmax(320px, .95fr);
    gap: 28px;
    padding-right: 28px;
    padding-left: 28px;
  }

  .hero-guide :deep(.digital-human-2d) { height: 520px; }
  .hero-meta { right: 28px; left: 28px; }
  .home-body { padding-right: 28px; padding-left: 28px; }
}

@media (max-width: 900px) {
  .home-page :deep(.app-header) {
    top: 14px;
    width: calc(100% - 32px);
  }

  .home-hero {
    display: flex;
    height: auto;
    min-height: 900px;
    max-height: none;
    padding: 110px 24px 92px;
  }

  .home-hero__inner {
    padding-top: 72px;
    transform: none;
  }

  .hero-guide {
    height: 430px;
    margin-top: 28px;
    padding-top: 0;
  }

  .hero-guide :deep(.digital-human-2d) {
    height: 430px;
    max-height: 430px;
  }

  .hero-meta { right: 24px; bottom: 20px; left: 24px; }
  .home-body { padding-right: 24px; padding-left: 24px; }
  .ability-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .ability-card:nth-child(2) { border-right: 0; }
  .ability-card:nth-child(-n + 2) { border-bottom: 1px solid rgba(40, 95, 82, .10); }
}

@media (max-width: 640px) {
  .home-page :deep(.app-header) { width: calc(100% - 24px); }

  .home-hero {
    min-height: 920px;
    padding: 92px 16px 88px;
  }

  .home-hero__inner { padding-top: 56px; }
  .hero__title { font-size: clamp(38px, 10vw, 52px); }
  .hero__desc { max-width: none; }
  .hero-task { width: 100%; }
  .hero-guide { height: 380px; margin-top: 22px; }
  .hero-guide :deep(.digital-human-2d) { height: 380px; max-height: 380px; }
  .hero-meta { right: 16px; bottom: 16px; left: 16px; }
  .home-body { padding-right: 16px; padding-left: 16px; }
  .ability-card { min-height: 136px; padding: 20px 16px; }
}
</style>
