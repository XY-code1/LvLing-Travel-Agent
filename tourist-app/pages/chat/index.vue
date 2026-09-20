<template>
  <view class="planner-page">
    <AppHeader />

    <view class="planner-shell">
      <view class="planner-heading">
        <view>
          <text class="planner-kicker">TRAVEL PLANNER</text>
          <text class="planner-title">{{ cityLabel }} · AI旅行规划</text>
          <text class="planner-subtitle">把想去的地方告诉灵灵，她会陪你把计划一步步整理清楚。</text>
        </view>
        <view class="planner-actions">
          <view class="planner-action" role="button" @tap="savePlanner">保存行程</view>
          <view class="planner-action planner-action--primary" role="button" @tap="sharePlanner">分享</view>
        </view>
      </view>

      <view class="planner-tabs" role="tablist">
        <view :class="['planner-tab', activeTab === 'chat' && 'planner-tab--active']" @tap="activeTab = 'chat'">AI</view>
        <view :class="['planner-tab', activeTab === 'itinerary' && 'planner-tab--active']" @tap="activeTab = 'itinerary'">行程</view>
        <view :class="['planner-tab', activeTab === 'map' && 'planner-tab--active']" @tap="activeTab = 'map'">地图</view>
      </view>

      <view v-if="travelPlan" class="agent-progress">
        <view class="agent-progress__heading">
          <text class="agent-progress__title">旅灵工作流</text>
          <text class="agent-progress__summary">{{ travelPlan.summary }}</text>
        </view>
        <view class="agent-progress__steps">
          <view v-for="step in travelPlan.steps" :key="step.id" class="agent-step">
            <view :class="['agent-step__dot', `agent-step__dot--${step.status.toLowerCase()}`]" />
            <text class="agent-step__label">{{ step.label }}</text>
          </view>
        </view>
      </view>

      <view class="planner-grid">
        <view :class="['planner-panel', 'planner-panel--chat', activeTab !== 'chat' && 'planner-panel--mobile-hidden']">
          <view class="panel-heading">
            <view>
              <text class="panel-title">AI对话</text>
              <text class="panel-caption">灵灵会结合城市资料回答</text>
            </view>
            <view class="panel-status"><view class="status-dot" />{{ aiLive ? 'AI：Live' : 'AI：Demo fallback' }}</view>
          </view>

          <view v-if="travelRequest" class="request-summary">
            <text class="request-summary__label">已识别</text>
            <text v-if="travelRequest.destination">{{ travelRequest.destination }}</text>
            <text v-if="travelRequest.days">{{ travelRequest.days }}天</text>
            <text v-if="travelRequest.budget">¥{{ travelRequest.budget }}</text>
            <text v-if="travelRequest.travelers">{{ travelRequest.travelers }}人</text>
            <text v-if="travelRequest.mobilityPreference">{{ travelRequest.mobilityPreference }}</text>
          </view>

          <AvatarBox
            class="planner-avatar"
            :connected="Boolean(sessionNo)"
            :stream-url="lastAnswer?.streamUrl || null"
            :audio-url="lastAnswer?.audioUrl || null"
            :avatar="store.selectedAvatar"
            image-override="/static/digital-human/lingling-jade.png"
            name-override="灵灵"
            meta-override="AI旅行数字员工"
            :text="lastAnswer?.answer || ''"
          />

          <scroll-view class="message-list" scroll-y>
            <view v-if="messages.length === 0" class="chat-empty">
              <text class="chat-empty__title">告诉旅灵你想去哪里</text>
              <text class="chat-empty__desc">例如：杭州两天，预算1500，不想太累</text>
              <view class="chat-empty__prompt" @tap="question = '杭州两天，预算1500，不想太累'">杭州两天，预算1500，不想太累</view>
            </view>
            <ChatBubble
              v-for="item in messages"
              :key="item.id"
              :role="item.role"
              :text="item.text"
              :avatar-image="assistantAvatarImage"
              :user-initial="userInitial"
            />
          </scroll-view>

          <view v-if="lastAnswer" class="source-card">
            <text class="tag">{{ lastAnswer.hitKb === 1 ? '知识库命中' : '通用回答' }}</text>
            <text class="muted">耗时 {{ lastAnswer.costMs }} ms / 情绪 {{ emotionText(lastAnswer.emotion) }}</text>
          </view>

          <view class="composer">
            <input v-model="question" class="input" placeholder="例如：杭州两天，预算1500，不想太累" @confirm="sendText" />
            <button class="button" :loading="loading" @tap="sendText">发送</button>
            <VoiceButton :recording="recording" :loading="voiceLoading" @toggle="toggleRecord" />
          </view>
          <view v-if="isDev" class="agent-debug">
            <text>Agent Debug</text>
            <text>request: {{ agentDebug.request }}</text>
            <text>HTTP: {{ agentDebug.http }}</text>
            <text>rawMessage: {{ agentDebug.rawMessage }} chars</text>
            <text>normalizedMessage: {{ agentDebug.normalizedMessage }} chars</text>
            <text>itinerary: {{ agentDebug.itineraryDays }} days</text>
            <text>latency: {{ agentDebug.latency }} ms</text>
            <text>errorType: {{ agentDebug.errorType || 'none' }}</text>
            <text>errorMessage: {{ agentDebug.errorMessage || 'none' }}</text>
            <text>requestUrl: {{ agentDebug.requestUrl || 'none' }}</text>
          </view>
        </view>

        <view :class="['planner-panel', 'planner-panel--itinerary', activeTab !== 'itinerary' && 'planner-panel--mobile-hidden']">
          <view class="panel-heading">
            <view>
              <text class="panel-title">行程计划</text>
              <text class="panel-caption">灵灵整理后的每日安排</text>
            </view>
            <view class="panel-heading__tools">
              <text class="panel-count">{{ activeRoute ? 'DAY 01' : '待规划' }}</text>
              <view class="panel-inline-action" role="button" @tap="loadPlannerRoute">
                {{ routeLoading ? '规划中…' : '生成路线' }}
              </view>
              <view class="panel-inline-action panel-inline-action--quiet" role="button" @tap="openRoutePage">
                路线详情
              </view>
            </view>
          </view>
          <view v-if="activePlanDay" class="itinerary-content">
            <view v-if="travelPlan" class="plan-overview">
              <text class="plan-overview__summary">{{ travelPlan.summary }}</text>
              <view class="plan-metrics">
                <view><text>预算</text><text>{{ formatBudget(travelPlan.budget.budget) }}</text></view>
                <view><text>已估</text><text>{{ formatBudget(travelPlan.budget.estimatedTotal) }}</text></view>
                <view><text>路程</text><text>{{ formatDistanceKm(travelPlan.budget.totalDistanceKm) }}</text></view>
              </view>
            </view>
            <view class="route-summary">
              <text class="route-summary__title">{{ activeRoute?.name || activePlanDay.title }}</text>
              <text class="route-summary__meta">DAY {{ String(activePlanDay.day).padStart(2, '0') }} · {{ activePlanDay.activities.length }} 个安排</text>
            </view>
            <text class="route-summary__reason">{{ activeRoute?.recommendReason || '根据当前可用城市资料生成。' }}</text>
            <view class="timeline">
              <view v-for="activity in activePlanDay.activities" :key="`${activity.poi.id}-${activity.time}`" class="timeline__item">
                <view class="timeline__marker">{{ activity.time }}</view>
                <view class="timeline__copy">
                  <text class="timeline__name">{{ activity.poi.name }}</text>
                  <text class="timeline__hint">{{ activity.transport || '行程安排' }}<text v-if="activity.duration"> · 约{{ activity.duration }}分钟</text></text>
                </view>
              </view>
            </view>
            <view v-for="planDay in (travelPlan?.days.slice(1) || [])" :key="planDay.day">
              <view class="route-summary">
                <text class="route-summary__title">{{ planDay.title }}</text>
                <text class="route-summary__meta">DAY {{ String(planDay.day).padStart(2, '0') }} · {{ planDay.activities.length }} 个安排</text>
              </view>
              <view class="timeline">
                <view v-for="activity in planDay.activities" :key="`${planDay.day}-${activity.poi.name}-${activity.time}`" class="timeline__item">
                  <view class="timeline__marker">{{ activity.time }}</view>
                  <view class="timeline__copy">
                    <text class="timeline__name">{{ activity.poi.name }}</text>
                    <text class="timeline__hint">{{ activity.transport || '行程安排' }} · 约 {{ activity.duration || 60 }} 分钟</text>
                  </view>
                </view>
              </view>
            </view>
          </view>
          <view v-else class="workspace-empty">
            <view class="workspace-empty__icon">✦</view>
            <text class="workspace-empty__title">{{ routeLoading ? '正在整理路线' : '还没有行程计划' }}</text>
            <text class="workspace-empty__desc">{{ routeMessage }}</text>
          </view>
        </view>

        <view :class="['planner-panel', 'planner-panel--map', activeTab !== 'map' && 'planner-panel--mobile-hidden']">
          <view class="panel-heading">
            <view>
              <text class="panel-title">路线定位</text>
              <text class="panel-caption">真实点位坐标 · 可打开高德导航</text>
            </view>
            <view class="panel-heading__tools">
              <text class="panel-count">GAODE</text>
              <view class="panel-inline-action" role="button" @tap="locateNearby">
                {{ locating ? '定位中…' : '定位附近' }}
              </view>
            </view>
          </view>
          <view class="map-live">
            <AmapPlannerMap :activities="mapActivities" :city-name="cityLabel" :city-center="cityCenter" :route="travelPlan?.route"
              @location="handleMapLocation" @location-error="handleLocationError" />
            <view v-if="nearbySpots.length" class="nearby-list map-live__nearby">
              <text class="nearby-list__label">当前位置附近</text>
              <view v-for="spot in nearbySpots" :key="spot.spotId" class="nearby-list__item">
                <text>{{ spot.name }}</text><text>{{ formatDistance(spot.distanceMeters) }}</text>
              </view>
            </view>
            <view v-if="mapActivities.length" class="map-live__footer">
              <text>{{ locationMessage }}</text>
              <view class="panel-inline-action" role="button" @tap="openRouteMap">打开高德导航</view>
            </view>
          </view>
        </view>
      </view>

      <view class="planner-checks">
        <view v-for="item in plannerChecks" :key="item.key" class="planner-check">
          <text :class="['check-icon', `check-icon--${item.check.status.toLowerCase()}`]">{{ checkIcon(item.check.status) }}</text>
          <text>{{ item.label }}</text>
          <text class="check-status">{{ checkStatusText(item.check.status) }}</text>
          <text v-if="item.key === 'weather' && item.check.status !== 'PENDING'" class="check-detail">{{ item.check.message }}</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { onLoad } from '@dcloudio/uni-app';

