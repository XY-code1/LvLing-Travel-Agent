import type { Object3D } from 'three';

export class MorphTargetController {
  private targets: Array<{ dictionary: Record<string, number>; influences: number[] }> = [];
  constructor(root: Object3D) {
    root.traverse((object: any) => { if (object.morphTargetDictionary && object.morphTargetInfluences) this.targets.push(object); });
  }
  set(alias: string, value: number): void {
    const aliases: Record<string, RegExp> = { blink: /blink|eye.?close/i, jawOpen: /jaw.?open|mouth.?open|viseme.?aa/i, mouthOpen: /mouth.?open|jaw.?open/i, smile: /smile|happy/i };
    const pattern = aliases[alias] || new RegExp(alias, 'i');
    this.targets.forEach((target) => Object.entries(target.dictionary).forEach(([name, index]) => { if (pattern.test(name)) target.influences[index] = Math.max(0, Math.min(1, value)); }));
  }
  names(): string[] { return this.targets.flatMap((target) => Object.keys(target.dictionary)); }
}
