export interface LinlyTalkerConfig {
  enabled?: boolean;
  baseUrl?: string;
  timeout?: number;
  avatarEngine?: string;
  ttsEngine?: string;
  voiceId?: string;
  avatarId?: string;
  outputDirectory?: string;
  streamEnabled?: boolean;
  streamUrl?: string;
}
