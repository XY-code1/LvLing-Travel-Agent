import type { AvatarConfigVO, AvatarRenderConfig } from '../../types';
import type { AvatarCanvasState } from './types';

const DEFAULT_RENDER_CONFIG: AvatarRenderConfig = {
  mouth: { x: 0.5, y: 0.13, width: 0.048, height: 0.018 },
  crop: { x: 0.5, y: 0.24, scale: 2.6 },
  breath: 0.012,
  blink: true
};

interface SourceRect {
  x: number;
  y: number;
  width: number;
  height: number;
}

export function parseRenderConfig(raw?: string | null): AvatarRenderConfig {
  if (!raw) {
    return DEFAULT_RENDER_CONFIG;
  }
  try {
    const parsed = JSON.parse(raw) as AvatarRenderConfig;
    return {
      mouth: { ...DEFAULT_RENDER_CONFIG.mouth, ...parsed.mouth },
      crop: { ...DEFAULT_RENDER_CONFIG.crop, ...parsed.crop },
      breath: parsed.breath ?? DEFAULT_RENDER_CONFIG.breath,
      blink: parsed.blink ?? DEFAULT_RENDER_CONFIG.blink
    };
  } catch {
    return DEFAULT_RENDER_CONFIG;
  }
}

export class AvatarCanvasRenderer {
  private canvas: HTMLCanvasElement | null = null;
  private ctx: CanvasRenderingContext2D | null = null;
  private avatarImage: HTMLImageElement | null = null;
  private imageToken = 0;
  private frameId = 0;
  private width = 320;
  private height = 220;
  private state: AvatarCanvasState = {
    avatar: null,
    renderConfig: DEFAULT_RENDER_CONFIG,
    speaking: false,
    text: '',
    progress: 0,
    connected: false
  };

  mount(canvas: HTMLCanvasElement): void {
    this.canvas = canvas;
    this.ctx = canvas.getContext('2d');
    this.resize();
    window.addEventListener('resize', this.resize);
    this.start();
  }

  destroy(): void {
    window.removeEventListener('resize', this.resize);
    if (this.frameId) {
      window.cancelAnimationFrame(this.frameId);
      this.frameId = 0;
    }
  }

  setState(nextState: Partial<AvatarCanvasState>): void {
    const previousImage = this.state.avatar?.avatarImage || '';
    this.state = { ...this.state, ...nextState };
    const nextImage = this.state.avatar?.avatarImage || '';
    if (previousImage !== nextImage) {
      void this.loadImage(nextImage);
    }
  }

  private resize = (): void => {
    if (!this.canvas || !this.ctx) {
      return;
    }
    const rect = this.canvas.getBoundingClientRect();
    const nextWidth = Math.max(280, Math.round(rect.width || 320));
    const nextHeight = Math.max(180, Math.round(rect.height || 220));
    const dpr = Math.max(1, window.devicePixelRatio || 1);
    this.width = nextWidth;
    this.height = nextHeight;
    this.canvas.width = Math.round(nextWidth * dpr);
    this.canvas.height = Math.round(nextHeight * dpr);
    this.ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
  };

  private start(): void {
    const tick = (timestamp: number) => {
      this.draw(timestamp);
      this.frameId = window.requestAnimationFrame(tick);
    };
    this.frameId = window.requestAnimationFrame(tick);
  }

  private async loadImage(src: string): Promise<void> {
    const token = ++this.imageToken;
    if (!src) {
      this.avatarImage = null;
      return;
    }
    const img = new Image();
    img.crossOrigin = 'anonymous';
    img.decoding = 'async';
    img.src = src;
    await new Promise<void>((resolve, reject) => {
      img.onload = () => resolve();
      img.onerror = () => reject(new Error('avatar image load failed'));
    }).catch(() => undefined);
    if (token === this.imageToken && img.complete && img.naturalWidth > 0) {
      this.avatarImage = img;
    }
  }

