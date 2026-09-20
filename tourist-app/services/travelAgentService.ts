import { get, post } from '../utils/request';
import type { ApiResult } from '../types';

export interface AiRuntimeStatus {
  live: boolean;
  provider: string | null;
  model: string | null;
  baseUrlConfigured: boolean;
  apiKeyConfigured: boolean;
}

export function getAiRuntimeStatus(): Promise<AiRuntimeStatus> {
  return get('/api/tourist/agent/status');
}

export interface AgentCoordinate { longitude: number; latitude: number }
export interface AgentRouteData {
  origin: string;
  destination: string;
  city: string;
  routeMode: string;
  originCoordinates: AgentCoordinate;
  destinationCoordinates: AgentCoordinate;
  distanceMeters: number;
  durationSeconds: number;
  polyline: AgentCoordinate[];
  provider: string;
}
export interface AgentWeatherData {
  city: string;
  weather: string;
  temperature: string;
  humidity: string;
  windDirection: string;
  windPower: string;
  reportTime: string;
}
export interface AgentToolResult {
  taskId: number;
  toolName: string | null;
  status: 'SUCCESS' | 'FAILED' | 'SKIPPED';
  data: AgentRouteData | AgentWeatherData | null;
  errorCode: string | null;
  message: string | null;
}
export interface AgentTrace {
  taskId: number;
  step?: string;
  toolName: string | null;
  status: 'SUCCESS' | 'FAILED' | 'SKIPPED';
  durationMs: number;
  message?: string | null;
}
export interface AgentPlanResponse {
  context: { city: string | null; date: string | null; durationDays: number | null; budget: number | null; travelers: string | null };
  intent: { city: string | null; date: string | null; durationDays: number | null; budget: number | null;
    travelers: string | null; partyType: string; mobilityConstraint: boolean; preferences: string[];
    constraints: string[]; requestedPois: string[] };
  executionResults: AgentToolResult[];
  executionTrace: AgentTrace[];
  message: string;
  latencyMs: number;
  llmCalled: boolean;
  fallback: boolean;
  itinerary: Array<{ day: number; theme: string; items: Array<{ time: string; name: string; type: string; durationMinutes: number; reason: string; longitude?: number; latitude?: number; sequence: number }> }>;
  budget: { total: number | null; remaining: number | null };
}

export function runTravelAgent(message: string, city: string, location?: AgentCoordinate | null): Promise<AgentPlanResponse> {
  const payload = {
    message,
    city,
    longitude: location?.longitude,
    latitude: location?.latitude
  };
  if (typeof process !== 'undefined' && process.env?.NODE_ENV !== 'production') {
    console.info('[FrontendRequest]', { url: '/api/tourist/agent/plan', method: 'POST', payload });
  }
  return post<AgentPlanResponse | ApiResult<AgentPlanResponse>>('/api/tourist/agent/plan', payload).then((response) => {
    const wrapped = response as ApiResult<AgentPlanResponse>;
    const result = (wrapped && typeof wrapped === 'object' && 'data' in wrapped && typeof wrapped.code === 'number')
      ? wrapped.data
      : response as AgentPlanResponse;
    if (typeof process !== 'undefined' && process.env?.NODE_ENV !== 'production') {
      console.info('[AgentResponse]', { destination: result.intent?.city, days: result.intent?.durationDays, itineraryDays: result.itinerary?.length || 0, llmCalled: result.llmCalled, fallback: result.fallback, latencyMs: result.latencyMs });
    }
    return result;
  });
}
