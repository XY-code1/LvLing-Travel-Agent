import { resolveCityContextByLocation } from '../api/scenic';
import { useTouristStore } from '../stores';
import type { CityContextVO } from '../types';

declare const __DEFAULT_CITY_NAME__: string;
declare const __DEFAULT_CITY_CODE__: string;

let locationPending: Promise<CityContextVO> | null = null;
let autoLocationAttempted = false;
const DEFAULT_CITY_NAME = typeof __DEFAULT_CITY_NAME__ === 'string' && __DEFAULT_CITY_NAME__.trim()
  ? __DEFAULT_CITY_NAME__.trim() : '杭州';
const DEFAULT_CITY_ADCODE = typeof __DEFAULT_CITY_CODE__ === 'string' && __DEFAULT_CITY_CODE__.trim()
  ? __DEFAULT_CITY_CODE__.trim() : '330100';
const LOCATION_TIMEOUT_MS = 5000;
const FALLBACK_NOTICE = `未获取到精确位置，当前为你展示${DEFAULT_CITY_NAME}，可手动切换城市`;

/** Render from cache/fallback immediately; automatic geolocation runs once in the background. */
export function initializeCityContext(): Promise<CityContextVO> {
  const store = useTouristStore();
  if (store.cityContext?.city) {
    if (store.citySelectionSource === 'fallback') void autoLocateCurrentCity();
    return Promise.resolve(store.cityContext);
  }
  const fallback = activateDefaultCity();
  void autoLocateCurrentCity();
  return Promise.resolve(fallback);
}

export function initializeHomeCity(): Promise<CityContextVO> {
  return initializeCityContext();
}

export async function locateCurrentCity(): Promise<CityContextVO> {
  if (locationPending) return locationPending;
  locationPending = resolveCurrentLocation(false).finally(() => { locationPending = null; });
  return locationPending;
}

async function resolveCurrentLocation(automatic: boolean): Promise<CityContextVO> {
  const store = useTouristStore();
  const selectionAtStart = store.citySelectionSource;
  store.setLocationError('');
  store.setLocationStatus('locating');
  store.setCurrentLocation({ status: 'locating', source: 'browser' });
  try {
    const position = await browserPosition();
    store.setCurrentLocation({ coords: position, status: 'resolving', source: 'browser' });
    store.setLocationStatus('resolving');
    let context: CityContextVO;
    try {
      context = await resolveCityContextByLocation(position.longitude, position.latitude, 'wgs84');
    } catch (error) {
      console.warn('[Geolocation] reverse geocode failed; coordinates retained', error);
      store.setCurrentLocation({ cityName: null, status: 'partial' });
      store.setLocationStatus('success');
      throw markReverseGeocodeFailure(error);
    }
    store.setCurrentLocation({ cityName: context.city?.cityName || null, province: context.city?.province || null, status: 'success' });
    if (automatic && selectionAtStart !== 'manual' && store.citySelectionSource === 'manual') {
      return store.cityContext!;
    }
    if (store.citySelectionSource !== 'manual') store.setCityContext(context, 'location');
    store.setLocationStatus('success');
    return context;
  } catch (error: unknown) {
    store.setLocationStatus(locationErrorStatus(error));
    store.setLocationError(locationErrorMessage(error));
    if (isReverseGeocodeFailure(error)) return store.cityContext!;
    throw error instanceof Error ? error : new Error(locationErrorMessage(error));
  }
}

interface ReverseGeocodeError extends Error { reverseGeocodeFailed: true }

/** 保留反向地理编码失败的原始原因（后端错误码/网络错误），只在错误对象上打标记，不再用占位消息覆盖它。 */
function markReverseGeocodeFailure(error: unknown): ReverseGeocodeError {
  const marked = (error instanceof Error ? error : new Error('REVERSE_GEOCODE_FAILED')) as ReverseGeocodeError;
  marked.reverseGeocodeFailed = true;
  return marked;
}

function isReverseGeocodeFailure(error: unknown): boolean {
  return error instanceof Error && (error as Partial<ReverseGeocodeError>).reverseGeocodeFailed === true;
}

/** Automatic location never overwrites a manual selection and never blocks rendering. */
export async function autoLocateCurrentCity(): Promise<CityContextVO | null> {
  const store = useTouristStore();
  if (autoLocationAttempted || store.citySelectionSource === 'manual'
    || store.citySelectionSource === 'location') return store.cityContext;
  autoLocationAttempted = true;
  try {
    if (locationPending) return await locationPending;
    locationPending = resolveCurrentLocation(true).finally(() => { locationPending = null; });
    return await locationPending;
  } catch {
    if (!store.cityContext?.city) activateDefaultCity(true);
    else store.setLocationError(FALLBACK_NOTICE);
    console.info('[Location] fallback city');
    return store.cityContext;
  }
}

function activateDefaultCity(logFallback = false): CityContextVO {
  const store = useTouristStore();
  const context = defaultCityContext();
  store.setCityContext(context, 'fallback');
  store.setFeaturedCityContext(context);
  store.setLocationError(FALLBACK_NOTICE);
  if (logFallback) console.info('[Location] fallback city');
  return context;
}

