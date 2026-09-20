import type { NearbySpotVO, TouristHomeHotVO, TouristSpotDetailVO } from '../types';
import type { CityContextVO, CityVO } from '../types';
import { get } from '../utils/request';

export function getHotScenic(): Promise<TouristHomeHotVO> {
  return get<TouristHomeHotVO>('/api/tourist/scenic/hot');
}

export function listCities(): Promise<CityVO[]> { return get<CityVO[]>('/api/tourist/cities'); }
export function getCityContext(cityId: number): Promise<CityContextVO> {
  return get<CityContextVO>(`/api/tourist/cities/${cityId}/context`);
}
export async function resolveCityContext(cityName: string, cityCode?: string): Promise<CityContextVO> {
  const cityNameQuery = `cityName=${encodeURIComponent(cityName.trim())}`;
  const cityCodeQuery = cityCode ? `&cityCode=${encodeURIComponent(cityCode.trim())}` : '';
  const url = `/api/tourist/context/resolve?${cityNameQuery}${cityCodeQuery}`;
  console.info('[CitySearch] query=', cityName.trim());
  const result = await get<CityContextVO>(url);
  console.info('[CitySearch] result=', result);
  return result;
}

export function resolveCityContextByLocation(
  longitude: number,
  latitude: number,
  coordinateSystem: 'wgs84' | 'gcj02' = 'wgs84'
): Promise<CityContextVO> {
  return get<CityContextVO>('/api/tourist/context/resolve-location', {
    longitude,
    latitude,
    coordinateSystem
  });
}

export function getSpotDetail(id: number): Promise<TouristSpotDetailVO> {
  return get<TouristSpotDetailVO>(`/api/tourist/spot/${id}`);
}

export function getNearbySpots(
  scenicId: number,
  longitude: number,
  latitude: number
): Promise<NearbySpotVO[]> {
  return get<NearbySpotVO[]>('/api/tourist/spot/nearby', { scenicId, longitude, latitude });
}
