<template>
  <view class="app-header">
    <view class="brand" role="button" aria-label="返回旅灵首页" @tap="goHome">
      <view class="brand__mark">旅</view>
      <text class="brand__name">旅灵</text>
    </view>

    <view class="nav" aria-label="主导航">
      <view
        v-for="item in items"
        :key="item.key"
        class="nav__item"
        :class="{ 'nav__item--active': active === item.key }"
        role="link"
        @tap="go(item.url)"
      >
        <text>{{ item.label }}</text>
      </view>
    </view>

    <view class="header-actions">
      <view v-if="cityOptions.length" class="city-picker" @tap="cityMenuOpen = !cityMenuOpen">
        <text class="city-picker__label">{{ currentCity || '探索城市' }}</text>
        <text class="city-picker__chevron">⌄</text>
        <view v-if="cityMenuOpen" class="city-picker__menu" @tap.stop>
          <view
            v-for="(option, index) in cityOptions"
            :key="`${option.cityName}-${index}`"
            class="city-picker__option"
            @tap="selectCity(option)"
          >{{ option.cityName }}</view>
        </view>
      </view>
      <text v-else class="city-picker__label">{{ currentCity || '探索城市' }}</text>

      <view class="trip-link" role="link" @tap="go(APP_ROUTES.history)">
        <text>我的行程</text>
      </view>

      <view class="profile-link" role="link" @tap="go('/pages/profile/index')">
        <text>{{ loggedIn ? '个人中心' : '登录' }}</text>
      </view>

      <view
        class="menu-trigger"
        role="button"
        aria-label="打开导航菜单"
        @tap="open = !open"
      >
        <view class="menu-trigger__line" />
        <view class="menu-trigger__line" />
        <view class="menu-trigger__line" />
      </view>
    </view>

    <view v-if="open" class="mobile-menu">
      <view
        v-for="item in items"
        :key="item.key"
        class="mobile-menu__item"
        :class="{ 'mobile-menu__item--active': active === item.key }"
        role="link"
        @tap="go(item.url)"
      >
        <text>{{ item.label }}</text>
      </view>
      <view class="mobile-menu__item" role="link" @tap="go('/pages/profile/index')">
        <text>{{ loggedIn ? '个人中心' : '登录' }}</text>
      </view>
      <view class="mobile-menu__item" role="link" @tap="go(APP_ROUTES.history)">
        <text>我的行程</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';

import { getCityContext, resolveCityContext } from '../../api/scenic';
import { autoLocateCurrentCity, initializeCityContext, locateCurrentCity } from '../../composables/useCityContext';
import { APP_ROUTES, currentPrimaryRoute, navigateTo, PRIMARY_ROUTES } from '../../router';
import { useTouristStore } from '../../stores';
import type { CityVO } from '../../types';
import { isLoggedIn } from '../../utils/auth';

const store = useTouristStore();
const open = ref(false);
const cityMenuOpen = ref(false);
const loggedIn = isLoggedIn();
const currentCity = computed(() => store.currentCity?.cityName || '探索城市');
const cityOptions = computed(() => {
  const selected = store.currentCity;
  const cityPool = [selected, ...store.recentCities].filter((city): city is CityVO => Boolean(city));
  const unique = cityPool.filter((city, index) => cityPool.findIndex((item) => item.cityKey === city.cityKey
    || item.cityCode === city.cityCode) === index);
  return [
    ...unique.map((city) => ({
      cityName: city === selected ? `${city.cityName} ✓ 当前城市` : `${city.cityName} · 最近访问`,
      city
    })),
    { cityName: '定位当前位置', action: 'locate' as const },
    { cityName: '搜索更多城市…', action: 'discover' as const }
  ];
});
const active = currentPrimaryRoute();
const items = PRIMARY_ROUTES;

watch(currentCity, (next) => console.info('[NavbarCity] updated=', next), { immediate: true });

async function selectCity(option: (typeof cityOptions.value)[number]): Promise<void> {
  cityMenuOpen.value = false;
  if (option?.action === 'discover') {
    go(APP_ROUTES.cities);
    return;
  }
  if (option?.action === 'locate') {
    try { await locateCurrentCity(); } catch { go(APP_ROUTES.cities); }
    return;
  }
  const city = option?.city;
  if (!city || city.cityKey === store.currentCity?.cityKey) return;
  store.setCityContext(city.id ? await getCityContext(city.id) : await resolveCityContext(city.cityName, city.cityCode));
}

onMounted(async () => {
  try {
    await initializeCityContext();
    await autoLocateCurrentCity();
  } catch {
    // 页面负责展示业务错误；Header 保持可导航。
  }
});

function goHome(): void {
  open.value = false;
  uni.reLaunch({ url: APP_ROUTES.home });
}

