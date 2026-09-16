export class LipSyncController {
  private analyser?: AnalyserNode;
  private data?: Uint8Array;
  constructor(private readonly audio: HTMLAudioElement, private readonly setMouth: (value: number) => void) {}
  get connected(): boolean { return Boolean(this.analyser); }
  connect(context: AudioContext): void { const source = context.createMediaElementSource(this.audio); this.analyser = context.createAnalyser(); this.analyser.fftSize = 256; source.connect(this.analyser); this.analyser.connect(context.destination); this.data = new Uint8Array(this.analyser.frequencyBinCount); }
  update(): void { if (!this.analyser || !this.data) return; this.analyser.getByteFrequencyData(this.data); const average = this.data.reduce((sum, value) => sum + value, 0) / this.data.length / 255; this.setMouth(Math.min(1, average * 2.8)); }
  reset(): void { this.setMouth(0); }
}
