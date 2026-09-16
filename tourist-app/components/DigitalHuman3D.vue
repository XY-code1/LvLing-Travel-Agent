<template>
  <view class="digital-human-3d" :class="{ 'digital-human-3d--debug': debug }">
    <view ref="container" class="digital-human-3d__container"><canvas ref="canvas" class="digital-human-3d__canvas" type="webgl" /><view v-if="debug && errorText" class="digital-human-3d__error">{{ errorText }}</view><view v-if="debug && !errorText" class="digital-human-3d__loading">正在加载灵灵…</view></view>
    <view class="digital-human-3d__badge">{{ assetMode === 'lingling' ? '灵灵 · 在线' : '灵灵 · 预览' }}</view>
    <view v-if="debug" class="digital-human-3d__debug">
      <button @tap="play">播放测试语音</button><button @tap="stop">停止</button><button @tap="blink">眨眼</button><button @tap="smile">微笑</button><button @tap="idle">进入 Idle</button><button @tap="speaking">进入 Speaking</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue';
import * as THREE from 'three';
import { loadAvatar } from '../digital-human/ModelLoader';
import { AnimationController } from '../digital-human/AnimationController';
import { MorphTargetController } from '../digital-human/MorphTargetController';
import { LipSyncController } from '../digital-human/LipSyncController';
import { ExpressionController } from '../digital-human/ExpressionController';
import type { DigitalHumanState } from '../digital-human/digitalHumanState';

const debugFromUrl = typeof window !== 'undefined' && new URLSearchParams(window.location.search).get('debug3d') === '1';
const props = withDefaults(defineProps<{ modelUrl?: string; audioUrl?: string; debug?: boolean }>(), { modelUrl: '/static/digital-human/lingling.glb', audioUrl: '/static/audio/phase4f-test.wav', debug: debugFromUrl });
const emit = defineEmits<{ ready: []; error: [error: unknown]; state: [value: DigitalHumanState] }>();
const container = ref<HTMLElement>(); const canvas = ref<HTMLCanvasElement>(); const errorText = ref('');
const assetMode = ref<'lingling' | 'preview'>('lingling');
let renderer: THREE.WebGLRenderer; let scene: THREE.Scene; let camera: THREE.PerspectiveCamera; let avatar: THREE.Group; let animation: AnimationController; let morphs: MorphTargetController; let expression: ExpressionController; let lipSync: LipSyncController; let audio: HTMLAudioElement; let context: AudioContext; let frame = 0; let last = 0; let blinkTimer: ReturnType<typeof setTimeout>;

