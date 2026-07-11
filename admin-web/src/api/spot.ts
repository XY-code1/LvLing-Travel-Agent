import { apiDelete, apiGet, apiPost, apiPut } from './request';

import type { PageResult, SpotFormDTO, SpotPageQuery, SpotVO, StatusDTO } from '../types';

export function pageSpot(params: SpotPageQuery): Promise<PageResult<SpotVO>> {
  return apiGet<PageResult<SpotVO>>('/admin/spot/page', params);
}

export function createSpot(data: SpotFormDTO): Promise<SpotVO> {
  return apiPost<SpotVO>('/admin/spot', data);
}

export function updateSpot(data: SpotFormDTO & { id: number }): Promise<SpotVO> {
  return apiPut<SpotVO>('/admin/spot', data);
}

export function deleteSpot(id: number): Promise<null> {
  return apiDelete<null>(`/admin/spot/${id}`);
}

export function changeSpotStatus(data: StatusDTO): Promise<null> {
  return apiPut<null>('/admin/spot/status', data);
}