function go(url: string): void {
  open.value = false;
  if (url === APP_ROUTES.home || url.startsWith(`${APP_ROUTES.home}?`)) {
    uni.reLaunch({ url });
    return;
  }
  if (PRIMARY_ROUTES.some((item) => item.url === url)) {
    uni.redirectTo({ url });
    return;
  }
  navigateTo(url);
}
</script>

<style scoped lang="scss">
.app-header {
  position: relative;
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  min-height: 68px;
  padding: 8px 20px 8px 24px;
  color: var(--jade-900);
  background: rgba(248, 246, 240, .78);
  border: 1px solid rgba(255, 255, 255, .7);
  border-radius: var(--radius);
  box-shadow: 0 12px 30px rgba(23, 63, 53, .10);
  backdrop-filter: blur(18px) saturate(120%);
}

.brand,
.nav,
.header-actions,
.city-picker,
.trip-link,
.profile-link {
  display: flex;
  align-items: center;
}

.brand {
  flex: 0 0 auto;
  gap: 10px;
  cursor: pointer;
}

.brand__mark {
  display: grid;
  place-items: center;
  width: 36px;
  height: 36px;
  color: var(--jade-700);
  border: 1px solid rgba(94, 140, 126, .62);
  border-radius: 50%;
  font-weight: 800;
}

.brand__name {
  font-family: Georgia, 'Songti SC', serif;
  font-size: 20px;
  font-weight: 800;
  letter-spacing: .08em;
}

.nav {
  flex: 1 1 auto;
  justify-content: center;
  gap: 4px;
  min-width: 0;
}

.nav__item {
  padding: 10px 14px;
  color: rgba(23, 63, 53, .72);
  font-size: 13px;
  white-space: nowrap;
  border-radius: 10px;
  cursor: pointer;
  transition: color .18s ease, background-color .18s ease;
}

.nav__item:active,
.nav__item--active {
  color: var(--ivory);
  background: var(--jade-900);
}

.header-actions {
  flex: 0 0 auto;
  gap: 12px;
  min-width: 0;
  white-space: nowrap;
}

.city-picker {
  position: relative;
  gap: 4px;
  color: var(--jade-700);
  font-size: 12px;
  cursor: pointer;
}

.city-picker__menu {
  position: absolute;
  top: calc(100% + 12px);
  right: 0;
  z-index: 30;
  width: 210px;
  padding: 8px;
  border: 1px solid rgba(26, 82, 68, 0.12);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 16px 34px rgba(14, 58, 48, 0.16);
}

.city-picker__option {
  padding: 10px 12px;
  border-radius: 9px;
  color: var(--jade-800);
}

.city-picker__option:hover { background: rgba(42, 111, 91, 0.08); }

.city-picker__label {
  overflow: hidden;
  max-width: 92px;
  text-overflow: ellipsis;
}

.city-picker__chevron {
  color: var(--jade-500);
  font-size: 16px;
  line-height: 1;
}

.trip-link {
  color: rgba(23, 63, 53, .72);
  font-size: 12px;
  cursor: pointer;
}

.profile-link {
  min-height: 38px;
  padding: 0 15px;
  color: var(--ivory);
  background: var(--jade-500);
  border-radius: 12px;
  font-size: 12px;
  cursor: pointer;
}

.menu-trigger,
.mobile-menu {
  display: none;
}

@media (max-width: 1100px) {
  .app-header {
    gap: 12px;
    padding-left: 16px;
  }

  .nav__item {
    padding-right: 9px;
    padding-left: 9px;
    font-size: 12px;
  }

  .city-picker {
    display: none;
  }

  .trip-link { display: none; }
}

@media (max-width: 767px) {
  .app-header {
    min-height: 56px;
    padding: 7px 14px;
    border-radius: 14px;
  }

  .brand__mark {
    width: 30px;
    height: 30px;
  }

  .brand__name {
    font-size: 16px;
  }

  .nav,
  .profile-link,
  .trip-link,
  .header-actions > .city-picker,
  .header-actions > .city-picker__label {
    display: none;
  }

  .header-actions {
    margin-left: auto;
  }

  .menu-trigger {
    display: grid;
    gap: 4px;
    width: 32px;
    padding: 7px 5px;
    cursor: pointer;
  }

  .menu-trigger__line {
    display: block;
    width: 22px;
    height: 2px;
    background: var(--jade-900);
    border-radius: 999px;
  }

  .mobile-menu {
    position: absolute;
    top: calc(100% + 8px);
    right: 0;
    left: 0;
    display: grid;
    gap: 4px;
    padding: 10px;
    background: rgba(248, 246, 240, .97);
    border: 1px solid rgba(23, 63, 53, .10);
    border-radius: 14px;
    box-shadow: 0 16px 36px rgba(23, 63, 53, .14);
  }

  .mobile-menu__item {
    padding: 12px 14px;
    color: var(--jade-900);
    font-size: 14px;
    border-radius: 10px;
  }

  .mobile-menu__item--active {
    color: var(--ivory);
    background: var(--jade-900);
  }
}
</style>
