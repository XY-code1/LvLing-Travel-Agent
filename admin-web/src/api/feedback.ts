import { apiGet, apiPut } from './request';

import type { AdminFeedbackPageQuery, AdminFeedbackVO, FeedbackReplyDTO, PageResult } from '../types';

export function pageFeedback(params: AdminFeedbackPageQuery): Promise<PageResult<AdminFeedbackVO>> {
  return apiGet<PageResult<AdminFeedbackVO>>('/admin/feedback/page', params);
}

export function replyFeedback(data: FeedbackReplyDTO): Promise<null> {
  return apiPut<null>('/admin/feedback/reply', data);
}
