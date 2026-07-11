import { apiDelete, apiGet, apiPost, apiPut } from './request';

import type { AiConfigFormDTO, AiConfigTestDTO, AiConfigTestVO, AiConfigVO } from '../types';

export function listAiConfig(serviceType?: string): Promise<AiConfigVO[]> {
  return apiGet<AiConfigVO[]>('/admin/ai-config/list', { serviceType });
}

export function createAiConfig(data: AiConfigFormDTO): Promise<AiConfigVO> {
  return apiPost<AiConfigVO>('/admin/ai-config', data);
}

export function updateAiConfig(data: AiConfigFormDTO & { id: number }): Promise<AiConfigVO> {
  return apiPut<AiConfigVO>('/admin/ai-config', data);
}

export function deleteAiConfig(id: number): Promise<null> {
  return apiDelete<null>(`/admin/ai-config/${id}`);
}

export function enableAiConfig(id: number): Promise<null> {
  return apiPut<null>('/admin/ai-config/enable', { id });
}

export function testAiConfig(id: number, data: AiConfigTestDTO): Promise<AiConfigTestVO> {
  return apiPost<AiConfigTestVO>(`/admin/ai-config/test/${id}`, data);
}

export function testVisionAiConfig(id: number, file: File, prompt: string): Promise<AiConfigTestVO> {
  const data = new FormData();
  data.append('image', file);
  if (prompt.trim()) {
    data.append('prompt', prompt.trim());
  }
  return apiPost<AiConfigTestVO>(`/admin/ai-config/test/${id}/vision`, data);
}
