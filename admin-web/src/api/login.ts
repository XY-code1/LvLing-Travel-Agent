import { apiGet, apiPost } from './request';

import type { AdminInfoVO, AdminLoginDTO, AdminLoginVO } from '../types';

export function adminLogin(data: AdminLoginDTO): Promise<AdminLoginVO> {
  return apiPost<AdminLoginVO>('/admin/login', data);
}

export function adminLogout(): Promise<null> {
  return apiPost<null>('/admin/logout');
}

export function getAdminInfo(): Promise<AdminInfoVO> {
  return apiGet<AdminInfoVO>('/admin/info');
}
