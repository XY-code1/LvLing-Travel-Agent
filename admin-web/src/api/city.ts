import { apiDelete, apiGet, apiPost, apiPut } from './request';
import type { CityVO } from '../types';
export function pageCities(keyword?: string): Promise<CityVO[]> { return apiGet<CityVO[]>('/admin/city/page', { keyword }); }
export function createCity(data: Partial<CityVO>): Promise<CityVO> { return apiPost<CityVO>('/admin/city', data); }
export function updateCity(data: CityVO): Promise<CityVO> { return apiPut<CityVO>('/admin/city', data); }
export function disableCity(id: number): Promise<null> { return apiDelete<null>(`/admin/city/${id}`); }
