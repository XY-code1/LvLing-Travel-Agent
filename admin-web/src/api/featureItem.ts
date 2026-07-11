import { apiDelete, apiGet, apiPost, apiPut } from './request';

import type { FeatureItemFormDTO, FeatureItemPageQuery, FeatureItemVO, PageResult, StatusDTO } from '../types';

export function pageFeatureItem(params: FeatureItemPageQuery): Promise<PageResult<FeatureItemVO>> {
  return apiGet<PageResult<FeatureItemVO>>('/admin/feature-item/page', params);
}

export function createFeatureItem(data: FeatureItemFormDTO): Promise<FeatureItemVO> {
  return apiPost<FeatureItemVO>('/admin/feature-item', data);
}

export function updateFeatureItem(data: FeatureItemFormDTO & { id: number }): Promise<FeatureItemVO> {
  return apiPut<FeatureItemVO>('/admin/feature-item', data);
}

export function deleteFeatureItem(id: number): Promise<null> {
  return apiDelete<null>(`/admin/feature-item/${id}`);
}

export function changeFeatureItemStatus(data: StatusDTO): Promise<null> {
  return apiPut<null>('/admin/feature-item/status', data);
}