import { getCurrentAvatar } from '../../api/avatar';
import { askTextStream, askVoiceStream, createSession, getSessionMessages, type ChatStreamHandlers } from '../../api/chat';
import { parseTravelRequest, planTravel } from '../../services/travelPlanner';
import { resolveCityContext } from '../../api/scenic';
import AvatarBox from '../../components/AvatarBox/index.vue';
import ChatBubble from '../../components/ChatBubble/index.vue';
import AppHeader from '../../components/layout/AppHeader.vue';
import AmapPlannerMap from '../../components/planner/AmapPlannerMap.vue';
import VoiceButton from '../../components/VoiceButton/index.vue';
import { initializeCityContext } from '../../composables/useCityContext';
import { navigateTo } from '../../router';
import { useTouristStore } from '../../stores';
import type {
  ChatAnswerVO,
  MessageVO,
  CityPoiVO,
  PlannerCheckStatus,
  RouteRecommendVO,
  SourceVO,
  TravelPlan,
  TravelRequest
} from '../../types';
import { isLoggedIn } from '../../utils/auth';
import { emotionText } from '../../utils/display';
import { getAiRuntimeStatus, runTravelAgent, type AgentPlanResponse } from '../../services/travelAgentService';

interface MessageItem {
  id: string;
  role: 'user' | 'ai';
  text: string;
}

const store = useTouristStore();
const loading = ref(false);
const isDev = typeof process !== 'undefined' && process.env?.NODE_ENV !== 'production';
const agentDebug = ref({ request: 'idle', http: 0, rawMessage: 0, normalizedMessage: 0, itineraryDays: 0, latency: 0, errorType: '', errorMessage: '', requestUrl: '/api/tourist/agent/plan' });
const aiLive = ref(false);
const voiceLoading = ref(false);
const recording = ref(false);
const question = ref('');
const messages = ref<MessageItem[]>([]);
const lastAnswer = ref<ChatAnswerVO | null>(null);
const activeTab = ref<'chat' | 'itinerary' | 'map'>('chat');
const plannerRoutes = ref<RouteRecommendVO[]>([]);
const travelPlan = ref<TravelPlan | null>(null);
const nearbySpots = ref<Array<{ spotId: string; name: string; distanceMeters: number; canGuide: boolean }>>([]);
const routeLoading = ref(false);
const locating = ref(false);
const routeMessage = ref('先生成一条当前城市的真实路线，之后再用左侧 AI 对话细化偏好。');
const latestAgentMessage = ref('');
const latestAgentLatencyMs = ref(0);
const locationMessage = ref('尚未获取当前位置');
const currentLocation = ref<{ longitude: number; latitude: number; city: string } | null>(null);
const sessionNo = computed(() => store.currentSessionNo);
const cityLabel = computed(() => store.travelTaskContext?.destinationCity?.cityName
  || store.currentCity?.cityName || store.featuredCityContext?.city?.cityName || '探索城市');
const activeRoute = computed(() => plannerRoutes.value[0] || null);
const activeRouteSpots = computed(() => activeRoute.value?.spots || []);
const travelRequest = computed<TravelRequest | null>(() => travelPlan.value?.request || null);
const activePlanDay = computed(() => travelPlan.value?.days[0] || null);
const mapActivities = computed(() => activePlanDay.value?.activities || []);
const activeCity = computed(() => store.travelTaskContext?.destinationCity || store.currentCity
  || store.featuredCityContext?.city || null);
const cityCenter = computed(() => activeCity.value ? {
  longitude: activeCity.value.longitude,
  latitude: activeCity.value.latitude
} : null);
const plannerChecks = computed(() => {
  const pending = (message: string) => ({
    status: 'PENDING' as PlannerCheckStatus,
    message,
    source: 'planner.pending'
  });
  const checks = travelPlan.value?.checks;
  return [
    { key: 'weather', label: '天气', check: checks?.weather || pending('天气工具尚未接入') },
    { key: 'route', label: '路线', check: checks?.route || pending('等待路线') },
    { key: 'time', label: '时间', check: checks?.timeConflict || pending('等待行程') },
    { key: 'budget', label: '预算', check: checks?.budget || pending('等待预算') }
  ];
});
const assistantAvatarImage = computed(() => '/static/digital-human/lingling-jade.png');
const userInitial = computed(() => (store.profile?.nickname || store.touristInfo?.nickname || '我').slice(0, 1));
let uniRecorder: ReturnType<typeof uni.getRecorderManager> | null = null;
let mediaRecorder: MediaRecorder | null = null;
let mediaStream: MediaStream | null = null;
let mediaChunks: BlobPart[] = [];
let locationPlanPending = false;

