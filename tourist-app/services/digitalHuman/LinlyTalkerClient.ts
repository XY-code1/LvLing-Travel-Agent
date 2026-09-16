import type { LinlyTalkerConfig } from './LinlyTalkerConfig';
import type { DigitalHumanRequest, DigitalHumanResult } from './types';

export class LinlyTalkerClient {
  constructor(private readonly config: LinlyTalkerConfig = {}) {}

  async healthCheck(): Promise<boolean> {
    // The browser must not call Linly-Talker directly. Backend adapter health
    // and endpoint mapping are intentionally left explicit until that adapter
    // is implemented and secured.
    void this.config.baseUrl;
    return false;
    // TODO: call the backend DigitalHuman endpoint, not Python port 8001/8003.
  }

  async synthesizeSpeech(_text: string): Promise<{ audioUrl?: string }> {
    throw new Error('Linly-Talker TTS adapter 未连接');
  }

  async generateAvatarVideo(_request: DigitalHumanRequest): Promise<DigitalHumanResult> {
    throw new Error('Linly-Talker Talker adapter 未连接');
  }

  async generateDigitalHumanReply(request: DigitalHumanRequest): Promise<DigitalHumanResult> {
    return this.generateAvatarVideo(request);
  }

  async getTaskStatus(_taskId: string): Promise<unknown> {
    throw new Error('Linly-Talker 当前源码未提供任务状态 API');
  }
}
