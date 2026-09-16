import type { HeyGemConfig } from './HeyGemConfig';
import type { DigitalHumanRequest, DigitalHumanResult } from './types';

export class HeyGemClient {
  constructor(private readonly config: HeyGemConfig = {}) {}

  async generate(_request: DigitalHumanRequest): Promise<DigitalHumanResult> {
    void this.config;
    throw new Error('HeyGem 服务未连接');
    // TODO: 根据取得的 HeyGem 源码确认真实接口、参数和返回值后实现。
  }
}
