import type { NearbySpotVO, TouristHomeHotVO, TouristSpotDetailVO } from '../types';
import { get } from '../utils/request';

export function getHotScenic(): Promise<TouristHomeHotVO> {
  return get<TouristHomeHotVO>('/api/tourist/scenic/hot');
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
