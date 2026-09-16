import { AnimationMixer, LoopRepeat, type Object3D, type AnimationClip } from 'three';

export class AnimationController {
  readonly mixer: AnimationMixer;
  private idle?: any;
  constructor(root: Object3D, clips: AnimationClip[]) {
    this.mixer = new AnimationMixer(root);
    const idle = clips.find((clip) => /idle/i.test(clip.name));
    if (idle) this.idle = this.mixer.clipAction(idle).setLoop(LoopRepeat, Infinity).play();
  }
  update(delta: number): void { this.mixer.update(delta); }
  dispose(): void { this.mixer.stopAllAction(); this.mixer.uncacheRoot(this.mixer.getRoot()); }
}
