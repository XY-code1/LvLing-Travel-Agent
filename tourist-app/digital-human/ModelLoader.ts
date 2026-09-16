import { GLTFLoader } from 'three/examples/jsm/loaders/GLTFLoader.js';
import type { Group } from 'three';

export interface ModelAudit { meshCount: number; triangleCount: number; boneCount: number; animations: string[]; morphTargets: string[]; textureCount: number; }

export function loadAvatar(url: string): Promise<{ scene: Group; animations: any[]; audit: ModelAudit }> {
  return new Promise((resolve, reject) => new GLTFLoader().load(url, (gltf) => {
    const audit: ModelAudit = { meshCount: 0, triangleCount: 0, boneCount: 0, animations: gltf.animations.map((a) => a.name), morphTargets: [], textureCount: 0 };
    gltf.scene.traverse((object: any) => {
      if (!object.isMesh) return;
      audit.meshCount += 1;
      audit.triangleCount += object.geometry?.index ? object.geometry.index.count / 3 : (object.geometry?.attributes?.position?.count || 0) / 3;
      if (object.skeleton) audit.boneCount += object.skeleton.bones.length;
      if (object.morphTargetDictionary) audit.morphTargets.push(...Object.keys(object.morphTargetDictionary));
      if (object.material?.map) audit.textureCount += 1;
    });
    console.info('[DigitalHuman3D] model audit', audit);
    resolve({ scene: gltf.scene, animations: gltf.animations, audit });
  }, undefined, reject));
}
