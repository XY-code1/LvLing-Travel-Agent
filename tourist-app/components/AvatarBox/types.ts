import type { AvatarConfigVO, AvatarRenderConfig } from '../../types';

export interface AvatarCanvasState {
  avatar: AvatarConfigVO | null;
  renderConfig: AvatarRenderConfig;
  speaking: boolean;
  text: string;
  progress: number;
  connected: boolean;
}
