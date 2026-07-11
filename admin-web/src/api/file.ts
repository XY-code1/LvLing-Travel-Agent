import { apiPost } from './request';

import type { FileUploadVO } from '../types';

export function uploadImage(file: File): Promise<FileUploadVO> {
  const data = new FormData();
  data.append('file', file);
  return apiPost<FileUploadVO>('/admin/files/image', data);
}