function defaultCityContext(): CityContextVO {
  const isHangzhou = DEFAULT_CITY_ADCODE === '330100';
  return {
    city: {
      id: null, cityCode: DEFAULT_CITY_ADCODE, cityName: DEFAULT_CITY_NAME,
      province: isHangzhou ? '浙江省' : null, country: '中国', description: null, slogan: null,
      coverImage: null, heroImages: null, themeConfig: null,
      longitude: isHangzhou ? 120.1551 : null, latitude: isHangzhou ? 30.2741 : null,
      status: 1, sortOrder: 0, cityKey: `fallback:${DEFAULT_CITY_ADCODE}`,
      adcode: DEFAULT_CITY_ADCODE, source: 'fallback'
    },
    scenicAreas: [], pois: [], services: [], announcements: [], knowledgeSources: [],
    discovered: false, fallback: true, message: FALLBACK_NOTICE,
    source: 'fallback', cityKey: `fallback:${DEFAULT_CITY_ADCODE}`,
    adcode: DEFAULT_CITY_ADCODE, weather: null
  };
}

function browserPosition(): Promise<{ longitude: number; latitude: number; accuracy: number }> {
  if (typeof navigator === 'undefined' || !navigator.geolocation) {
    return Promise.reject(new Error('GEOLOCATION_UNSUPPORTED'));
  }
  console.info('[Location] requesting');
  return new Promise((resolve, reject) => navigator.geolocation.getCurrentPosition(
    ({ coords }) => {
      console.info('[Geolocation]', { status: 'success', latitude: coords.latitude, longitude: coords.longitude, accuracy: coords.accuracy });
      resolve({ longitude: coords.longitude, latitude: coords.latitude, accuracy: coords.accuracy });
    },
    (error) => {
      if (error.code === 1) console.info('[Location] denied');
      else if (error.code === 3) console.info('[Location] timeout');
      reject(new Error(error.code === 1 ? 'GEOLOCATION_DENIED'
        : error.code === 3 ? 'GEOLOCATION_TIMEOUT' : 'GEOLOCATION_UNAVAILABLE'));
    },
    { enableHighAccuracy: false, timeout: LOCATION_TIMEOUT_MS, maximumAge: 60000 }
  ));
}

function locationErrorStatus(error: unknown): 'permission-denied' | 'unavailable' | 'timeout' | 'backend-error' {
  const message = error instanceof Error ? error.message : '';
  if (message === 'GEOLOCATION_DENIED') return 'permission-denied';
  if (message === 'GEOLOCATION_TIMEOUT') return 'timeout';
  if (message === 'GEOLOCATION_UNAVAILABLE' || message === 'GEOLOCATION_UNSUPPORTED') return 'unavailable';
  return 'backend-error';
}

function locationErrorMessage(error: unknown): string {
  const message = error instanceof Error ? error.message : '';
  if (message === 'GEOLOCATION_DENIED') return '定位权限未开启';
  if (message === 'GEOLOCATION_TIMEOUT') return '定位超时';
  if (message === 'GEOLOCATION_UNSUPPORTED') return '当前浏览器不支持定位';
  if (message === 'GEOLOCATION_UNAVAILABLE') return '暂时无法获取设备位置';
  return resolveLocationErrorMessage(message);
}

/** 把后端或网络返回的真实原因翻译成可操作的提示，避免所有失败都折叠成一句无从排查的"位置解析服务暂时不可用"。 */
function resolveLocationErrorMessage(message: string): string {
  if (!message || message === 'REVERSE_GEOCODE_FAILED') return '位置解析服务暂时不可用，请稍后重试';
  if (message.includes('AMAP_WEB_SERVICE_KEY_NOT_CONFIGURED')) return '服务端未配置高德 Web 服务 Key，请联系管理员';
  if (message.includes('DAILY_QUERY_OVER_LIMIT')) return '位置解析服务今日调用额度已用尽，请稍后重试';
  if (message.includes('INVALID_USER_KEY') || message.includes('USERKEY_PLAT_NOMATCH')) return '高德 Key 无效或与服务类型不匹配，请联系管理员';
  if (message.includes('SERVICE_NOT_AVAILABLE')) return '高德未开通该位置服务，请联系管理员';
  if (message.includes('AMAP_API_ERROR') || message.includes('AMAP_API_REQUEST_FAILED')) return '位置解析服务暂时不可用，请稍后重试';
  if (message.includes('LOCATION_CITY_NOT_FOUND') || message.includes('CITY_NOT_FOUND')) return '未识别出当前位置所属城市，请手动选择城市';
  if (message.includes('LOCATION_COORDINATE')) return '定位坐标无效，请重新定位';
  if (message.startsWith('HTTP_5')) return '位置解析服务出错，请稍后重试';
  if (message.startsWith('HTTP_')) return `位置解析请求失败（${message}）`;
  if (message.startsWith('request:fail')) return '无法连接位置解析服务，请确认后端服务已启动后重试';
  return `位置解析服务暂时不可用（${message}）`;
}