function setState(value: DigitalHumanState): void { emit('state', value); }
function blink(): void { expression?.blink(); }
function smile(): void { expression?.smile(); }
function idle(): void { setState('IDLE'); expression?.neutral(); }
function speaking(): void { setState('SPEAKING'); }
async function play(): Promise<void> { if (!audio) return; context ||= new AudioContext(); if (!lipSync) lipSync = new LipSyncController(audio, (value) => morphs?.set('jawOpen', value)); if (!lipSync.connected) lipSync.connect(context); await context.resume(); audio.currentTime = 0; await audio.play(); setState('SPEAKING'); }
function stop(): void { audio?.pause(); lipSync?.reset(); idle(); }
function resize(): void { if (!renderer) return; const target = (canvas.value?.tagName === 'CANVAS' ? canvas.value : container.value?.querySelector('canvas')) as HTMLCanvasElement | null; const width = target?.clientWidth || container.value?.clientWidth || 420; const height = target?.clientHeight || container.value?.clientHeight || 560; console.log('[DigitalHuman3D] size', width, height); renderer.setSize(width, height, false); camera.aspect = width / height; camera.updateProjectionMatrix(); }
function loop(time: number): void { const delta = Math.min((time - last) / 1000, 0.05); last = time; animation?.update(delta); lipSync?.update(); if (avatar) avatar.rotation.y = Math.sin(time / 2600) * 0.025; renderer?.render(scene, camera); frame = requestAnimationFrame(loop); }
function scheduleBlink(): void { blinkTimer = setTimeout(() => { blink(); scheduleBlink(); }, 3000 + Math.random() * 3000); }
async function init(): Promise<void> {
  const canvasElement = (canvas.value?.tagName === 'CANVAS' ? canvas.value : container.value?.querySelector('canvas')) as HTMLCanvasElement | null;
  console.log('[DigitalHuman3D] mounted', canvasElement); if (!canvasElement || typeof window === 'undefined') { errorText.value = 'Canvas 未找到'; return; }
  try {
    console.log('[DigitalHuman3D] container', container.value);
    renderer = new THREE.WebGLRenderer({ canvas: canvasElement, alpha: true, antialias: true }); renderer.setPixelRatio(Math.min(window.devicePixelRatio, 1.5));
    console.log('[DigitalHuman3D] renderer created', canvasElement.width, canvasElement.height);
    scene = new THREE.Scene(); camera = new THREE.PerspectiveCamera(28, 1, 0.1, 100); camera.position.set(0, 1.25, 3.2);
    scene.add(new THREE.HemisphereLight(0xfff8ed, 0x0b3d32, 2)); const key = new THREE.DirectionalLight(0xffffff, 2); key.position.set(2, 3, 3); scene.add(key);
    const cube = new THREE.Mesh(new THREE.BoxGeometry(.8, .8, .8), new THREE.MeshStandardMaterial({ color: 0x18a879 })); cube.position.z = 0; scene.add(cube);
    resize(); frame = requestAnimationFrame(loop); console.log('[DigitalHuman3D] loading model', props.modelUrl);
    let result; try { result = await loadAvatar(props.modelUrl); } catch (formalError) { if (props.modelUrl.endsWith('/lingling.glb')) { assetMode.value = 'preview'; result = await loadAvatar('/static/digital-human/test-avatar.glb'); } else throw formalError; }
    scene.remove(cube); cube.geometry.dispose(); (cube.material as THREE.Material).dispose(); avatar = result.scene;
    const box = new THREE.Box3().setFromObject(avatar); const size = box.getSize(new THREE.Vector3()); const center = box.getCenter(new THREE.Vector3()); avatar.position.sub(center); avatar.position.y -= size.y * .08; camera.position.z = Math.max(2.2, size.y * 1.8); camera.lookAt(0, 0, 0); scene.add(avatar); console.log('[DigitalHuman3D] model loaded'); animation = new AnimationController(avatar, result.animations); morphs = new MorphTargetController(avatar); expression = new ExpressionController(morphs); audio = new Audio(props.audioUrl); audio.addEventListener('ended', stop); scheduleBlink(); new ResizeObserver(resize).observe(canvasElement); errorText.value = ''; setState('IDLE'); emit('ready');
  } catch (error) { console.error('[DigitalHuman3D] model load failed', error); errorText.value = `模型加载失败：${error instanceof Error ? error.message : '请查看控制台'}`; setState('ERROR'); emit('error', error); }
}
onMounted(() => { void init(); });
onBeforeUnmount(() => { cancelAnimationFrame(frame); clearTimeout(blinkTimer); audio?.pause(); context?.close(); animation?.dispose(); avatar?.traverse((object: any) => { object.geometry?.dispose(); const material = object.material; (Array.isArray(material) ? material : [material]).forEach((item) => item?.dispose?.()); }); renderer?.dispose(); });
</script>

<style scoped lang="scss">
.digital-human-3d { position:relative; width:100%; height:100%; min-height:360px; overflow:hidden; background:transparent; }.digital-human-3d__container,.digital-human-3d__canvas { display:block; width:100%; height:100%; }.digital-human-3d__loading,.digital-human-3d__error { position:absolute; left:12px; bottom:90px; right:12px; padding:10px; border-radius:12px; background:rgba(0,0,0,.65); color:#fff; font-size:14px; text-align:center; }.digital-human-3d__error { background:rgba(120,0,0,.8); }.digital-human-3d__badge { position:absolute; top:8px; right:8px; padding:7px 12px; border-radius:20px; background:rgba(8,55,43,.7); color:#fff; font-size:14px; }.digital-human-3d__debug { position:absolute; bottom:8px; left:8px; right:8px; display:flex; flex-wrap:wrap; gap:5px; }.digital-human-3d__debug button { margin:0; padding:0 10px; height:30px; line-height:30px; font-size:12px; background:rgba(255,255,255,.85); }.digital-human-3d--debug { min-height:560px; border:3px solid red; background:rgba(0,0,0,.08); }
</style>
