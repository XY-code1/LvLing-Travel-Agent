export class LipSyncController {
  private analyser?: AnalyserNode;
  private data?: Uint8Array;
  private source?: MediaElementAudioSourceNode;
  private value = 0;
  constructor(private readonly audio: HTMLAudioElement, private readonly setMouth: (value: number) => void) {}
  connect(context: AudioContext): void {
    if (this.analyser) return;
    this.source = context.createMediaElementSource(this.audio);
    this.analyser = context.createAnalyser(); this.analyser.fftSize = 256;
    this.source.connect(this.analyser); this.analyser.connect(context.destination);
    this.data = new Uint8Array(this.analyser.frequencyBinCount);
  }
  update(): void {
    if (!this.analyser || !this.data) return;
    this.analyser.getByteTimeDomainData(this.data);
    let sum = 0; for (const sample of this.data) { const v = (sample - 128) / 128; sum += v * v; }
    const target = Math.min(1, Math.sqrt(sum / this.data.length) * 3.2);
    this.value += (target - this.value) * 0.22; this.setMouth(this.value);
  }
  reset(): void { this.value = 0; this.setMouth(0); }
}

