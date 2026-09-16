import { post } from '../utils/request';

export interface TtsRequest { text: string; voiceProfileId: string; voiceId: string; rate: number; pitch: number; }
export interface TtsResult { audioUrl: string; rawResponse?: string; }

export function synthesize(request: TtsRequest): Promise<TtsResult> {
  const speechRate = Math.round((request.rate - 1) * 100);
  const pitchRate = Math.round((request.pitch - 1) * 100);
  console.info('[TTS Frontend Request]', {
    text: request.text,
    voiceProfileId: request.voiceProfileId,
    voiceId: request.voiceId,
    rate: request.rate,
    pitch: request.pitch
  });
  return post<TtsResult>('/api/tourist/tts/synthesize', {
    text: request.text,
    voiceId: request.voiceId,
    speechRate,
    pitchRate
  });
}
