export type DigitalHumanState = 'idle' | 'listening' | 'thinking' | 'speaking' | 'error';

export interface DigitalHumanRequest { text?: string; audioUrl?: string; cityId?: number | null; }
export interface DigitalHumanResult { audioUrl?: string; videoUrl?: string; subtitle?: string; }

export interface DigitalHumanProvider {
  readonly name: string;
  generate(request: DigitalHumanRequest): Promise<DigitalHumanResult>;
}
