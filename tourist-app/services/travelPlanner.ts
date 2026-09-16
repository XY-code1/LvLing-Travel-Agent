import { recommendRoute } from '../api/route';
import { geocodeAmap, getAmapWeather, searchAmapPoi } from './amapService';
import type {
  PlannerStep,
  RouteRecommendVO,
  TravelActivity,
  TravelPlan,
  TravelRequest
} from '../types';

const KNOWN_PLACES = [
  '西湖', '灵隐寺', '雷峰塔', '河坊街', '宽窄巷子', '兵马俑', '古城墙', '青岛'
];

const CHINESE_NUMBERS: Record<string, number> = {
  零: 0, 一: 1, 二: 2, 两: 2, 三: 3, 四: 4, 五: 5,
  六: 6, 七: 7, 八: 8, 九: 9, 十: 10
};

export interface TravelPlanResult {
  request: TravelRequest;
  routes: RouteRecommendVO[];
  plan: TravelPlan;
}

export function parseTravelRequest(rawText: string, fallbackDestination?: string): TravelRequest {
  const raw = rawText.trim();
  const destinationMatch = raw.match(/([\u4e00-\u9fa5]{2,8})(?=玩|旅行|旅游)/);
  const dateMatch = raw.match(/(\d{4}[-/.]\d{1,2}[-/.]\d{1,2}|\d{1,2}月\d{1,2}日)/);
  const daysMatch = raw.match(/([0-9一二两三四五六七八九十]+)\s*(?:天|日)/);
  const budgetMatch = raw.match(/预算\s*(?:¥|￥)?\s*([\d,]+)/i);
  const travelersMatch = raw.match(/([0-9一二两三四五六七八九十]+)\s*个?人/);

  const destination = destinationMatch?.[1] || fallbackDestination || null;
  const days = daysMatch ? parseNumber(daysMatch[1]) : null;
  const travelers = travelersMatch ? parseNumber(travelersMatch[1]) : null;
  const budget = budgetMatch ? Number(budgetMatch[1].replace(/,/g, '')) : null;
  const requiredPlaces = KNOWN_PLACES.filter((place) => raw.includes(place));
  const preferences = preferenceKeywords(raw);

  return {
    destination,
    startDate: dateMatch?.[1] || null,
    days,
    budget: Number.isFinite(budget) ? budget : null,
    travelers,
    preferences,
    requiredPlaces,
    mobilityPreference: mobilityPreference(raw),
    transportPreference: transportPreference(raw),
    rawText: raw
  };
}

