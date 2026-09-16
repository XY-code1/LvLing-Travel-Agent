<template>
  <view class="amap-planner">
    <view id="guido-amap-container" class="amap-planner__canvas" />
    <view v-if="status !== 'ready'" class="amap-planner__state">
      <view v-if="status === 'loading'" class="amap-planner__spinner" />
      <text class="amap-planner__title">{{ statusTitle }}</text>
      <text class="amap-planner__message">{{ statusMessage }}</text>
    </view>
    <view v-if="status === 'ready' && routePending" class="amap-planner__notice">路线数据待计算</view>
  </view>
</template>

<script setup lang="ts">
import { load as loadAmap } from '@amap/amap-jsapi-loader';
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import type { TravelActivity } from '../../types';
import { planAmapWalkingRoute, type AmapCoordinate } from '../../services/amapService';

declare const __AMAP_JS_KEY__: string;
declare const __AMAP_SECURITY_CODE__: string;

const props = defineProps<{
  activities: TravelActivity[];
  cityName: string;
  cityCenter?: { longitude: number | null; latitude: number | null } | null;
}>();

const status = ref<'loading' | 'ready' | 'unconfigured' | 'error'>('loading');
const statusMessage = ref('正在载入地图底图…');
const routePending = ref(false);
let AMap: any;
let map: any;
let overlays: any[] = [];
let renderSequence = 0;

const statusTitle = computed(() => ({
  loading: '正在加载地图',
  ready: '',
  unconfigured: '地图服务暂未配置',
  error: '地图暂时无法加载'
}[status.value]));

function validActivities(): TravelActivity[] {
  return props.activities.filter((item) => Number.isFinite(item.coordinates.longitude)
    && Number.isFinite(item.coordinates.latitude));
}

async function init(): Promise<void> {
  if (!__AMAP_JS_KEY__ || !__AMAP_SECURITY_CODE__) {
    status.value = 'unconfigured';
    statusMessage.value = '请在 tourist-app/.env.local 配置 JS API Key 与 securityJsCode。';
    return;
  }
  (window as any)._AMapSecurityConfig = { securityJsCode: __AMAP_SECURITY_CODE__ };
  try {
    AMap = await loadAmap({ key: __AMAP_JS_KEY__, version: '2.0', plugins: ['AMap.Scale', 'AMap.ToolBar'] });
    await nextTick();
    const center = props.cityCenter && Number.isFinite(props.cityCenter.longitude) && Number.isFinite(props.cityCenter.latitude)
      ? [props.cityCenter.longitude, props.cityCenter.latitude]
      : [120.1551, 30.2741];
    map = new AMap.Map('guido-amap-container', { zoom: 11, center, viewMode: '2D', resizeEnable: true });
    map.addControl(new AMap.Scale());
    map.addControl(new AMap.ToolBar({ position: { top: '12px', right: '12px' } }));
    status.value = 'ready';
    await renderPlan();
  } catch (error) {
    console.error('[AMap] load failed', error);
    status.value = 'error';
    statusMessage.value = error instanceof Error ? error.message : '请检查 JS API Key、securityJsCode 与域名白名单。';
  }
}

async function renderPlan(): Promise<void> {
  if (!map || !AMap) return;
  const sequence = ++renderSequence;
  map.remove(overlays);
  overlays = [];
  const activities = validActivities();
  if (!activities.length) return;

  const markerOverlays = activities.map((activity, index) => {
    const marker = new AMap.Marker({
      position: [activity.coordinates.longitude, activity.coordinates.latitude],
      title: activity.poi.name,
      label: { direction: 'top', offset: new AMap.Pixel(0, -4), content: `<span class="guido-amap-marker">${index + 1}</span>` }
    });
    const info = new AMap.InfoWindow({
      offset: new AMap.Pixel(0, -28),
      content: `<div class="guido-amap-info"><strong>${escapeHtml(activity.poi.name)}</strong><span>${escapeHtml(activity.time)} · 停留${activity.duration || '待定'}分钟</span></div>`
    });
    marker.on('click', () => info.open(map, marker.getPosition()));
    return marker;
  });
  overlays.push(...markerOverlays);
  map.add(markerOverlays);
  map.setFitView(markerOverlays, false, [56, 42, 56, 42], 15);

  if (activities.length < 2) return;
  routePending.value = true;
  try {
    const points: AmapCoordinate[] = activities.map((item) => ({
      longitude: item.coordinates.longitude as number,
      latitude: item.coordinates.latitude as number
    }));
    const route = await planAmapWalkingRoute(points);
    if (sequence !== renderSequence || !route.polyline.length) return;
    const polyline = new AMap.Polyline({
      path: route.polyline.map((point) => [point.longitude, point.latitude]),
      strokeColor: '#176b55', strokeWeight: 6, strokeOpacity: .82,
      lineJoin: 'round', lineCap: 'round', showDir: true
    });
    overlays.push(polyline);
    map.add(polyline);
    map.setFitView(overlays, false, [56, 42, 56, 42], 15);
    routePending.value = false;
  } catch (error) {
    console.warn('[AMap Route] unavailable', error);
    routePending.value = true;
  }
}

function escapeHtml(value: string): string {
  return value.replace(/[&<>'"]/g, (char) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '"': '&quot;' }[char] || char));
}

onMounted(() => { void init(); });
watch(() => props.activities, () => { void renderPlan(); }, { deep: true });
watch(() => props.cityCenter, (center) => {
  if (map && center && Number.isFinite(center.longitude) && Number.isFinite(center.latitude) && !validActivities().length) {
    map.setCenter([center.longitude, center.latitude]);
  }
}, { deep: true });
onBeforeUnmount(() => { renderSequence += 1; map?.destroy(); map = null; overlays = []; });
</script>

<style scoped>
.amap-planner { position: relative; width: 100%; min-height: 430px; height: 100%; overflow: hidden; border-radius: 16px; background: #eaf1eb; }
.amap-planner__canvas { position: absolute; inset: 0; width: 100%; height: 100%; }
.amap-planner__state { position: absolute; inset: 0; z-index: 2; display: grid; place-content: center; justify-items: center; padding: 32px; text-align: center; background: linear-gradient(145deg, #edf3ee, #e3ece6); }
.amap-planner__spinner { width: 28px; height: 28px; margin-bottom: 16px; border: 3px solid rgba(23,107,85,.14); border-top-color: #176b55; border-radius: 50%; animation: amap-spin .8s linear infinite; }
.amap-planner__title { color: #123e34; font-size: 17px; font-weight: 800; }
.amap-planner__message { max-width: 280px; margin-top: 8px; color: #61736e; font-size: 12px; line-height: 1.65; }
.amap-planner__notice { position: absolute; right: 12px; bottom: 12px; z-index: 2; padding: 7px 10px; color: #52645f; background: rgba(255,255,255,.92); border-radius: 8px; font-size: 11px; box-shadow: 0 4px 16px rgba(20,65,52,.12); }
@keyframes amap-spin { to { transform: rotate(360deg); } }
</style>

<style>
.guido-amap-marker { display: grid; place-items: center; width: 24px; height: 24px; color: #fff; background: #123e34; border: 2px solid #fff; border-radius: 50%; font: 700 12px/1 sans-serif; box-shadow: 0 3px 10px rgba(18,62,52,.3); }
.guido-amap-info { display: grid; gap: 5px; min-width: 130px; padding: 2px; color: #123e34; font: 13px/1.4 sans-serif; }
.guido-amap-info span { color: #687a74; font-size: 12px; }
</style>
