import { apiGet, apiPut } from './request';

import type { PageResult, StatusDTO, TouristUserPageQuery, TouristUserVO } from '../types';

export function pageTouristUser(params: TouristUserPageQuery): Promise<PageResult<TouristUserVO>> {
  return apiGet<PageResult<TouristUserVO>>('/admin/user/page', params);
}

export function changeTouristUserStatus(data: StatusDTO): Promise<null> {
  return apiPut<null>('/admin/user/status', data);
}