export async function planTravel(
  scenicId: number,
  rawText: string,
  fallbackDestination?: string
): Promise<TravelPlanResult> {
  const request = parseTravelRequest(rawText, fallbackDestination);
  const routePreference = request.preferences.join('、')
    || request.requiredPlaces.join('、')
    || '经典轻松';
  const routes = await recommendRoute(scenicId, routePreference);
  const route = routes[0] || null;
  const spots = route?.spots || [];
  const localActivities = spots.map((spot, index) => activityFromSpot(spot, index, route?.estimateMinutes || null));
  const destination = request.destination || fallbackDestination || '';
  const [placeResolution, weatherResolution] = await Promise.all([
    resolveRequiredPlaces(request.requiredPlaces, destination),
    resolveWeather(destination)
  ]);
  const activities = placeResolution.activities.length === request.requiredPlaces.length && placeResolution.activities.length
    ? placeResolution.activities
    : localActivities;
  const routeMatchesDestination = !request.destination
    || !fallbackDestination
    || request.destination === fallbackDestination;
  const hasCoordinates = spots.length > 0 && spots.every((spot) =>
    typeof spot.longitude === 'number' && typeof spot.latitude === 'number'
  );
  const totalDistanceKm = hasCoordinates ? calculateDistanceKm(spots) : null;
  const travelers = request.travelers || 1;
  const days = request.days || 1;
  const transport = spots.length ? Math.max(30, spots.length * 15) : null;
  const dining = spots.length ? days * travelers * 120 : null;
  const estimatedTotal = transport !== null && dining !== null ? transport + dining : null;
  const mapCoordinatesReady = activities.length > 0 && activities.every((activity) =>
    Number.isFinite(activity.coordinates.longitude) && Number.isFinite(activity.coordinates.latitude));
  const routeCheck = route || placeResolution.activities.length
    ? routeMatchesDestination && mapCoordinatesReady
      ? check('PASS', '路线点位与坐标已准备完成。', 'route.recommend')
      : check('WARN', routeMatchesDestination
        ? '路线已生成，但部分点位缺少坐标。'
        : `当前展示的是${fallbackDestination || '当前城市'}已配置路线，目标城市资料待同步。`, 'route.recommend')
    : check('WARN', '当前城市暂无已配置的公开路线。', 'route.recommend');
  const budgetCheck = request.budget === null
    ? check('PENDING', '尚未提供预算，先显示交通与餐饮规则估算。', 'planner.rule')
    : estimatedTotal === null
      ? check('PENDING', '路线点位不足，暂无法完成预算估算。', 'planner.rule')
      : estimatedTotal <= request.budget
        ? check('PASS', `已估算约 ¥${estimatedTotal}，低于 ¥${request.budget} 预算。`, 'planner.rule')
        : check('WARN', `已估算约 ¥${estimatedTotal}，高于 ¥${request.budget} 预算，需调整。`, 'planner.rule');
  const steps = buildSteps(request, route, spots.length, routeMatchesDestination);
  const summary = route
    ? `${request.destination || fallbackDestination || '当前城市'} · ${request.days || 1}天计划已生成；${routeMatchesDestination ? '路线沿用当前城市资料。' : '目标城市 POI 尚未同步，当前先展示已配置路线。'}`
    : `${request.destination || fallbackDestination || '当前城市'} · 已识别需求，等待可用路线资料。`;

  return {
    request,
    routes,
    plan: {
      request,
      summary,
      budget: {
        budget: request.budget,
        estimatedTotal,
        transport,
        dining,
        tickets: null,
        totalDistanceKm,
        walkingDistanceKm: totalDistanceKm,
        note: '交通与餐饮为规则估算；门票、天气和开放时间尚未接入。'
      },
      checks: {
        weather: weatherResolution
          ? check('PASS', `${weatherResolution.city}当前${weatherResolution.weather}，${weatherResolution.temperature}℃。`, 'amap.weather')
          : check('PENDING', '高德天气尚未返回数据，请检查 Web Service Key。', 'amap.weather.pending'),
        openingHours: check('PENDING', '路线接口未返回景点开放时间。', 'scenic.openingHours.pending'),
        route: routeCheck,
        timeConflict: activities.length
          ? check('PASS', '当前时间点无重复安排。', 'planner.schedule')
          : check('PENDING', '没有足够点位可检查时间冲突。', 'planner.schedule'),
        budget: budgetCheck
      },
      days: activities.length ? [{ day: 1, title: '轻松游览日', activities }] : [],
      steps,
      routeId: route?.routeId || null
    }
  };
}

async function resolveRequiredPlaces(names: string[], city: string): Promise<{ activities: TravelActivity[] }> {
  if (!names.length || !city) return { activities: [] };
  const results = await Promise.all(names.map(async (name, index) => {
    try {
      const poi = (await searchAmapPoi(name, city))[0];
      const geocoded = poi ? null : await geocodeAmap(name, city);
      const coordinates = poi?.coordinates || geocoded?.coordinates;
      if (!coordinates) return null;
      return {
        time: ['09:00', '12:30', '15:00', '18:00'][index] || `${String(9 + index * 2).padStart(2, '0')}:00`,
        poi: { id: null, name: poi?.name || name },
        coordinates,
        duration: 90,
        transport: index === 0 ? '抵达' : '步行 / 公共交通',
        estimatedCost: null,
        reason: '地点与坐标来自高德 POI / 地理编码。'
      } satisfies TravelActivity;
    } catch (error) {
      console.warn('[AMap POI] unavailable', { name, error });
      return null;
    }
  }));
  return { activities: results.filter((item): item is TravelActivity => Boolean(item)) };
}

async function resolveWeather(city: string) {
  if (!city) return null;
  try {
    return await getAmapWeather(city);
  } catch (error) {
    console.warn('[AMap Weather] unavailable', error);
    return null;
  }
}

function activityFromSpot(
  spot: RouteRecommendVO['spots'][number],
  index: number,
  estimateMinutes: number | null
): TravelActivity {
  const times = ['09:00', '12:30', '15:00', '18:00'];
  const time = times[index] || `${String(9 + index * 2).padStart(2, '0')}:00`;
  const duration = estimateMinutes && spot.sortOrder > 0
    ? Math.max(30, Math.round(estimateMinutes / Math.max(spot.sortOrder, 1)))
    : null;
  return {
    time,
    poi: { id: spot.spotId, name: spot.name },
    coordinates: { longitude: spot.longitude, latitude: spot.latitude },
    duration,
    transport: index === 0 ? '抵达' : '步行 / 景区接驳',
    estimatedCost: null,
    reason: '来自当前城市已配置路线，未虚构景点数据。'
  };
}

