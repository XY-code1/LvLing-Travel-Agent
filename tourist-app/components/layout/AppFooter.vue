<template>
  <view class="app-footer">
    <PageContainer>
      <view class="app-footer__main">
        <view>
          <view class="app-footer__brand">
            <view class="app-footer__mark">旅</view>
            <text class="app-footer__name">旅灵</text>
          </view>
          <text class="app-footer__tagline">让每一次出发，都有人替你想好。</text>
        </view>

        <view class="app-footer__links" aria-label="页脚导航">
          <view v-for="item in links" :key="item.label" class="app-footer__link" role="link" @tap="go(item.url)">
            <text>{{ item.label }}</text>
          </view>
        </view>
      </view>

      <view class="app-footer__bottom">
        <text>旅灵 AI 旅行数字员工</text>
        <text>© {{ year }} Guido</text>
      </view>
    </PageContainer>
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue';

import { APP_ROUTES, navigateTo } from '../../router';
import PageContainer from './PageContainer.vue';

const year = computed(() => new Date().getFullYear());
const links = [
  { label: 'AI旅行', url: APP_ROUTES.planner },
  { label: '城市探索', url: APP_ROUTES.cities },
  { label: '景点服务', url: APP_ROUTES.spot }
];

function go(url: string): void {
  if (url === APP_ROUTES.home || url.startsWith(`${APP_ROUTES.home}?`)) {
    uni.reLaunch({ url });
    return;
  }
  navigateTo(url);
}
</script>

<style scoped lang="scss">
.app-footer {
  padding: 48px 0 28px;
  color: var(--text-secondary);
  background: var(--jade-900);
}

.app-footer__main,
.app-footer__bottom,
.app-footer__brand,
.app-footer__links {
  display: flex;
  align-items: center;
}

.app-footer__main {
  justify-content: space-between;
  gap: 28px;
}

.app-footer__brand {
  gap: 10px;
  color: var(--ivory);
}

.app-footer__mark {
  display: grid;
  place-items: center;
  width: 30px;
  height: 30px;
  color: var(--gold);
  border: 1px solid rgba(217, 183, 110, .72);
  border-radius: 50%;
  font-size: 14px;
}

.app-footer__name {
  font-family: Georgia, 'Songti SC', serif;
  font-size: 18px;
  font-weight: 800;
  letter-spacing: .08em;
}

.app-footer__tagline {
  display: block;
  margin-top: 12px;
  color: rgba(248, 246, 240, .62);
  font-size: 13px;
}

.app-footer__links {
  gap: 24px;
}

.app-footer__link {
  color: rgba(248, 246, 240, .76);
  font-size: 13px;
  cursor: pointer;
}

.app-footer__bottom {
  justify-content: space-between;
  gap: 20px;
  margin-top: 36px;
  padding-top: 18px;
  color: rgba(248, 246, 240, .46);
  border-top: 1px solid rgba(248, 246, 240, .16);
  font-size: 12px;
}

@media (max-width: 767px) {
  .app-footer {
    padding-top: 36px;
  }

  .app-footer__main,
  .app-footer__bottom {
    align-items: flex-start;
    flex-direction: column;
  }

  .app-footer__links {
    flex-wrap: wrap;
    gap: 12px 20px;
  }

  .app-footer__bottom {
    gap: 8px;
  }
}
</style>
