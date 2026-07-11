import { apiGet, apiPost, request } from './request';

import type { SentimentReportVO } from '../types';

export function generateSentiment(date: string): Promise<SentimentReportVO> {
  return apiPost<SentimentReportVO>('/admin/sentiment/generate', { date });
}

export function getSentimentReport(date: string): Promise<SentimentReportVO> {
  return apiGet<SentimentReportVO>('/admin/sentiment/report', { date });
}

export async function downloadSentimentDocx(date: string): Promise<Blob> {
  const response = await request.post<Blob>(
    '/admin/sentiment/generate-docx',
    { date },
    { responseType: 'blob' }
  );
  return response.data;
}