function buildSteps(
  request: TravelRequest,
  route: RouteRecommendVO | null,
  spotCount: number,
  routeMatchesDestination: boolean
): PlannerStep[] {
  return [
    step('intent', '理解需求', 'DONE', request.rawText ? '已提取城市、天数、预算与偏好。' : '等待旅行需求输入。'),
    step('city', '查询城市', 'DONE', routeMatchesDestination ? '沿用当前 CityContext。' : '已识别目标城市，但当前上下文资料不同。'),
    step('poi', '查询景点', spotCount ? 'DONE' : 'PENDING', spotCount ? `路线返回 ${spotCount} 个真实点位。` : '当前城市暂无可用点位。'),
    step('weather', '检查天气', 'PENDING', '天气服务尚未接入。'),
    step('route', '规划路线', route ? 'DONE' : 'ERROR', route ? '复用现有路线推荐接口。' : '路线推荐接口未返回结果。'),
    step('schedule', '安排时间', route ? 'DONE' : 'PENDING', route ? '按点位顺序生成第一天时间轴。' : '等待路线点位。'),
    step('budget', '计算预算', spotCount ? 'DONE' : 'PENDING', spotCount ? '已完成交通与餐饮规则估算。' : '等待点位数据。'),
    step('conflict', '检查冲突', spotCount ? 'DONE' : 'PENDING', spotCount ? '当前时间轴无重复时间。' : '等待行程数据。'),
    step('complete', '完成', route ? 'DONE' : 'PENDING', route ? 'TravelPlan 已生成。' : '尚未完成。')
  ];
}

function step(id: string, label: string, status: PlannerStep['status'], detail: string): PlannerStep {
  return { id, label, status, detail };
}

function check(status: TravelPlan['checks']['weather']['status'], message: string, source: string) {
  return { status, message, source };
}

function parseNumber(value: string): number | null {
  if (/^\d+$/.test(value)) {
    return Number(value);
  }
  if (value === '十') {
    return 10;
  }
  if (value.startsWith('十')) {
    return 10 + (CHINESE_NUMBERS[value.slice(1)] || 0);
  }
  if (value.endsWith('十')) {
    return (CHINESE_NUMBERS[value[0]] || 0) * 10;
  }
  if (value.includes('十')) {
    const [tens, ones] = value.split('十');
    return (CHINESE_NUMBERS[tens] || 0) * 10 + (CHINESE_NUMBERS[ones] || 0);
  }
  return CHINESE_NUMBERS[value] ?? null;
}

function preferenceKeywords(raw: string): string[] {
  const candidates = [
    ['不想太累', '轻松'], ['轻松', '轻松'], ['休闲', '休闲'], ['美食', '美食'],
    ['历史', '历史文化'], ['文化', '历史文化'], ['亲子', '亲子'], ['拍照', '拍照']
  ];
  return candidates.filter(([needle]) => raw.includes(needle)).map(([, value]) => value)
    .filter((value, index, values) => values.indexOf(value) === index);
}

function mobilityPreference(raw: string): string | null {
  if (/(不想太累|少走路|适合老人|轻松)/.test(raw)) return '轻松少步行';
  if (/(徒步|步行优先)/.test(raw)) return '步行优先';
  return null;
}

function transportPreference(raw: string): string | null {
  if (raw.includes('地铁')) return '地铁';
  if (raw.includes('公交')) return '公交';
  if (raw.includes('打车')) return '打车';
  if (raw.includes('自驾')) return '自驾';
  return null;
}

function calculateDistanceKm(spots: RouteRecommendVO['spots']): number | null {
  if (spots.length < 2) return 0;
  let total = 0;
  for (let index = 1; index < spots.length; index += 1) {
    const previous = spots[index - 1];
    const current = spots[index];
    if (!previous || !current || previous.longitude === null || previous.latitude === null
      || current.longitude === null || current.latitude === null) return null;
    total += haversine(previous.latitude, previous.longitude, current.latitude, current.longitude);
  }
  return Number(total.toFixed(1));
}

function haversine(latitude1: number, longitude1: number, latitude2: number, longitude2: number): number {
  const radians = Math.PI / 180;
  const a = Math.sin((latitude2 - latitude1) * radians / 2) ** 2
    + Math.cos(latitude1 * radians) * Math.cos(latitude2 * radians)
    * Math.sin((longitude2 - longitude1) * radians / 2) ** 2;
  return 6371 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}
