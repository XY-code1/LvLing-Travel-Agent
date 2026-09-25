import { defineStore } from 'pinia';
import { computed, ref } from 'vue';

import type { AvatarConfigVO, CityContextVO, CityVO, CurrentLocationState, TouristInfoVO, TouristProfileVO, TravelTaskContext } from '../types';
import { clearAuth, getStoredUser, getToken, setStoredUser, setToken } from '../utils/auth';
import { resolveCityContext } from '../api/scenic';

export type LocationStatus = 'idle' | 'locating' | 'resolving' | 'success'
  | 'permission-denied' | 'unavailable' | 'timeout' | 'backend-error';
export type CitySelectionSource = 'location' | 'manual' | 'fallback' | 'restored';

export const useTouristStore = defineStore('tourist', () => {
  const persistedCityContext = readPersistedCityContext();
  const persistedSelectionSource = readCitySelectionSource();
  const token = ref(getToken());
  const touristInfo = ref<TouristInfoVO | null>(getStoredUser());
  const profile = ref<TouristProfileVO | null>(null);
  const selectedAvatar = ref<AvatarConfigVO | null>(null);
  const defaultAvatar = selectedAvatar;
  const avatarOptions = ref<AvatarConfigVO[]>([]);
  const persistedSession = readPersistedSession();
  const currentSessionNo = ref(persistedSession.sessionNo);
  const currentSessionAvatarId = ref<number | null>(persistedSession.avatarId);
  const currentScenicId = ref(persistedCityContext?.scenicAreas?.length
    ? persistedCityContext.scenicAreas[0].id
    : persistedSession.scenicId);
  const currentCityId = ref<number | null>(persistedCityContext?.city?.id
    ?? (Number(uni.getStorageSync('guido_city_id')) || null));
  const currentCity = ref<CityVO | null>(persistedCityContext?.city || null);
  const cityContext = ref<CityContextVO | null>(persistedCityContext);
  const recentCities = ref<CityVO[]>(readRecentCities());
  const citySelectionSource = ref<CitySelectionSource>(persistedSelectionSource || (persistedCityContext ? 'restored' : 'fallback'));
  const locationError = ref('');
  const locationStatus = ref<LocationStatus>('idle');
  const currentLocation = ref<CurrentLocationState>({ coords: null, cityName: null, province: null, district: null, status: 'idle', source: 'none' });
  const featuredCityContext = ref<CityContextVO | null>(null);
  const travelTaskContext = ref<TravelTaskContext | null>(readTravelTaskContext());
  const experienceCityContext = computed(() => travelTaskContext.value?.destinationCityContext
    || cityContext.value || featuredCityContext.value);

  function setLogin(nextToken: string, user: TouristInfoVO): void {
    token.value = nextToken;
    touristInfo.value = user;
    setToken(nextToken);
    setStoredUser(user);
  }

  function setProfile(nextProfile: TouristProfileVO): void {
    profile.value = nextProfile;
  }

  function setSession(sessionNo: string, scenicId: number, avatarId?: number | null): void {
    currentSessionNo.value = sessionNo;
    currentScenicId.value = scenicId;
    currentSessionAvatarId.value = avatarId ?? null;
    uni.setStorageSync(SESSION_STORAGE_KEY, JSON.stringify({ sessionNo, scenicId, avatarId: avatarId ?? null }));
  }
  function setCityContext(context: CityContextVO, selectionSource: CitySelectionSource = 'manual'): void {
    if (!context?.city?.cityName || !(context.cityKey || context.city.cityKey || context.adcode || context.city.adcode)) {
      throw new Error('城市上下文无效');
    }
    const previous = cityContext.value;
    context.source ||= context.fallback ? 'fallback' : context.discovered ? 'discovered' : 'local';
    cityContext.value = context; currentCity.value = context.city;
    citySelectionSource.value = selectionSource;
    currentCityId.value = context.city?.id ?? null;
    uni.setStorageSync('guido_city_context', JSON.stringify(context));
    uni.setStorageSync('guido_city_selection_source', selectionSource);
    if (selectionSource === 'location') uni.setStorageSync('guido_location_resolved_at', Date.now());
    if (currentCityId.value) uni.setStorageSync('guido_city_id', currentCityId.value);
    else uni.removeStorageSync('guido_city_id');
    currentScenicId.value = context.scenicAreas.length ? context.scenicAreas[0].id : 0;
    if (previous?.cityKey && previous.cityKey !== context.cityKey) clearSession();
    if (context.city) {
      recentCities.value = [context.city, ...recentCities.value.filter((city) => city.cityKey !== context.city?.cityKey
        && city.cityCode !== context.city?.cityCode)].slice(0, 5);
      uni.setStorageSync('guido_recent_cities', JSON.stringify(recentCities.value));
    }
    console.info('[CityContext]', { previous, next: context, selectionSource });
  }

  function setLocationError(message: string): void { locationError.value = message; }
  function setLocationStatus(status: LocationStatus): void { locationStatus.value = status; }
  function setCurrentLocation(value: Partial<CurrentLocationState>): void { currentLocation.value = { ...currentLocation.value, ...value }; }
  function setFeaturedCityContext(context: CityContextVO): void { featuredCityContext.value = context; }
  function startTravelTask(rawRequest: string): TravelTaskContext {
    const task: TravelTaskContext = {
      taskId: `task-${Date.now()}`,
      rawRequest,
      currentCity: currentCity.value,
      destinationCity: null,
      destinationCityContext: null,
      date: null,
      duration: null,
      travelers: null,
      mobility: null,
      budget: null,
      interests: [],
      mustVisit: [],
      selectedPois: [],
      route: null,
      weather: null,
      services: [],
      validation: null,
      executionTrace: [],
      updatedAt: new Date().toISOString()
    };
    setTravelTaskContext(task);
    return task;
  }
  function setTravelTaskContext(task: TravelTaskContext): void {
    travelTaskContext.value = task;
    uni.setStorageSync('guido_travel_task_context', JSON.stringify(task));
  }

  function setAvatarOptions(options: AvatarConfigVO[]): void {
    avatarOptions.value = options;
  }

  function setSelectedAvatar(avatar: AvatarConfigVO | null): void {
    selectedAvatar.value = avatar;
  }

  function setDefaultAvatar(avatar: AvatarConfigVO | null): void {
    setSelectedAvatar(avatar);
  }

  function clearSession(): void {
    currentSessionNo.value = '';
    currentSessionAvatarId.value = null;
    uni.removeStorageSync(SESSION_STORAGE_KEY);
  }

  async function ensureScenicId(): Promise<void> {
    if (currentScenicId.value > 0) return;
    const cityName = currentCity.value?.cityName || cityContext.value?.city?.cityName;
    if (!cityName) return;
    try {
      const context = await resolveCityContext(cityName);
      if (context?.scenicAreas?.length) {
        setCityContext(context, 'manual');
      }
    } catch (error) {
      // 解析失败时保持现有 scenicId，对话仍可用通用知识降级回答。
      console.warn('[Scenic] resolve scenicId failed; keep as-is', error);
    }
  }

  function logoutLocal(): void {
    token.value = '';
    touristInfo.value = null;
    profile.value = null;
    selectedAvatar.value = null;
    avatarOptions.value = [];
    clearSession();
    clearAuth();
  }

  return {
    token,
    touristInfo,
    profile,
    defaultAvatar,
    selectedAvatar,
    avatarOptions,
    currentSessionNo,
    currentSessionAvatarId,
    currentScenicId,
    currentCityId, currentCity, cityContext, featuredCityContext, experienceCityContext,
    recentCities, locationError, locationStatus, currentLocation, citySelectionSource, travelTaskContext,
    setCityContext, setFeaturedCityContext, setLocationError, setLocationStatus, setCurrentLocation,
    startTravelTask, setTravelTaskContext,
    setLogin,
    setProfile,
    setSession,
    setAvatarOptions,
    setSelectedAvatar,
    setDefaultAvatar,
    clearSession,
    ensureScenicId,
    logoutLocal
  };
});