  private draw(timestamp: number): void {
    if (!this.ctx) {
      return;
    }
    this.drawBackdrop();
    if (!this.avatarImage) {
      this.drawFallback();
      return;
    }
    const rect = this.sourceRect(this.avatarImage);
    const breathPx = Math.sin(timestamp / 1400) * this.height * (this.state.renderConfig.breath ?? 0);
    this.ctx.save();
    this.ctx.shadowColor = 'rgba(15, 23, 42, 0.18)';
    this.ctx.shadowBlur = 18;
    this.ctx.shadowOffsetY = 8;
    this.ctx.drawImage(
      this.avatarImage,
      rect.x,
      rect.y,
      rect.width,
      rect.height,
      0,
      breathPx,
      this.width,
      this.height
    );
    this.ctx.restore();
    this.drawMouth(timestamp, rect, breathPx);
    this.drawVignette();
  }

  private drawBackdrop(): void {
    if (!this.ctx) {
      return;
    }
    const gradient = this.ctx.createLinearGradient(0, 0, this.width, this.height);
    gradient.addColorStop(0, '#edf7f3');
    gradient.addColorStop(0.55, '#f7faf8');
    gradient.addColorStop(1, '#e3ecf5');
    this.ctx.fillStyle = gradient;
    this.ctx.fillRect(0, 0, this.width, this.height);
    this.ctx.fillStyle = 'rgba(33, 87, 74, 0.08)';
    this.ctx.beginPath();
    this.ctx.ellipse(this.width * 0.72, this.height * 0.18, this.width * 0.28, this.height * 0.18, 0, 0, Math.PI * 2);
    this.ctx.fill();
    this.ctx.fillStyle = 'rgba(54, 83, 115, 0.08)';
    this.ctx.beginPath();
    this.ctx.ellipse(this.width * 0.22, this.height * 0.88, this.width * 0.36, this.height * 0.18, 0, 0, Math.PI * 2);
    this.ctx.fill();
  }

  private drawFallback(): void {
    if (!this.ctx) {
      return;
    }
    const cx = this.width * 0.5;
    const cy = this.height * 0.45;
    this.ctx.fillStyle = 'rgba(30, 70, 62, 0.16)';
    this.ctx.beginPath();
    this.ctx.arc(cx, cy - 24, 44, 0, Math.PI * 2);
    this.ctx.fill();
    this.ctx.beginPath();
    roundedRect(this.ctx, cx - 74, cy + 22, 148, 96, 34);
    this.ctx.fill();
  }

  private sourceRect(img: HTMLImageElement): SourceRect {
    const crop = this.state.renderConfig.crop || DEFAULT_RENDER_CONFIG.crop;
    const scale = clamp(crop.scale ?? 2.6, 1.2, 4);
    const aspect = this.width / this.height;
    let sourceHeight = img.height / scale;
    let sourceWidth = sourceHeight * aspect;
    if (sourceWidth > img.width) {
      sourceWidth = img.width;
      sourceHeight = sourceWidth / aspect;
    }
    const centerX = (crop.x ?? 0.5) * img.width;
    const centerY = (crop.y ?? 0.24) * img.height;
    const x = clamp(centerX - sourceWidth / 2, 0, img.width - sourceWidth);
    const y = clamp(centerY - sourceHeight / 2, 0, img.height - sourceHeight);
    return { x, y, width: sourceWidth, height: sourceHeight };
  }

