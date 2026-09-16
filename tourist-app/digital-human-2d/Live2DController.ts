/** Live2D adapter boundary. Cubism can be added without changing Agent/TTS callers. */
export interface Live2DController {
  setParameter(name: 'ParamMouthOpenY' | 'ParamEyeLOpen' | 'ParamEyeROpen' | 'ParamAngleX' | 'ParamAngleY' | 'ParamBodyAngleX', value: number): void;
  destroy(): void;
}