watch(() => store.cityContext?.cityKey, (next, previous) => {
  if (!previous || next === previous) return;
  plannerRoutes.value = [];
  travelPlan.value = null;
  nearbySpots.value = [];
  currentLocation.value = null;
  routeMessage.value = `已切换到${cityLabel.value}，请重新生成当前城市路线。`;
});

onLoad((query) => {
  if (typeof query.question === 'string') {
    try {
      question.value = decodeURIComponent(query.question);
    } catch {
      question.value = query.question;
    }
  }
});

onMounted(() => {
  void getAiRuntimeStatus().then((status) => { aiLive.value = status.live; }).catch(() => { aiLive.value = false; });
  void initializeCityContext().catch(() => null).finally(() => {
    if (question.value.trim()) void loadPlannerRoute(question.value.trim());
  });
  if (isLoggedIn()) {
    void loadAvatarState()
      .then(() => restoreCurrentSession())
      .catch((error: unknown) => {
        const message = error instanceof Error ? error.message : '数字人形象加载失败';
        uni.showToast({ title: message, icon: 'none' });
      });
  }
});

onBeforeUnmount(() => {
  stopBrowserTracks();
});

async function loadPlannerRoute(
  requestText?: string,
  onAgentResponse?: (response: AgentPlanResponse) => void
): Promise<Awaited<ReturnType<typeof planTravel>> | null> {
  routeLoading.value = true;
  try {
    const text = typeof requestText === 'string' ? requestText : question.value.trim();
    const result = await planTravel(store.currentScenicId, text,
      currentLocation.value?.city || cityLabel.value, currentLocation.value, onAgentResponse);
    plannerRoutes.value = result.routes;
    travelPlan.value = result.plan;
    latestAgentMessage.value = result.agentMessage;
    latestAgentLatencyMs.value = result.agentLatencyMs;
    try {
      await syncTravelTaskContext(text, result);
    } catch (error: unknown) {
      console.warn('[TravelAgent] context enrichment failed; preserving Agent response', {
        errorName: error instanceof Error ? error.name : 'UnknownError',
        message: error instanceof Error ? error.message : String(error)
      });
    }
    routeMessage.value = plannerRoutes.value.length
      ? result.plan.summary
      : '已识别需求，但当前城市暂时没有已配置的公开路线。';
    if (plannerRoutes.value.length) {
      locationMessage.value = '路线点位已准备，可继续定位附近景点。';
    }
    return result;
  } catch (error: unknown) {
    console.error('[TravelAgent Failure]', {
      url: '/api/tourist/agent/plan',
      status: 0,
      statusText: '',
      errorName: error instanceof Error ? error.name : 'UnknownError',
      message: error instanceof Error ? error.message : String(error)
    });
    plannerRoutes.value = [];
    routeMessage.value = error instanceof Error ? error.message : '路线加载失败，请稍后重试。';
    return null;
  } finally {
    routeLoading.value = false;
    if (locationPlanPending && currentLocation.value) {
      locationPlanPending = false;
      void loadPlannerRoute(question.value.trim());
    }
  }
}

async function syncTravelTaskContext(text: string, result: Awaited<ReturnType<typeof planTravel>>): Promise<void> {
  const destination = result.request.destination || cityLabel.value;
  let destinationContext = store.travelTaskContext?.destinationCityContext || null;
  if (!destinationContext || destinationContext.city?.cityName !== destination) {
    destinationContext = await resolveCityContext(destination);
  }
  const previous = store.travelTaskContext || store.startTravelTask(text);
  store.setTravelTaskContext({
    ...previous,
    rawRequest: text,
    currentCity: store.currentCity,
    destinationCity: destinationContext.city,
    destinationCityContext: destinationContext,
    date: result.request.startDate,
    duration: result.request.days,
    travelers: result.request.travelers ?? result.agentIntent.partyType,
    mobility: result.request.mobilityPreference || (result.agentIntent.mobilityConstraint ? 'LOW' : null),
    budget: result.request.budget,
    interests: result.request.preferences,
    mustVisit: result.request.requiredPlaces.length ? result.request.requiredPlaces : result.agentIntent.requestedPois,
    selectedPois: destinationContext.pois,
    route: result.plan.route,
    weather: destinationContext.weather,
    services: destinationContext.services,
    validation: result.plan.checks,
    executionTrace: result.executionTrace.map((step) => ({
      step: step.step || String(step.taskId), status: step.status, tool: step.toolName, message: step.message || null
    })),
    updatedAt: new Date().toISOString()
  });
}

function handleMapLocation(value: { longitude: number; latitude: number; city: string }): void {
  currentLocation.value = value;
  locationMessage.value = value.city ? `已定位：${value.city}` : '已获取当前位置';
  if (typeof process !== 'undefined' && process.env?.NODE_ENV !== 'production') {
    console.info('[Location] browser coordinates received', { city: value.city, available: true });
  }
  rerunCurrentLocationPlan();
}

function handleLocationError(message: string): void {
  if (currentLocation.value) {
    return;
  }
  currentLocation.value = null;
  locationMessage.value = message;
}

function locateNearby(): void {
  if (locating.value) {
    return;
  }
  locating.value = true;
  const onSuccess = (longitude: number, latitude: number): void => {
    currentLocation.value = { longitude, latitude, city: currentLocation.value?.city || '' };
    void loadNearbySpots(longitude, latitude);
    rerunCurrentLocationPlan();
  };
  const onFailure = (): void => {
    locating.value = false;
    locationMessage.value = '浏览器未授予定位权限，请允许定位后重试。';
  };
  if (typeof navigator !== 'undefined' && navigator.geolocation) {
    navigator.geolocation.getCurrentPosition(
      (position) => onSuccess(position.coords.longitude, position.coords.latitude),
      onFailure,
      { enableHighAccuracy: false, timeout: 8000, maximumAge: 30000 }
    );
    return;
  }
  uni.getLocation({
    type: 'gcj02',
    success: (response: { longitude: number; latitude: number }) => onSuccess(response.longitude, response.latitude),
    fail: onFailure
  });
}

function rerunCurrentLocationPlan(): void {
  const text = question.value.trim();
  if (text.includes('当前位置')) {
    if (routeLoading.value) {
      locationPlanPending = true;
      return;
    }
    void loadPlannerRoute(text);
  }
}

async function loadNearbySpots(longitude: number, latitude: number): Promise<void> {
  try {
    nearbySpots.value = cityPoisByDistance(longitude, latitude).slice(0, 8).map(({ poi, distanceMeters }) => ({
      spotId: poi.id,
      name: poi.name,
      distanceMeters,
      canGuide: poi.source === 'local' && /^\d+$/.test(poi.id)
    }));
    locationMessage.value = nearbySpots.value.length
      ? `已定位，附近有 ${nearbySpots.value.length} 个可讲解点位。`
      : '已定位，但当前位置附近暂无可讲解点位。';
  } catch (error: unknown) {
    nearbySpots.value = [];
    locationMessage.value = error instanceof Error ? error.message : '附近点位加载失败，请稍后重试。';
  } finally {
    locating.value = false;
  }
}

function cityPoisByDistance(longitude: number, latitude: number): Array<{ poi: CityPoiVO; distanceMeters: number }> {
  return (store.experienceCityContext?.pois || []).filter((poi) => poi.longitude != null && poi.latitude != null)
    .map((poi) => ({ poi, distanceMeters: Math.round(distance(latitude, longitude, poi.latitude!, poi.longitude!) * 1000) }))
    .sort((left, right) => left.distanceMeters - right.distanceMeters);
}

function distance(lat1: number, lng1: number, lat2: number, lng2: number): number {
  const radians = Math.PI / 180;
  const value = Math.sin((lat2 - lat1) * radians / 2) ** 2
    + Math.cos(lat1 * radians) * Math.cos(lat2 * radians) * Math.sin((lng2 - lng1) * radians / 2) ** 2;
  return 6371 * 2 * Math.atan2(Math.sqrt(value), Math.sqrt(1 - value));
}

