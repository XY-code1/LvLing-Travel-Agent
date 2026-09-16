import type { DigitalHumanProvider, DigitalHumanRequest, DigitalHumanResult } from './types';

export class MockDigitalHumanProvider implements DigitalHumanProvider {
  readonly name = 'Preview Mode';
  async generate(request: DigitalHumanRequest): Promise<DigitalHumanResult> {
    return { subtitle: request.text || '数字人服务未连接，当前为 Preview Mode' };
  }
}
