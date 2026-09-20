<template>
  <view class="inspiration-page">
    <AppHeader />

    <PageContainer>
      <view class="inspiration-hero">
        <SectionHeader
          eyebrow="TRAVEL INSPIRATION"
          title="先找到想要的旅行感觉"
          subtitle="从一座城市、一段风景开始，再让灵灵把灵感整理成真正可执行的行程。"
        />
      </view>

      <view class="inspiration-grid">
        <view v-for="item in inspirations" :key="item.title" class="inspiration-card" @tap="openPlanner(item.title)">
          <image class="inspiration-card__image" :src="item.image" mode="aspectFill" />
          <view class="inspiration-card__shade" />
          <view class="inspiration-card__copy">
            <text class="inspiration-card__title">{{ item.title }}</text>
            <text class="inspiration-card__desc">{{ item.description }}</text>
            <text class="inspiration-card__action">让灵灵规划 <text>→</text></text>
          </view>
        </view>
      </view>
    </PageContainer>

    <AppFooter />
  </view>
</template>

<script setup lang="ts">
import AppFooter from '../../components/layout/AppFooter.vue';
import AppHeader from '../../components/layout/AppHeader.vue';
import PageContainer from '../../components/layout/PageContainer.vue';
import SectionHeader from '../../components/layout/SectionHeader.vue';
import { navigateTo } from '../../router';

const inspirations = [
  {
    title: '山水慢游',
    description: '留一点时间给风景，也留一点时间给自己。',
    image: '/static/images/home-hero-scenic.webp'
  },
  {
    title: '人文漫行',
    description: '走进城市故事，把一段历史走成自己的记忆。',
    image: '/static/images/spot-lingshan-palace.webp'
  },
  {
    title: '一日松弛',
    description: '不赶行程，用舒服的节奏发现身边的好去处。',
    image: '/static/images/spot-wuyin-tancheng.webp'
  }
];

function openPlanner(inspiration: string): void {
  navigateTo(`/pages/chat/index?question=${encodeURIComponent(`我想要一段${inspiration}的旅行`)}`);
}
</script>

<style scoped lang="scss">
.inspiration-page {
  min-height: 100vh;
  padding: 16px 0 0;
  color: var(--text-main);
  background: var(--bg-main);
}

.inspiration-hero {
  padding: 88px 0 42px;
}

.inspiration-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 20px;
  padding-bottom: 88px;
}

.inspiration-card {
  position: relative;
  min-height: 360px;
  overflow: hidden;
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
  cursor: pointer;
}

.inspiration-card__image,
.inspiration-card__shade {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}

.inspiration-card__image {
  transition: transform .35s ease;
}

.inspiration-card:active .inspiration-card__image {
  transform: scale(1.03);
}

.inspiration-card__shade {
  background: linear-gradient(180deg, rgba(23, 63, 53, 0) 28%, rgba(23, 63, 53, .86) 100%);
}

.inspiration-card__copy {
  position: absolute;
  right: 24px;
  bottom: 22px;
  left: 24px;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}

.inspiration-card__title {
  color: var(--ivory);
  font-size: 24px;
  font-weight: 800;
}

.inspiration-card__desc {
  margin-top: 8px;
  color: rgba(248, 246, 240, .78);
  font-size: 13px;
  line-height: 1.6;
}

.inspiration-card__action {
  margin-top: 16px;
  padding: 8px 12px;
  color: var(--jade-900);
  background: rgba(248, 246, 240, .9);
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}

@media (max-width: 767px) {
  .inspiration-hero {
    padding: 56px 0 32px;
  }

  .inspiration-grid {
    grid-template-columns: 1fr;
    padding-bottom: 56px;
  }

  .inspiration-card {
    min-height: 280px;
  }
}
</style>