function savePlanner(): void {
  if (!activeRoute.value) {
    uni.showToast({ title: '请先生成路线', icon: 'none' });
    return;
  }
  uni.setStorageSync('guido_planner_draft', {
    city: cityLabel.value,
    scenicId: store.currentScenicId,
    route: activeRoute.value,
    plan: travelPlan.value,
    savedAt: new Date().toISOString()
  });
  uni.showToast({ title: '已保存到本机草稿', icon: 'success' });
}

async function sharePlanner(): Promise<void> {
  const shareUrl = typeof window !== 'undefined' ? window.location.href : '/pages/chat/index';
  const shareData = {
    title: `${cityLabel.value} · AI旅行规划`,
    text: activeRoute.value?.name || '旅灵 AI旅行规划',
    url: shareUrl
  };
  const browserNavigator = typeof navigator !== 'undefined'
    ? navigator as Navigator & { share?: (data: typeof shareData) => Promise<void> }
    : null;
  try {
    if (browserNavigator?.share) {
      await browserNavigator.share(shareData);
      return;
    }
    await new Promise<void>((resolve, reject) => {
      uni.setClipboardData({ data: shareUrl, success: () => resolve(), fail: reject });
    });
    uni.showToast({ title: '规划链接已复制', icon: 'success' });
  } catch (error: unknown) {
    if (error instanceof Error && error.name === 'AbortError') {
      return;
    }
    uni.showToast({ title: '分享失败，请复制当前页面链接', icon: 'none' });
  }
}

function openRoutePage(): void {
  navigateTo('/pages/route/index');
}

function openRouteMap(): void {
  const points = mapActivities.value.filter((activity) =>
    typeof activity.coordinates.longitude === 'number' && typeof activity.coordinates.latitude === 'number'
  );
  if (!points.length) {
    uni.showToast({ title: '当前路线缺少景点坐标', icon: 'none' });
    return;
  }
  const origin = points[0];
  const destination = points[points.length - 1];
  if (!origin || !destination) {
    return;
  }
  const via = points.slice(1, -1)
    .map((activity) => `${activity.coordinates.longitude},${activity.coordinates.latitude},${encodeURIComponent(activity.poi.name)}`)
    .join(';');
  const mapUrl = `https://uri.amap.com/navigation?from=${origin.coordinates.longitude},${origin.coordinates.latitude},${encodeURIComponent(origin.poi.name)}&to=${destination.coordinates.longitude},${destination.coordinates.latitude},${encodeURIComponent(destination.poi.name)}&via=${via}&mode=walk`;
  if (typeof window !== 'undefined') {
    window.open(mapUrl, '_blank', 'noopener,noreferrer');
    return;
  }
  uni.setClipboardData({ data: mapUrl, success: () => uni.showToast({ title: '地图链接已复制', icon: 'success' }) });
}

function formatDistance(distanceMeters: number): string {
  if (distanceMeters >= 1000) {
    return `${(distanceMeters / 1000).toFixed(1)}km`;
  }
  return `${distanceMeters}m`;
}

function formatBudget(value: number | null): string {
  return value === null ? '待估' : `¥${value}`;
}

function formatDistanceKm(value: number | null): string {
  return value === null ? '待计算' : `${value.toFixed(1)}km`;
}

function checkIcon(status: PlannerCheckStatus): string {
  return status === 'PASS' ? '✓' : status === 'WARN' ? '!' : status === 'FAIL' ? '×' : '·';
}

function checkStatusText(status: PlannerCheckStatus): string {
  return { PASS: '通过', WARN: '需关注', PENDING: '待接入', FAIL: '失败' }[status];
}

async function ensureSession(): Promise<string> {
  await ensureAvatarLoaded();
  const avatarId = store.selectedAvatar?.id;
  if (store.currentSessionNo) {
    if (!store.currentSessionAvatarId || !avatarId || store.currentSessionAvatarId === avatarId) {
      return store.currentSessionNo;
    }
  }
  const session = await createSession(store.currentScenicId, avatarId);
  const selected = session.selectedAvatar || session.defaultAvatar || store.selectedAvatar;
  store.setSession(session.sessionNo, session.scenicId, selected?.id || null);
  store.setSelectedAvatar(selected || null);
  return session.sessionNo;
}

async function ensureAvatarLoaded(): Promise<void> {
  if (!store.selectedAvatar) {
    try {
      await loadAvatarState();
    } catch (error) {
      // Avatar configuration is optional; text chat must still be usable.
      console.warn('[Chat] avatar unavailable', error);
    }
  }
}

async function loadAvatarState(): Promise<void> {
  const current = await getCurrentAvatar();
  store.setSelectedAvatar(current || null);
}

async function restoreCurrentSession(): Promise<void> {
  if (store.currentSessionNo) {
    await loadSessionMessages(store.currentSessionNo);
  }
}

async function loadSessionMessages(sessionNoValue: string): Promise<void> {
  if (messages.value.length > 0) {
    return;
  }
  const records = await getSessionMessages(sessionNoValue);
  if (records.length === 0) {
    return;
  }
  const restored: MessageItem[] = [];
  records.forEach((item) => {
    const questionText = historyQuestionText(item);
    if (questionText) {
      restored.push({ id: `u-${item.messageId}`, role: 'user', text: questionText });
    }
    if (item.answer) {
      restored.push({ id: `a-${item.messageId}`, role: 'ai', text: item.answer });
    }
  });
  messages.value = restored;
  const latest = [...records].reverse().find((item) => item.answer);
  if (latest) {
    lastAnswer.value = historyAnswer(latest);
  }
}

function normalizeAgentResponse(result: AgentPlanResponse, rawText: string) {
  const request = parseTravelRequest(rawText, cityLabel.value);
  const days = (result.itinerary || []).map((day) => ({
    day: day.day,
    title: day.theme,
    activities: day.items.map((item) => ({
      time: item.time,
      poi: { id: null, name: item.name },
      coordinates: { longitude: item.longitude ?? null, latitude: item.latitude ?? null },
      duration: item.durationMinutes,
      transport: null,
      estimatedCost: null,
      reason: item.reason
    }))
  }));
  return {
    assistantMessage: result.message?.trim() || '',
    itinerary: days,
    budget: { budget: result.budget?.total ?? request.budget, estimatedTotal: result.budget?.total ?? null },
    route: null,
    source: result.llmCalled && !result.fallback ? 'LIVE' : 'fallback',
    latency: result.latencyMs,
    plan: {
      request,
      summary: result.message || `${request.destination || cityLabel.value} 行程已生成`,
      budget: {
        budget: request.budget,
        estimatedTotal: result.budget?.total ?? null,
        transport: null,
        dining: null,
        tickets: null,
        totalDistanceKm: null,
        walkingDistanceKm: null,
        note: '预算来自 Travel Agent。'
      },
      checks: {
        weather: { status: 'PENDING', message: '等待天气 enrichment', source: 'agent' },
        openingHours: { status: 'PENDING', message: '等待开放时间 enrichment', source: 'agent' },
        route: { status: 'PENDING', message: '等待路线 enrichment', source: 'agent' },
        timeConflict: { status: days.length ? 'PASS' : 'PENDING', message: days.length ? 'Agent 已生成行程' : '等待行程', source: 'agent' },
        budget: { status: result.budget?.total != null ? 'PASS' : 'PENDING', message: '预算来自 Travel Agent', source: 'agent' }
      },
      days,
      steps: [],
      routeId: null,
      route: null
    } as TravelPlan
  };
}

