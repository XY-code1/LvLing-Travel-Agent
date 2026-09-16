export class ExpressionController {
  private blinkTimer?: ReturnType<typeof setTimeout>;
  constructor(private readonly setBlink: (value: number) => void) { this.schedule(); }
  blink(): void { this.setBlink(1); setTimeout(() => this.setBlink(0), 140); }
  private schedule(): void { this.blinkTimer = setTimeout(() => { this.blink(); this.schedule(); }, 3000 + Math.random() * 3000); }
  dispose(): void { if (this.blinkTimer) clearTimeout(this.blinkTimer); }
}

