import type { FeedbackSubmitDTO, RouteRecommendVO, TouristFeedbackVO } from '../types';
import { get, post } from '../utils/request';

export function recommendRoute(
  scenicId: number,
  interest?: string,
  routeType?: number
): Promise<RouteRecommendVO[]> {
  return get<RouteRecommendVO[]>('/api/tourist/route/recommend', { scenicId, interest, routeType });
}

export function submitFeedback(data: FeedbackSubmitDTO): Promise<null> {
  return post<null>('/api/tourist/feedback/submit', data);
}

export function listMyFeedback(): Promise<TouristFeedbackVO[]> {
  return get<TouristFeedbackVO[]>('/api/tourist/feedback/mine');
}