function readPersistedCityContext(): CityContextVO | null {
  const value = uni.getStorageSync('guido_city_context');
  if (!value) return null;
  try {
    const context = typeof value === 'string' ? JSON.parse(value) : value;
    return context?.city?.cityName ? context as CityContextVO : null;
  } catch {
    uni.removeStorageSync('guido_city_context');
    return null;
  }
}

function readCitySelectionSource(): CitySelectionSource | null {
  const value = uni.getStorageSync('guido_city_selection_source');
  return value === 'location' || value === 'manual' || value === 'fallback' ? value : null;
}

function readRecentCities(): CityVO[] {
  const value = uni.getStorageSync('guido_recent_cities');
  if (!value) return [];
  try {
    const cities = typeof value === 'string' ? JSON.parse(value) : value;
    return Array.isArray(cities) ? cities.filter((city) => city?.cityName).slice(0, 5) : [];
  } catch {
    uni.removeStorageSync('guido_recent_cities');
    return [];
  }
}

function readTravelTaskContext(): TravelTaskContext | null {
  const value = uni.getStorageSync('guido_travel_task_context');
  if (!value) return null;
  try {
    const task = typeof value === 'string' ? JSON.parse(value) : value;
    return task?.taskId && task?.rawRequest ? task as TravelTaskContext : null;
  } catch {
    uni.removeStorageSync('guido_travel_task_context');
    return null;
  }
}

const SESSION_STORAGE_KEY = 'guido_tourist_session';

function readPersistedSession(): { sessionNo: string; scenicId: number; avatarId: number | null } {
  const value = uni.getStorageSync(SESSION_STORAGE_KEY);
  if (!value) return { sessionNo: '', scenicId: 0, avatarId: null };
  try {
    const parsed = typeof value === 'string' ? JSON.parse(value) : value;
    return {
      sessionNo: typeof parsed?.sessionNo === 'string' ? parsed.sessionNo : '',
      scenicId: typeof parsed?.scenicId === 'number' ? parsed.scenicId : 0,
      avatarId: typeof parsed?.avatarId === 'number' ? parsed.avatarId : null
    };
  } catch {
    uni.removeStorageSync(SESSION_STORAGE_KEY);
    return { sessionNo: '', scenicId: 0, avatarId: null };
  }
}
