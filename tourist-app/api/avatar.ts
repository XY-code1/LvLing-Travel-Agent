import type { AvatarConfigVO } from '../types';
import { get, put } from '../utils/request';

export function getDefaultAvatar(): Promise<AvatarConfigVO | null> {
  return get<AvatarConfigVO | null>('/api/tourist/avatar/default');
}

export function listAvatars(): Promise<AvatarConfigVO[]> {
  return get<AvatarConfigVO[]>('/api/tourist/avatar/list');
}

export function getCurrentAvatar(): Promise<AvatarConfigVO | null> {
  return get<AvatarConfigVO | null>('/api/tourist/avatar/current');
}

export function selectAvatar(avatarId: number): Promise<AvatarConfigVO> {
  return put<AvatarConfigVO>('/api/tourist/avatar/current', { avatarId });
}