async function sendText(): Promise<void> {
  if (loading.value) return;
  if (isLoggedIn()) {
    await sendChatText();
    return;
  }
  const text = question.value.trim();
  if (!text) {
    uni.showToast({ title: '请输入问题', icon: 'none' });
    return;
  }
  const startedAt = typeof performance !== 'undefined' ? performance.now() : Date.now();
  agentDebug.value = { ...agentDebug.value, request: 'sending', http: 0, errorType: '', errorMessage: '', requestUrl: '/api/tourist/agent/plan' };
  console.log('[FrontendRequest]', { text, startTime: Date.now(), url: '/api/tourist/agent/plan' });
  loading.value = true;
  const aiMessageId = `a-${Date.now()}`;
  messages.value.push({ id: `u-${Date.now()}`, role: 'user', text });
  messages.value.push({ id: aiMessageId, role: 'ai', text: '正在思考…' });
  beginAnswer();
  question.value = '';
  try {
    const raw = await runTravelAgent(text, currentLocation.value?.city || cityLabel.value, currentLocation.value);
    agentDebug.value = { ...agentDebug.value, http: 200, rawMessage: raw.message?.length || 0 };
    console.log('[FrontendRawResponse]', { status: 200, raw, elapsed: Math.round((typeof performance !== 'undefined' ? performance.now() : Date.now()) - startedAt) });
    const result = normalizeAgentResponse(raw, text);
    agentDebug.value = { ...agentDebug.value, normalizedMessage: result.assistantMessage.length, itineraryDays: result.itinerary.length };
    console.log('[FrontendNormalized]', {
      assistantMessage: result.assistantMessage,
      assistantMessageLength: result.assistantMessage.length,
      itineraryDays: result.itinerary.length,
      budget: result.budget,
      source: result.source
    });
    if (!result.assistantMessage) throw new Error('AGENT_MESSAGE_EMPTY');
    latestAgentMessage.value = result.assistantMessage;
    latestAgentLatencyMs.value = Math.max(1, Math.round((typeof performance !== 'undefined' ? performance.now() : Date.now()) - startedAt));
    agentDebug.value = { ...agentDebug.value, request: 'success', latency: latestAgentLatencyMs.value, errorType: '', errorMessage: '' };
    travelPlan.value = result.plan;
    patchAnswer({ answer: result.assistantMessage, costMs: latestAgentLatencyMs.value });
    updateMessage(aiMessageId, result.assistantMessage);
    console.log('[FrontendState]', {
      messagesCount: messages.value.length,
      latestMessage: messages.value[messages.value.length - 1],
      latestAgentMessage: latestAgentMessage.value,
      itineraryDays: travelPlan.value.days.length
    });
    // Route/weather enrichment remains available through the itinerary route action;
    // it must never trigger a second Agent request or block this response.
  } catch (error: unknown) {
    const fallbackMessage = '灵灵暂时没有生成回复，请稍后重试。';
    patchAnswer({ answer: fallbackMessage, costMs: 0 });
    updateMessage(aiMessageId, fallbackMessage);
    const transportError = error as Error & { errorType?: string; statusCode?: number; requestUrl?: string };
    agentDebug.value = {
      ...agentDebug.value,
      request: 'error',
      http: transportError.statusCode ?? 0,
      latency: Math.max(1, Math.round((typeof performance !== 'undefined' ? performance.now() : Date.now()) - startedAt)),
      errorType: transportError.errorType || transportError.name || 'UNKNOWN_ERROR',
      errorMessage: transportError.message,
      requestUrl: transportError.requestUrl || '/api/tourist/agent/plan'
    };
    console.error('[TravelAgent Failure]', {
      url: '/api/tourist/agent/plan',
      status: 0,
      statusText: '',
      errorName: error instanceof Error ? error.name : 'UnknownError',
      message: error instanceof Error ? error.message : String(error)
    });
  } finally {
    loading.value = false;
  }
}

async function sendChatText(): Promise<void> {
  const text = question.value.trim();
  if (!text) return;
  loading.value = true;
  const aiMessageId = `a-${Date.now()}`;
  messages.value.push({ id: `u-${Date.now()}`, role: 'user', text });
  messages.value.push({ id: aiMessageId, role: 'ai', text: '正在思考…' });
  question.value = '';
  beginAnswer();
  try {
    const answer = await askTextStream(await ensureSession(), text, undefined, streamHandlers(aiMessageId));
    finishAnswer(aiMessageId, answer);
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : '对话失败，请稍后重试';
    patchAnswer({ answer: message });
    updateMessage(aiMessageId, message);
  } finally {
    loading.value = false;
  }
}

function toggleRecord(): void {
  if (recording.value) {
    stopRecord();
    return;
  }
  void startRecord();
}

async function startRecord(): Promise<void> {
  if (voiceLoading.value) {
    return;
  }
  if (canUseBrowserRecorder()) {
    await startBrowserRecord();
    return;
  }
  startUniRecord();
}

function stopRecord(): void {
  if (mediaRecorder && mediaRecorder.state !== 'inactive') {
    mediaRecorder.stop();
    return;
  }
  if (uniRecorder) {
    uniRecorder.stop();
  }
}

async function startBrowserRecord(): Promise<void> {
  try {
    mediaStream = await navigator.mediaDevices.getUserMedia({ audio: true });
    mediaChunks = [];
    const mimeType = preferredMimeType();
    mediaRecorder = new MediaRecorder(mediaStream, mimeType ? { mimeType } : undefined);
    mediaRecorder.ondataavailable = (event) => {
      if (event.data && event.data.size > 0) {
        mediaChunks.push(event.data);
      }
    };
    mediaRecorder.onstop = () => {
      recording.value = false;
      const blob = new Blob(mediaChunks, { type: mimeType || 'audio/webm' });
      stopBrowserTracks();
      void toAsrWavBlob(blob).then((audio) => sendVoice(audio));
    };
    mediaRecorder.onerror = () => {
      recording.value = false;
      stopBrowserTracks();
      uni.showToast({ title: '录音失败', icon: 'none' });
    };
    recording.value = true;
    mediaRecorder.start();
    window.setTimeout(() => {
      if (mediaRecorder && mediaRecorder.state === 'recording') {
        mediaRecorder.stop();
      }
    }, 60000);
  } catch {
    recording.value = false;
    stopBrowserTracks();
    uni.showToast({ title: '无法打开麦克风，请检查浏览器权限', icon: 'none' });
  }
}

function startUniRecord(): void {
  const recorder = getUniRecorder();
  if (!recorder) {
    uni.showToast({ title: '当前环境不支持录音', icon: 'none' });
    return;
  }
  recording.value = true;
  recorder.start({ duration: 60000, format: 'mp3' });
}

function getUniRecorder(): ReturnType<typeof uni.getRecorderManager> | null {
  if (typeof uni.getRecorderManager !== 'function') {
    return null;
  }
  if (uniRecorder) {
    return uniRecorder;
  }
  uniRecorder = uni.getRecorderManager();
  uniRecorder.onStop((result: { tempFilePath: string }) => {
    recording.value = false;
    void sendVoice(result.tempFilePath);
  });
  uniRecorder.onError(() => {
    recording.value = false;
    uni.showToast({ title: '录音失败', icon: 'none' });
  });
  return uniRecorder;
}

function canUseBrowserRecorder(): boolean {
  return typeof navigator !== 'undefined'
    && Boolean(navigator.mediaDevices?.getUserMedia)
    && typeof MediaRecorder !== 'undefined';
}

function preferredMimeType(): string | undefined {
  const candidates = ['audio/webm;codecs=opus', 'audio/webm', 'audio/mp4', 'audio/ogg;codecs=opus'];
  return candidates.find((item) => MediaRecorder.isTypeSupported(item));
}

function stopBrowserTracks(): void {
  mediaStream?.getTracks().forEach((track) => track.stop());
  mediaStream = null;
  mediaRecorder = null;
}

