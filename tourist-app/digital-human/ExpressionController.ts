import type { MorphTargetController } from './MorphTargetController';
export class ExpressionController {
  constructor(private readonly morphs: MorphTargetController) {}
  blink(): void { this.morphs.set('blink', 1); setTimeout(() => this.morphs.set('blink', 0), 140); }
  smile(value = 0.7): void { this.morphs.set('smile', value); }
  neutral(): void { this.morphs.set('smile', 0); this.morphs.set('blink', 0); }
}