  private drawMouth(timestamp: number, rect: SourceRect, offsetY: number): void {
    if (!this.ctx || !this.avatarImage) {
      return;
    }
    const mouth = this.state.renderConfig.mouth || DEFAULT_RENDER_CONFIG.mouth;
    const imageX = (mouth.x ?? 0.5) * this.avatarImage.width;
    const imageY = (mouth.y ?? 0.13) * this.avatarImage.height;
    const imageW = (mouth.width ?? 0.048) * this.avatarImage.width;
    const imageH = (mouth.height ?? 0.018) * this.avatarImage.height;
    const x = ((imageX - rect.x) / rect.width) * this.width;
    const y = ((imageY - rect.y) / rect.height) * this.height + offsetY;
    const w = Math.max(8, (imageW / rect.width) * this.width);
    const h = Math.max(4, (imageH / rect.height) * this.height);
    if (x < -w || x > this.width + w || y < -h || y > this.height + h) {
      return;
    }

    const open = this.speechOpen(timestamp);
    this.ctx.save();
    this.ctx.lineCap = 'round';
    this.ctx.lineJoin = 'round';
    this.ctx.translate(x, y);

    this.ctx.fillStyle = `rgba(52, 13, 22, ${this.state.speaking ? 0.72 : 0.42})`;
    this.ctx.beginPath();
    this.ctx.ellipse(0, h * 0.2, w * 0.42, h * (0.18 + open * 1.2), 0, 0, Math.PI * 2);
    this.ctx.fill();

    this.ctx.strokeStyle = 'rgba(118, 35, 49, 0.82)';
    this.ctx.lineWidth = Math.max(1.2, h * 0.18);
    this.ctx.beginPath();
    this.ctx.moveTo(-w * 0.45, 0);
    this.ctx.quadraticCurveTo(0, h * (0.22 + open * 0.32), w * 0.45, 0);
    this.ctx.stroke();

    if (this.state.speaking && open > 0.36) {
      this.ctx.fillStyle = 'rgba(255, 246, 235, 0.9)';
      this.ctx.beginPath();
      this.ctx.ellipse(0, -h * 0.04, w * 0.27, h * 0.12, 0, 0, Math.PI * 2);
      this.ctx.fill();
    }
    this.ctx.restore();
  }

  private speechOpen(timestamp: number): number {
    if (!this.state.speaking) {
      return 0.04;
    }
    const text = this.state.text || '';
    const index = text.length > 0 ? Math.min(text.length - 1, Math.floor(this.state.progress * text.length)) : 0;
    const charEnergy = text.length > 0 ? (text.charCodeAt(index) % 11) / 22 : 0.18;
    const fast = (Math.sin(timestamp / 64) + 1) / 2;
    const slow = (Math.sin(timestamp / 145 + charEnergy * 8) + 1) / 2;
    return clamp(0.12 + fast * 0.42 + slow * 0.22 + charEnergy, 0.08, 0.92);
  }

  private drawVignette(): void {
    if (!this.ctx) {
      return;
    }
    const gradient = this.ctx.createRadialGradient(
      this.width * 0.5,
      this.height * 0.42,
      this.height * 0.1,
      this.width * 0.5,
      this.height * 0.5,
      this.width * 0.72
    );
    gradient.addColorStop(0, 'rgba(255,255,255,0)');
    gradient.addColorStop(1, 'rgba(15,23,42,0.12)');
    this.ctx.fillStyle = gradient;
    this.ctx.fillRect(0, 0, this.width, this.height);
  }
}

function clamp(value: number, min: number, max: number): number {
  return Math.min(max, Math.max(min, value));
}

function roundedRect(ctx: CanvasRenderingContext2D, x: number, y: number, width: number, height: number, radius: number): void {
  const r = Math.min(radius, width / 2, height / 2);
  ctx.moveTo(x + r, y);
  ctx.lineTo(x + width - r, y);
  ctx.quadraticCurveTo(x + width, y, x + width, y + r);
  ctx.lineTo(x + width, y + height - r);
  ctx.quadraticCurveTo(x + width, y + height, x + width - r, y + height);
  ctx.lineTo(x + r, y + height);
  ctx.quadraticCurveTo(x, y + height, x, y + height - r);
  ctx.lineTo(x, y + r);
  ctx.quadraticCurveTo(x, y, x + r, y);
}