async function toAsrWavBlob(blob: Blob): Promise<Blob> {
  const AudioContextCtor = window.AudioContext || (window as Window & { webkitAudioContext?: typeof AudioContext }).webkitAudioContext;
  if (!AudioContextCtor) {
    return blob;
  }
  let audioContext: AudioContext | null = null;
  try {
    audioContext = new AudioContextCtor();
    const decoded = await audioContext.decodeAudioData(await blob.arrayBuffer());
    const pcm = resampleToMono(decoded, 16000);
    return new Blob([encodeWav(pcm, 16000)], { type: 'audio/wav' });
  } catch {
    return blob;
  } finally {
    await audioContext?.close().catch(() => undefined);
  }
}

function resampleToMono(buffer: AudioBuffer, targetSampleRate: number): Float32Array {
  const sourceRate = buffer.sampleRate;
  const sourceLength = buffer.length;
  const mono = new Float32Array(sourceLength);
  for (let channel = 0; channel < buffer.numberOfChannels; channel += 1) {
    const data = buffer.getChannelData(channel);
    for (let i = 0; i < sourceLength; i += 1) {
      mono[i] += data[i] / buffer.numberOfChannels;
    }
  }
  if (sourceRate === targetSampleRate) {
    return mono;
  }
  const targetLength = Math.max(1, Math.round(sourceLength * targetSampleRate / sourceRate));
  const output = new Float32Array(targetLength);
  const ratio = sourceRate / targetSampleRate;
  for (let i = 0; i < targetLength; i += 1) {
    const sourceIndex = i * ratio;
    const left = Math.floor(sourceIndex);
    const right = Math.min(left + 1, sourceLength - 1);
    const fraction = sourceIndex - left;
    output[i] = mono[left] * (1 - fraction) + mono[right] * fraction;
  }
  return output;
}

function encodeWav(samples: Float32Array, sampleRate: number): ArrayBuffer {
  const bytesPerSample = 2;
  const dataLength = samples.length * bytesPerSample;
  const buffer = new ArrayBuffer(44 + dataLength);
  const view = new DataView(buffer);
  writeAscii(view, 0, 'RIFF');
  view.setUint32(4, 36 + dataLength, true);
  writeAscii(view, 8, 'WAVE');
  writeAscii(view, 12, 'fmt ');
  view.setUint32(16, 16, true);
  view.setUint16(20, 1, true);
  view.setUint16(22, 1, true);
  view.setUint32(24, sampleRate, true);
  view.setUint32(28, sampleRate * bytesPerSample, true);
  view.setUint16(32, bytesPerSample, true);
  view.setUint16(34, 16, true);
  writeAscii(view, 36, 'data');
  view.setUint32(40, dataLength, true);
  let offset = 44;
  for (const sample of samples) {
    const clamped = Math.max(-1, Math.min(1, sample));
    view.setInt16(offset, clamped < 0 ? clamped * 0x8000 : clamped * 0x7fff, true);
    offset += bytesPerSample;
  }
  return buffer;
}

function writeAscii(view: DataView, offset: number, value: string): void {
  for (let i = 0; i < value.length; i += 1) {
    view.setUint8(offset + i, value.charCodeAt(i));
  }
}

async function sendVoice(file: string | Blob): Promise<void> {
  const userMessageId = `v-${Date.now()}`;
  const aiMessageId = `a-${Date.now() + 1}`;
  voiceLoading.value = true;
  messages.value.push({ id: userMessageId, role: 'user', text: '正在识别语音…' });
  messages.value.push({ id: aiMessageId, role: 'ai', text: '正在思考…' });
  beginAnswer();
  try {
    const answer = await askVoiceStream(await ensureSession(), file, {
      ...streamHandlers(aiMessageId),
      onAsr: (text) => {
        updateMessage(userMessageId, text || '语音提问');
      },
      onError: (message) => {
        updateMessage(userMessageId, '语音识别不可用');
        uni.showToast({ title: message, icon: 'none' });
      }
    });
    if (answer.asrText) {
      updateMessage(userMessageId, answer.asrText);
    }
    finishAnswer(aiMessageId, answer);
  } catch (error: unknown) {
    removeMessage(aiMessageId);
    updateMessage(userMessageId, '语音提问失败');
    const message = error instanceof Error ? error.message : '语音问答失败';
    uni.showToast({ title: message, icon: 'none' });
  } finally {
    voiceLoading.value = false;
  }
}

function beginAnswer(): void {
  lastAnswer.value = {
    messageId: 0,
    answer: '',
    hitKb: 0,
    sources: [],
    emotion: 'NEUTRAL',
    streamUrl: null,
    audioUrl: null,
    costMs: 0
  };
}

function streamHandlers(messageId: string): ChatStreamHandlers {
  return {
    onMeta: (id) => patchAnswer({ messageId: id }),
    onDelta: (delta) => {
      const next = `${lastAnswer.value?.answer || ''}${delta}`;
      patchAnswer({ answer: next });
      updateMessage(messageId, next || '正在思考…');
    },
    onError: (message) => {
      patchAnswer({ answer: message });
      updateMessage(messageId, message);
    },
    onSources: (hitKb, sources) => patchAnswer({ hitKb, sources }),
    onEmotion: (emotion) => patchAnswer({ emotion }),
    onAudio: (audioUrl) => patchAnswer({ audioUrl }),
    onAvatar: (streamUrl) => patchAnswer({ streamUrl })
  };
}

function patchAnswer(patch: Partial<ChatAnswerVO>): void {
  lastAnswer.value = { ...(lastAnswer.value || emptyAnswer()), ...patch };
}

function finishAnswer(messageId: string, answer: ChatAnswerVO): void {
  lastAnswer.value = answer;
  updateMessage(messageId, answer.answer || '暂无回复');
}

function emptyAnswer(): ChatAnswerVO {
  return {
    messageId: 0,
    answer: '',
    hitKb: 0,
    sources: [],
    emotion: 'NEUTRAL',
    streamUrl: null,
    audioUrl: null,
    costMs: 0
  };
}

function historyQuestionText(item: MessageVO): string {
  if (item.inputType === 'VOICE') {
    return item.asrText || item.question || '语音提问';
  }
  if (item.inputType === 'IMAGE') {
    return item.question || '图片讲解';
  }
  return item.question || '';
}

function historyAnswer(item: MessageVO): ChatAnswerVO {
  return {
    messageId: item.messageId,
    answer: item.answer || '',
    hitKb: item.hitKb || 0,
    sources: parseSources(item.sources),
    emotion: item.emotion || 'NEUTRAL',
    streamUrl: item.streamUrl,
    audioUrl: item.audioUrl,
    costMs: item.costMs || 0
  };
}

function parseSources(raw: string | null): SourceVO[] {
  if (!raw) {
    return [];
  }
  try {
    const parsed = JSON.parse(raw) as unknown;
    return Array.isArray(parsed) ? parsed as SourceVO[] : [];
  } catch {
    return [];
  }
}

function updateMessage(id: string, text: string): void {
  const item = messages.value.find((message) => message.id === id);
  if (item) {
    item.text = text;
  }
}

function removeMessage(id: string): void {
  messages.value = messages.value.filter((message) => message.id !== id);
}
</script>

