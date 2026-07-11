import { apiGet, apiPut } from './request';

import type { AdminChatMessageVO, AdminChatPageQuery, PageResult } from '../types';

export function pageChat(params: AdminChatPageQuery): Promise<PageResult<AdminChatMessageVO>> {
  return apiGet<PageResult<AdminChatMessageVO>>('/admin/chat/page', params);
}

export function getChatDetail(id: number): Promise<AdminChatMessageVO> {
  return apiGet<AdminChatMessageVO>(`/admin/chat/${id}`);
}

export function markChatSupplement(messageId: number): Promise<null> {
  return apiPut<null>('/admin/chat/mark', { messageId });
}
