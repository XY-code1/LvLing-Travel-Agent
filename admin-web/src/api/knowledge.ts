import { apiDelete, apiGet, apiPost, apiPut } from './request';

import type {
  KnowledgeDocumentVO,
  KnowledgePageQuery,
  KnowledgeSyncVO,
  KnowledgeTestVO,
  KnowledgeUploadVO,
  PageResult,
  StatusDTO
} from '../types';

export function pageKnowledge(params: KnowledgePageQuery): Promise<PageResult<KnowledgeDocumentVO>> {
  return apiGet<PageResult<KnowledgeDocumentVO>>('/admin/knowledge/page', params);
}

export function uploadKnowledge(file: File, scenicId: number | null): Promise<KnowledgeUploadVO> {
  const data = new FormData();
  data.append('file', file);
  if (scenicId !== null) {
    data.append('scenicId', String(scenicId));
  }
  return apiPost<KnowledgeUploadVO>('/admin/knowledge/upload', data);
}

export function syncKnowledge(id: number): Promise<KnowledgeSyncVO> {
  return apiPost<KnowledgeSyncVO>(`/admin/knowledge/sync/${id}`);
}

export function deleteKnowledge(id: number): Promise<null> {
  return apiDelete<null>(`/admin/knowledge/${id}`);
}

export function changeKnowledgeStatus(data: StatusDTO): Promise<null> {
  return apiPut<null>('/admin/knowledge/status', data);
}

export function testKnowledge(question: string): Promise<KnowledgeTestVO> {
  return apiPost<KnowledgeTestVO>('/admin/knowledge/test', { question });
}
