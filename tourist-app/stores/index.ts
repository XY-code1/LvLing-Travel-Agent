import { defineStore } from 'pinia';
import { ref } from 'vue';

import type { AvatarConfigVO, CityContextVO, CityVO, TouristInfoVO, TouristProfileVO } from '../types';
import { clearAuth, getStoredUser, getToken, setStoredUser, setToken } from '../utils/auth';

export const useTouristStore = defineStore('tourist', () => {
  const token = ref(getToken());
  const touristInfo = ref<TouristInfoVO | null>(getStoredUser());
  const profile = ref<TouristProfileVO | null>(null);
  const selectedAvatar = ref<AvatarConfigVO | null>(null);
  const defaultAvatar = selectedAvatar;
  const avatarOptions = ref<AvatarConfigVO[]>([]);
  const currentSessionNo = ref('');
  const currentSessionAvatarId = ref<number | null>(null);
  const currentScenicId = ref(1);
  const currentCityId = ref<number | null>(Number(uni.getStorageSync('guido_city_id')) || null);
  const currentCity = ref<CityVO | null>(null);
  const cityContext = ref<CityContextVO | null>(null);

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
  }
  function setCityContext(context: CityContextVO): void {
    cityContext.value = context; currentCity.value = context.city;
    currentCityId.value = context.city?.id ?? null;
    if (currentCityId.value) uni.setStorageSync('guido_city_id', currentCityId.value);
    if (context.scenicAreas.length) currentScenicId.value = context.scenicAreas[0].id;
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
    currentCityId, currentCity, cityContext, setCityContext,
    setLogin,
    setProfile,
    setSession,
    setAvatarOptions,
    setSelectedAvatar,
    setDefaultAvatar,
    clearSession,
    logoutLocal
  };
});
