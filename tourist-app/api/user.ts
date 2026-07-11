import type { TouristProfileUpdateDTO, TouristProfileVO } from '../types';
import { get, put, upload } from '../utils/request';

export function getProfile(): Promise<TouristProfileVO> {
  return get<TouristProfileVO>('/api/tourist/user/profile');
}

export function updateProfile(data: TouristProfileUpdateDTO): Promise<TouristProfileVO> {
  return put<TouristProfileVO>('/api/tourist/user/profile', data);
}

export function uploadAvatar(filePath: string): Promise<TouristProfileVO> {
  return upload<TouristProfileVO>('/api/tourist/user/avatar', filePath, 'file');
}
