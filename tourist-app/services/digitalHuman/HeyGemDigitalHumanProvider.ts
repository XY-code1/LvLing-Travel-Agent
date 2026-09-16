import { HeyGemClient } from './HeyGemClient';
import type { DigitalHumanProvider, DigitalHumanRequest, DigitalHumanResult } from './types';

export class HeyGemDigitalHumanProvider implements DigitalHumanProvider {
  readonly name = 'HeyGem';
  constructor(private readonly client = new HeyGemClient()) {}
  generate(request: DigitalHumanRequest): Promise<DigitalHumanResult> { return this.client.generate(request); }
}
