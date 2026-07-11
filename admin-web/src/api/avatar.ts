import { apiGet, apiPost, apiPut } from './request';

import type { AvatarConfigVO, AvatarTestVO } from '../types';

export function listAvatar(): Promise<AvatarConfigVO[]> {
  return apiGet<AvatarConfigVO[]>('/admin/avatar/list');
}

export function enableAvatar(id: number, enabled: number): Promise<null> {
  return apiPut<null>('/admin/avatar/enable', { id, enabled });
}

export function testAvatar(id: number, text: string): Promise<AvatarTestVO> {
  return apiPost<AvatarTestVO>('/admin/avatar/test', { id, text });
}
