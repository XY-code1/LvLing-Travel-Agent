import type { RegisterVO, TouristLoginDTO, TouristLoginVO, TouristRegisterDTO } from '../types';
import { post } from '../utils/request';

export function registerTourist(data: TouristRegisterDTO): Promise<RegisterVO> {
  return post<RegisterVO>('/api/tourist/auth/register', data);
}

export function loginTourist(data: TouristLoginDTO): Promise<TouristLoginVO> {
  return post<TouristLoginVO>('/api/tourist/auth/login', data);
}

export function logoutTourist(): Promise<null> {
  return post<null>('/api/tourist/auth/logout');
}
