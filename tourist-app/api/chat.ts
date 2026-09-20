import type { ChatAnswerVO, MessageVO, SessionCreateVO, SessionVO } from '../types';
import { get, post, streamPost, streamUpload, upload } from '../utils/request';
import { type SseEvent } from '../utils/sse';

export interface ChatStreamHandlers {
  onAsr?: (text: string) => void;
  onMeta?: (messageId: number) => void;
  onDelta?: (text: string) => void;
  onError?: (message: string) => void;
  onSources?: (hitKb: number, sources: ChatAnswerVO['sources']) => void;
  onEmotion?: (emotion: string) => void;
  onAudio?: (audioUrl: string | null) => void;
  onAvatar?: (streamUrl: string | null) => void;
  onDone?: (answer: ChatAnswerVO) => void;
}

export function createSession(scenicId: number, avatarId?: number): Promise<SessionCreateVO> {
  return postResult<SessionCreateVO>('/api/tourist/session/create', { scenicId, avatarId });
}

export function getSessionHistory(): Promise<SessionVO[]> {
  return getResult<SessionVO[]>('/api/tourist/session/history');
}

export function getSessionMessages(sessionNo: string): Promise<MessageVO[]> {
  return getResult<MessageVO[]>(`/api/tourist/session/${sessionNo}/messages`);
}

export function askText(sessionNo: string, question: string, spotId?: number): Promise<ChatAnswerVO> {
  return postResult<ChatAnswerVO>('/api/tourist/chat/text', { sessionNo, question, spotId });
}

type ApiResult<T> = { code: number; msg?: string; data: T };

function unwrap<T>(response: T | ApiResult<T>): T {
  if (response && typeof response === 'object' && 'code' in (response as object)
    && 'data' in (response as object)) {
    const wrapped = response as ApiResult<T>;
    if (wrapped.code !== 200 && wrapped.code !== 0) {
      throw new Error(wrapped.msg || `API_${wrapped.code}`);
    }
    return wrapped.data;
  }
  return response as T;
}

async function getResult<T>(url: string): Promise<T> {
  return unwrap(await get<T | ApiResult<T>>(url));
}

async function postResult<T>(url: string, data?: unknown): Promise<T> {
  return unwrap(await post<T | ApiResult<T>>(url, data));
}

export async function askTextStream(
  sessionNo: string,
  question: string,
  spotId?: number,
  handlers?: ChatStreamHandlers
): Promise<ChatAnswerVO> {
  const accumulator = createAccumulator();
  await streamPost('/api/tourist/chat/text/stream', { sessionNo, question, spotId }, (event) => {
    applyEvent(accumulator, event, handlers);
  });
  const answer = toChatAnswer(accumulator.events);
  if (!answer.answer) throw new Error('灵灵没有收到回复，请稍后重试');
  handlers?.onDone?.(answer);
  return answer;
}

export function askVoice(sessionNo: string, filePath: string): Promise<ChatAnswerVO> {
  return upload<ChatAnswerVO>('/api/tourist/chat/voice', filePath, 'audio', { sessionNo });
}

export async function askVoiceStream(
  sessionNo: string,
  file: string | Blob,
  handlers?: ChatStreamHandlers
): Promise<ChatAnswerVO> {
  const accumulator = createAccumulator();
  await streamUpload('/api/tourist/chat/voice/stream', file, 'audio', { sessionNo }, (event) => {
    applyEvent(accumulator, event, handlers);
  });
  const answer = toChatAnswer(accumulator.events);
  if (!answer.answer) throw new Error('灵灵没有收到回复，请稍后重试');
  handlers?.onDone?.(answer);
  return answer;
}

function createAccumulator(): { events: SseEvent[] } {
  return { events: [] };
}

function applyEvent(
  accumulator: { events: SseEvent[] },
  event: SseEvent,
  handlers?: ChatStreamHandlers
): void {
  accumulator.events.push(event);
  const payload = parsePayload(event.data);
  if (event.event === 'asr') {
    handlers?.onAsr?.(stringField(payload, 'text', ''));
  } else if (event.event === 'meta') {
    handlers?.onMeta?.(numberField(payload, 'messageId', 0));
  } else if (event.event === 'delta') {
    handlers?.onDelta?.(stringField(payload, 'text', ''));
  } else if (event.event === 'error') {
    handlers?.onError?.(stringField(payload, 'message', 'AI 服务暂不可用，请检查后台配置。'));
  } else if (event.event === 'sources') {
    handlers?.onSources?.(numberField(payload, 'hit', 0), sourceArray(payload.sources));
  } else if (event.event === 'emotion') {
    handlers?.onEmotion?.(stringField(payload, 'emotion', 'NEUTRAL'));
  } else if (event.event === 'audio') {
    handlers?.onAudio?.(nullableString(payload.audioUrl));
  } else if (event.event === 'avatar') {
    handlers?.onAvatar?.(nullableString(payload.streamUrl));
  }
}

function toChatAnswer(events: Array<{ event: string; data: string }>): ChatAnswerVO {
  let messageId = 0;
  let answer = '';
  let hitKb = 0;
  let sources: ChatAnswerVO['sources'] = [];
  let emotion = 'NEUTRAL';
  let streamUrl: string | null = null;
  let audioUrl: string | null = null;
  let costMs = 0;
  let asrText: string | undefined;
  for (const item of events) {
    const payload = parsePayload(item.data);
    if (item.event === 'asr') {
      asrText = stringField(payload, 'text', '');
    } else if (item.event === 'meta') {
      messageId = numberField(payload, 'messageId', messageId);
    } else if (item.event === 'delta') {
      answer += stringField(payload, 'text', '');
    } else if (item.event === 'error') {
      answer = stringField(payload, 'message', 'AI 服务暂不可用，请检查后台配置。');
    } else if (item.event === 'sources') {
      hitKb = numberField(payload, 'hit', hitKb);
      sources = sourceArray(payload.sources);
    } else if (item.event === 'emotion') {
      emotion = stringField(payload, 'emotion', emotion);
    } else if (item.event === 'audio') {
      audioUrl = nullableString(payload.audioUrl);
    } else if (item.event === 'avatar') {
      streamUrl = nullableString(payload.streamUrl);
    } else if (item.event === 'done') {
      costMs = numberField(payload, 'costMs', costMs);
    }
  }
  return { messageId, answer, hitKb, sources, emotion, streamUrl, audioUrl, costMs, asrText };
}

function parsePayload(data: string): Record<string, unknown> {
  try {
    const parsed = JSON.parse(data) as unknown;
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? parsed as Record<string, unknown> : {};
  } catch {
    return {};
  }
}

function numberField(payload: Record<string, unknown>, key: string, fallback: number): number {
  return typeof payload[key] === 'number' ? payload[key] : fallback;
}

function stringField(payload: Record<string, unknown>, key: string, fallback: string): string {
  return typeof payload[key] === 'string' ? payload[key] : fallback;
}

function nullableString(value: unknown): string | null {
  return typeof value === 'string' && value ? value : null;
}

function sourceArray(value: unknown): ChatAnswerVO['sources'] {
  return Array.isArray(value) ? value as ChatAnswerVO['sources'] : [];
}
