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
