import { apiDelete, apiGet, apiPost, apiPut } from './request';

import type { PageResult, ScenicFormDTO, ScenicPageQuery, ScenicVO, StatusDTO } from '../types';

export function pageScenic(params: ScenicPageQuery): Promise<PageResult<ScenicVO>> {
  return apiGet<PageResult<ScenicVO>>('/admin/scenic/page', params);
}

export function createScenic(data: ScenicFormDTO): Promise<ScenicVO> {
  return apiPost<ScenicVO>('/admin/scenic', data);
}

export function updateScenic(data: ScenicFormDTO & { id: number }): Promise<ScenicVO> {
  return apiPut<ScenicVO>('/admin/scenic', data);
}

export function deleteScenic(id: number): Promise<null> {
  return apiDelete<null>(`/admin/scenic/${id}`);
}

export function changeScenicStatus(data: StatusDTO): Promise<null> {
  return apiPut<null>('/admin/scenic/status', data);
}
