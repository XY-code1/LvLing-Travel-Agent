import { apiGet, apiPost } from './request';

import type { AdminInfoVO, AdminLoginDTO, AdminLoginVO, CaptchaVO } from '../types';

export function getAdminCaptcha(): Promise<CaptchaVO> { return apiGet<CaptchaVO>('/admin/captcha'); }
export function adminRegister(data: { username: string; password: string; realName?: string; captchaId: string; captchaCode: string }): Promise<null> { return apiPost<null>('/admin/register', data); }
export function forgotAdminPassword(data: { username: string; newPassword: string; resetToken: string; captchaId: string; captchaCode: string }): Promise<null> { return apiPost<null>('/admin/forgot-password', data); }

export function adminLogin(data: AdminLoginDTO): Promise<AdminLoginVO> {
  return apiPost<AdminLoginVO>('/admin/login', data);
}

export function adminLogout(): Promise<null> {
  return apiPost<null>('/admin/logout');
}

export function getAdminInfo(): Promise<AdminInfoVO> {
  return apiGet<AdminInfoVO>('/admin/info');
}
