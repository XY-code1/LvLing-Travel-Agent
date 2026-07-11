import { apiDelete, apiGet, apiPost, apiPut } from './request';

import type { PageResult, RouteFormDTO, RoutePageQuery, RouteVO, StatusDTO } from '../types';

export function pageRoute(params: RoutePageQuery): Promise<PageResult<RouteVO>> {
  return apiGet<PageResult<RouteVO>>('/admin/route/page', params);
}

export function getRouteDetail(id: number): Promise<RouteVO> {
  return apiGet<RouteVO>(`/admin/route/${id}`);
}

export function createRoute(data: RouteFormDTO): Promise<RouteVO> {
  return apiPost<RouteVO>('/admin/route', data);
}

export function updateRoute(data: RouteFormDTO & { id: number }): Promise<RouteVO> {
  return apiPut<RouteVO>('/admin/route', data);
}

export function deleteRoute(id: number): Promise<null> {
  return apiDelete<null>(`/admin/route/${id}`);
}

export function changeRouteStatus(data: StatusDTO): Promise<null> {
  return apiPut<null>('/admin/route/status', data);
}

export function updateRouteSpots(routeId: number, spotIds: number[]): Promise<null> {
  return apiPut<null>('/admin/route/spots', { routeId, spotIds });
}
