import { LinlyTalkerClient } from './LinlyTalkerClient';
import type { DigitalHumanProvider, DigitalHumanRequest, DigitalHumanResult } from './types';

export class LinlyTalkerDigitalHumanProvider implements DigitalHumanProvider {
  readonly name = 'Linly-Talker';
  constructor(private readonly client = new LinlyTalkerClient()) {}
  generate(request: DigitalHumanRequest): Promise<DigitalHumanResult> {
    return this.client.generateDigitalHumanReply(request);
  }
}