<style scoped lang="scss">
.planner-page {
  min-height: 100vh;
  padding: 20px 24px 48px;
  color: var(--text-main);
  background: linear-gradient(180deg, #f8f6f0 0%, #eef4ef 100%);
  box-sizing: border-box;
}

.planner-page :deep(.app-header) {
  width: min(100%, 1376px);
  margin: 0 auto;
}

.planner-shell {
  width: min(100%, 1280px);
  margin: 28px auto 0;
}

.planner-heading,
.panel-heading,
.planner-actions,
.panel-status,
.planner-checks,
.planner-checks view {
  display: flex;
  align-items: center;
}

.planner-heading {
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 22px;
}

.planner-kicker,
.planner-title,
.planner-subtitle,
.panel-title,
.panel-caption,
.chat-empty__title,
.chat-empty__desc,
.workspace-empty__title,
.workspace-empty__desc,
.map-placeholder__title,
.map-placeholder__desc {
  display: block;
}

.planner-kicker {
  color: var(--jade-700);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: .18em;
}

.planner-title {
  margin-top: 8px;
  color: var(--jade-900);
  font-family: Georgia, 'Songti SC', serif;
  font-size: clamp(26px, 3vw, 40px);
  font-weight: 800;
}

.planner-subtitle {
  margin-top: 8px;
  color: var(--text-secondary);
  font-size: 14px;
}

.planner-actions { gap: 10px; }
.planner-action {
  padding: 10px 14px;
  color: var(--jade-700);
  border: 1px solid rgba(40, 95, 82, .18);
  border-radius: 999px;
  font-size: 13px;
  cursor: pointer;
}
.planner-action--primary { color: var(--ivory); background: var(--jade-900); border-color: var(--jade-900); }

.planner-tabs { display: none; }

.agent-progress {
  display: grid;
  gap: 12px;
  margin-bottom: 16px;
  padding: 14px 18px;
  background: rgba(255, 255, 255, .64);
  border: 1px solid rgba(40, 95, 82, .10);
  border-radius: 16px;
}

.agent-progress__heading { display: flex; align-items: baseline; gap: 12px; min-width: 0; }
.agent-progress__title { color: var(--jade-900); font-size: 13px; font-weight: 800; white-space: nowrap; }
.agent-progress__summary { min-width: 0; overflow: hidden; color: var(--text-secondary); font-size: 12px; text-overflow: ellipsis; white-space: nowrap; }
.agent-progress__steps { display: flex; flex-wrap: wrap; gap: 10px 18px; }
.agent-step { display: inline-flex; align-items: center; gap: 6px; color: var(--text-secondary); font-size: 11px; }
.agent-step__dot { width: 7px; height: 7px; border-radius: 50%; background: #c7d7ce; }
.agent-step__dot--done { background: var(--jade-500); box-shadow: 0 0 0 3px rgba(94, 170, 142, .12); }
.agent-step__dot--error { background: #c87962; }
.agent-step__dot--pending { background: #d4b46b; }

.request-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin: 14px 0 2px;
}
.request-summary text { padding: 4px 8px; color: var(--jade-700); background: #edf5ef; border-radius: 999px; font-size: 11px; }
.request-summary .request-summary__label { color: var(--text-secondary); background: #f3f4ee; }

.planner-grid {
  display: grid;
  grid-template-columns: minmax(270px, .3fr) minmax(330px, .38fr) minmax(280px, .32fr);
  gap: 16px;
  align-items: stretch;
}

.planner-panel {
  display: flex;
  min-width: 0;
  min-height: 590px;
  flex-direction: column;
  padding: 22px;
  background: rgba(255, 255, 255, .78);
  border: 1px solid rgba(40, 95, 82, .12);
  border-radius: 22px;
  box-shadow: 0 18px 44px rgba(22, 56, 45, .08);
  box-sizing: border-box;
}

.panel-heading {
  justify-content: space-between;
  gap: 12px;
  flex: 0 0 auto;
}

.panel-heading__tools {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 8px;
}

.panel-inline-action {
  padding: 6px 9px;
  color: var(--jade-700);
  background: #edf5ef;
  border: 1px solid rgba(40, 95, 82, .12);
  border-radius: 8px;
  font-size: 11px;
  white-space: nowrap;
  cursor: pointer;
}

.panel-inline-action--quiet {
  color: var(--text-secondary);
  background: transparent;
}

.panel-title { color: var(--jade-900); font-size: 18px; font-weight: 800; }
.panel-caption { margin-top: 4px; color: var(--text-secondary); font-size: 12px; }
.panel-count { color: var(--jade-500); font-size: 11px; font-weight: 800; letter-spacing: .12em; }
.panel-status { gap: 6px; color: var(--jade-700); font-size: 12px; }
.status-dot { width: 7px; height: 7px; border-radius: 50%; background: #5eaa8e; box-shadow: 0 0 0 4px rgba(94, 170, 142, .12); }

.planner-avatar {
  flex: 0 0 auto;
  height: 180px !important;
  margin: 16px -4px 14px;
  border-radius: 16px !important;
  box-shadow: none !important;
}

.message-list {
  flex: 1 1 auto;
  min-height: 170px;
  height: 280px;
  padding: 4px 2px;
  box-sizing: border-box;
}

.chat-empty {
  display: grid;
  align-content: center;
  min-height: 170px;
  padding: 20px 10px;
  text-align: center;
}
.chat-empty__title { color: var(--jade-900); font-size: 18px; font-weight: 800; }
.chat-empty__desc { margin-top: 8px; color: var(--text-secondary); font-size: 13px; line-height: 1.6; }
.chat-empty__prompt { justify-self: center; margin-top: 16px; padding: 9px 12px; color: var(--jade-700); background: #edf5ef; border-radius: 10px; font-size: 12px; }

.source-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin: 8px 0;
}

.composer {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto auto;
  align-items: center;
  gap: 8px;
  padding-top: 10px;
  border-top: 1px solid rgba(40, 95, 82, .10);
}
.composer .input { min-width: 0; height: 40px; padding: 0 12px; background: #f6f8f4; border-radius: 10px; font-size: 13px; }
.composer .button { min-width: 48px; min-height: 40px; padding: 0 12px; border-radius: 10px; font-size: 13px; }

.workspace-empty,
.map-placeholder {
  position: relative;
  display: grid;
  place-items: center;
  align-content: center;
  flex: 1 1 auto;
  min-height: 360px;
  padding: 36px;
  text-align: center;
}
.workspace-empty__icon { display: grid; place-items: center; width: 48px; height: 48px; color: var(--jade-700); background: #e4f0e8; border-radius: 50%; font-size: 20px; }
.workspace-empty__title { margin-top: 16px; color: var(--jade-900); font-size: 20px; font-weight: 800; }
.workspace-empty__desc { max-width: 250px; margin-top: 8px; color: var(--text-secondary); font-size: 13px; line-height: 1.7; }

.itinerary-content {
  flex: 1 1 auto;
  min-height: 360px;
  padding: 22px 4px 6px;
}

.plan-overview { display: grid; gap: 12px; margin-bottom: 20px; padding: 12px; background: #f3f7f2; border-radius: 14px; }
.plan-overview__summary { color: var(--jade-700); font-size: 12px; line-height: 1.6; }
.plan-metrics { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; }
.plan-metrics view { display: grid; gap: 3px; min-width: 0; }
.plan-metrics view text:first-child { color: var(--text-secondary); font-size: 10px; }
.plan-metrics view text:last-child { overflow: hidden; color: var(--jade-900); font-size: 13px; font-weight: 800; text-overflow: ellipsis; white-space: nowrap; }

.route-summary {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
}

.route-summary__title { color: var(--jade-900); font-size: 20px; font-weight: 800; }
.route-summary__meta { color: var(--text-secondary); font-size: 12px; white-space: nowrap; }
.route-summary__reason { display: block; margin-top: 7px; color: var(--text-secondary); font-size: 13px; line-height: 1.6; }

.timeline {
  position: relative;
  display: grid;
  gap: 18px;
  margin-top: 28px;
}

.timeline::before {
  position: absolute;
  top: 12px;
  bottom: 12px;
  left: 26px;
  width: 1px;
  background: rgba(40, 95, 82, .18);
  content: '';
}

.timeline__item {
  position: relative;
  display: grid;
  grid-template-columns: 52px 1fr;
  align-items: center;
  gap: 12px;
}

.timeline__marker {
  position: relative;
  z-index: 1;
  display: grid;
  place-items: center;
   width: 52px;
  height: 36px;
  color: var(--jade-700);
  background: #edf5ef;
  border: 1px solid rgba(40, 95, 82, .12);
  border-radius: 50%;
   font-size: 10px;
  font-weight: 800;
}

.timeline__copy { display: grid; gap: 3px; }
.timeline__name { color: var(--jade-900); font-size: 14px; font-weight: 700; }
.timeline__hint { color: var(--text-secondary); font-size: 11px; }

.map-placeholder { overflow: hidden; border-radius: 16px; background: #eaf1eb; }
.map-placeholder__grid { position: absolute; inset: 0; opacity: .55; background-image: linear-gradient(rgba(40,95,82,.08) 1px, transparent 1px), linear-gradient(90deg, rgba(40,95,82,.08) 1px, transparent 1px); background-size: 34px 34px; transform: rotate(-8deg) scale(1.2); }
.map-placeholder__pin { position: relative; z-index: 1; display: grid; place-items: center; width: 54px; height: 54px; color: var(--ivory); background: var(--jade-700); border-radius: 50% 50% 50% 8px; transform: rotate(-45deg); font-size: 25px; }
.map-placeholder__title, .map-placeholder__desc { position: relative; z-index: 1; }
.map-placeholder__title { margin-top: 22px; color: var(--jade-900); font-size: 17px; font-weight: 800; }
.map-placeholder__desc { max-width: 260px; margin-top: 8px; color: var(--text-secondary); font-size: 13px; line-height: 1.7; }

.map-content {
  position: relative;
  display: flex;
  flex: 1 1 auto;
  min-height: 360px;
  flex-direction: column;
  gap: 14px;
  overflow: hidden;
  padding: 16px;
  background: #eaf1eb;
  border-radius: 16px;
}

.map-live {
  position: relative;
  display: flex;
  flex: 1 1 auto;
  min-height: 430px;
  overflow: hidden;
  border-radius: 16px;
}

.map-live__nearby {
  position: absolute;
  right: 12px;
  bottom: 56px;
  left: 12px;
  z-index: 3;
  max-height: 132px;
  overflow: auto;
}

.map-live__footer {
  position: absolute;
  right: 12px;
  bottom: 12px;
  left: 12px;
  z-index: 3;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 8px 10px;
  color: var(--text-secondary);
  background: rgba(255, 255, 255, .92);
  border-radius: 10px;
  font-size: 11px;
  box-shadow: 0 5px 18px rgba(25, 67, 56, .12);
}

.map-content__status,
.map-route-list,
.nearby-list {
  position: relative;
  z-index: 1;
}

.map-content__status {
  display: flex;
  align-items: center;
  gap: 7px;
  color: var(--jade-700);
  font-size: 12px;
}

.route-map {
  position: relative;
  z-index: 1;
  height: 156px;
  flex: 0 0 156px;
  overflow: hidden;
  border: 1px solid rgba(40, 95, 82, .12);
  border-radius: 14px;
  background: rgba(242, 248, 243, .58);
}

.route-map__segment {
  position: absolute;
  height: 2px;
  transform-origin: 0 50%;
  background: var(--jade-700);
  opacity: .62;
}

.route-map__point {
  position: absolute;
  display: grid;
  place-items: center;
  width: 24px;
  height: 24px;
  color: var(--ivory);
  background: var(--jade-900);
  border: 3px solid rgba(255, 255, 255, .86);
  border-radius: 50%;
  transform: translate(-50%, -50%);
  box-shadow: 0 3px 10px rgba(25, 67, 56, .22);
}

.route-map__point text { font-size: 10px; font-weight: 800; }
.route-map__caption { position: absolute; right: 10px; bottom: 8px; color: var(--text-secondary); font-size: 10px; }

.map-content__footer { position: relative; z-index: 1; display: flex; justify-content: flex-end; }

.map-route-list,
.nearby-list {
  display: grid;
  gap: 9px;
  padding: 13px;
  background: rgba(255, 255, 255, .72);
  border: 1px solid rgba(40, 95, 82, .10);
  border-radius: 12px;
}

.map-route-list__item,
.nearby-list__item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  color: var(--jade-900);
  font-size: 12px;
}

.map-route-list__index {
  display: grid;
  place-items: center;
  width: 20px;
  height: 20px;
  color: var(--ivory);
  background: var(--jade-700);
  border-radius: 50%;
  font-size: 10px;
}

.nearby-list__label { color: var(--jade-700); font-size: 11px; font-weight: 800; }
.nearby-list__item { color: var(--text-main); }
.nearby-list__item text:last-child { color: var(--text-secondary); }
.map-content > .map-placeholder__grid { position: absolute; z-index: 0; inset: 0; }

.planner-checks {
  justify-content: space-between;
  gap: 12px;
  margin-top: 14px;
  padding: 14px 18px;
  color: var(--text-secondary);
  background: rgba(255,255,255,.58);
  border: 1px solid rgba(40,95,82,.10);
  border-radius: 16px;
  font-size: 12px;
}
.planner-check { gap: 6px; min-width: 0; }
.planner-check > text:nth-child(2) { white-space: nowrap; }
.check-icon { display: inline-grid; place-items: center; width: 17px; height: 17px; color: var(--jade-500); background: #e4f0e8; border-radius: 50%; font-size: 12px; font-weight: 800; }
.check-icon--warn { color: #946f2e; background: #f6ecd5; }
.check-icon--pending { color: #8b958e; background: #edf0ec; }
.check-icon--fail { color: #a84c3e; background: #f6e2df; }
.check-status { color: var(--text-secondary); font-size: 10px; }
.check-detail { color: var(--text-secondary); font-size: 10px; line-height: 1.5; }
.agent-debug { margin-top: 10px; padding: 8px 10px; border: 1px dashed #b7c8bd; border-radius: 8px; color: #587064; font-size: 10px; line-height: 1.6; display: flex; flex-direction: column; }

@media (max-width: 980px) {
  .planner-page { padding-right: 16px; padding-left: 16px; }
  .planner-grid { grid-template-columns: minmax(250px, .34fr) minmax(310px, .66fr); }
  .planner-panel--map { grid-column: 1 / -1; min-height: 260px; }
  .planner-panel--map .map-placeholder { min-height: 220px; }
}

@media (max-width: 767px) {
  .planner-page { padding: 12px 12px 32px; }
  .planner-shell { margin-top: 20px; }
  .planner-heading { align-items: flex-start; flex-direction: column; gap: 14px; }
  .planner-title { font-size: 28px; }
  .planner-subtitle { max-width: 320px; line-height: 1.6; }
  .planner-actions { align-self: stretch; }
  .planner-action { flex: 1; text-align: center; }
  .planner-tabs { display: grid; grid-template-columns: repeat(3, 1fr); gap: 4px; margin-bottom: 12px; padding: 4px; background: #e6eee7; border-radius: 12px; }
  .planner-tab { padding: 9px; color: var(--text-secondary); text-align: center; border-radius: 9px; font-size: 13px; }
  .planner-tab--active { color: var(--ivory); background: var(--jade-900); }
  .planner-grid { display: block; }
  .planner-panel { min-height: 610px; padding: 16px; border-radius: 18px; }
  .planner-panel--mobile-hidden { display: none; }
  .planner-panel--map { min-height: 420px; }
  .planner-checks { display: grid; grid-template-columns: repeat(2, 1fr); gap: 10px; }
  .agent-progress { padding: 12px; }
  .agent-progress__heading { display: grid; gap: 4px; }
  .agent-progress__summary { white-space: normal; }
  .agent-progress__steps { gap: 8px 12px; }
  .planner-check { justify-content: flex-start; }
}

@media (prefers-reduced-motion: reduce) {
  .planner-page * { scroll-behavior: auto !important; }
}
</style>
