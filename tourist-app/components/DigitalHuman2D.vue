<template>
  <view class="digital-human-2d" :class="[`digital-human-2d--${state.toLowerCase()}`, { 'digital-human-2d--missing': missing }]" @tap="playAudio">
    <view class="digital-human-2d__glow" />
    <!-- H5 uses a native img here so the static asset path is directly testable. -->
    <img v-if="!missing" :key="currentOutfit.id" class="digital-human-2d__image" :class="[`digital-human-2d__image--${currentOutfit.id}`, { 'digital-human-2d__image--switching': switching }]" :src="currentOutfit.image" alt="灵灵，旅灵 AI 旅行数字员工" @load="handleLoaded" @error="handleMissing" />
    <view v-else class="digital-human-2d__placeholder"><text>灵灵</text><text>正式人物形象待接入</text></view>
    <view class="digital-human-2d__status">灵灵 · {{ state === 'SPEAKING' ? '正在陪你规划' : '在线' }}</view>
    <view class="digital-human-2d__audio-hint" @tap.stop="playAudio">让灵灵说话</view>
    <view class="digital-human-2d__settings" @tap.stop="settingsOpen = !settingsOpen">设置</view>
    <view v-if="settingsOpen" class="digital-human-2d__panel" @tap.stop>
      <text class="digital-human-2d__panel-title">灵灵装扮</text>
      <view v-for="outfit in outfits" :key="outfit.id" class="digital-human-2d__option" @tap="selectOutfit(outfit)">{{ outfit.name }}</view>
      <text class="digital-human-2d__panel-title">声音</text>
      <view v-for="voice in voices" :key="voice.id" class="digital-human-2d__option" @tap="selectVoice(voice)">{{ voice.name }}</view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue';
import { ExpressionController } from '../digital-human-2d/ExpressionController';
import { LipSyncController } from '../digital-human-2d/LipSyncController';
import type { DigitalHumanState } from '../digital-human-2d/DigitalHumanState';
import { defaultLinglingOutfit, linglingOutfits, type LinglingOutfit } from '../digital-human-2d/LinglingOutfit';
import { linglingVoiceProfiles, type VoiceProfile } from '../digital-human-2d/VoiceProfile';
import { synthesize } from '../services/ttsService';

const props = withDefaults(defineProps<{ audioUrl?: string | null; state?: DigitalHumanState }>(), { audioUrl: '/static/audio/phase4f-test.wav', state: 'IDLE' });
const emit = defineEmits<{ state: [value: DigitalHumanState]; ready: []; error: [error: unknown] }>();
const state = ref<DigitalHumanState>(props.state); const missing = ref(false); const imageLoaded = ref(false); const switching = ref(false); const audioBlocked = ref(false); const mouth = ref(0); const blink = ref(0); const settingsOpen = ref(false); const currentOutfit = ref<LinglingOutfit>(defaultLinglingOutfit); const currentVoice = ref<VoiceProfile>(linglingVoiceProfiles[0]);
const outfits = linglingOutfits; const voices = linglingVoiceProfiles;
let audio: HTMLAudioElement | undefined; let context: AudioContext | undefined; let lipSync: LipSyncController | undefined; let expression: ExpressionController | undefined; let frame = 0;
function handleLoaded(): void { imageLoaded.value = true; console.info('[DigitalHuman2D] lingling.png loaded'); }
function handleMissing(error?: unknown): void { missing.value = true; console.error('[DigitalHuman2D] lingling.png load failed', error); emit('error', error || new Error('lingling.png not found')); }
function setState(value: DigitalHumanState): void { state.value = value; emit('state', value); }
function selectOutfit(outfit: LinglingOutfit): void {
  const image = new Image(); image.onload = () => { switching.value = true; currentOutfit.value = outfit; missing.value = false; settingsOpen.value = false; setTimeout(() => { switching.value = false; }, 260); }; image.onerror = () => { uni.showToast({ title: `${outfit.name}素材暂不可用`, icon: 'none' }); }; image.src = outfit.image;
}
function selectVoice(voice: VoiceProfile): void {
  currentVoice.value = voice;
  if (typeof localStorage !== 'undefined') localStorage.setItem('lingling-voice-profile', voice.id);
  settingsOpen.value = false;
  console.info('[VoiceProfile]', { selected: voice.id, provider: voice.provider, voiceId: voice.voiceId, voiceName: voice.voiceName || '(system voice)', voiceURI: voice.voiceURI || '(system voice)', rate: voice.rate, pitch: voice.pitch, volume: 1 });
}
function availableChineseVoices(): SpeechSynthesisVoice[] {
  if (typeof window === 'undefined' || !('speechSynthesis' in window)) return [];
  return window.speechSynthesis.getVoices().filter((item) => item.lang.toLowerCase().startsWith('zh'));
}
function resolveBrowserVoice(): SpeechSynthesisVoice | undefined {
  const voices = availableChineseVoices();
  if (!voices.length) return undefined;
  const exact = voices.find((item) => item.voiceURI === currentVoice.value.voiceURI || item.name === currentVoice.value.voiceName);
  if (exact) return exact;
  const profileIndex = voices.length > 1 ? ({ 'lingling-warm': 0, 'lingling-active': 1, 'lingling-calm': 2 } as Record<string, number>)[currentVoice.value.id] ?? 0 : 0;
  return voices[Math.min(profileIndex, voices.length - 1)];
}
async function playAudio(): Promise<void> {
  const text = '你好，我是灵灵，你的AI旅行数字员工。告诉我你想去哪里，我来帮你规划旅程。';
  if (currentVoice.value.provider === 'aliyun') {
    try {
      const result = await synthesize({ text, voiceProfileId: currentVoice.value.id, voiceId: currentVoice.value.voiceId, rate: currentVoice.value.rate, pitch: currentVoice.value.pitch });
      console.info('[TTS Result]', { source: 'backend', audio: result.audioUrl });
      if (audio) {
        audio.src = result.audioUrl;
        await audio.play();
        console.info('[Audio] play started', { source: 'backend', voiceId: currentVoice.value.voiceId });
        audioBlocked.value = false;
        setState('SPEAKING');
        return;
      }
    } catch (error) {
      console.warn('[TTS Error] backend failed, using browser fallback', error);
    }
  }
  if (typeof window !== 'undefined' && 'speechSynthesis' in window) {
    window.speechSynthesis.cancel();
    const utterance = new SpeechSynthesisUtterance(text); utterance.lang = currentVoice.value.voiceId; utterance.rate = currentVoice.value.rate; utterance.pitch = currentVoice.value.pitch;
    const voice = resolveBrowserVoice();
    console.info('[TTS]', { requestVoice: currentVoice.value.voiceId, provider: currentVoice.value.provider });
    console.info('[TTS Result]', { source: 'browser-fallback' });
    console.info('[Audio]', { source: 'browser-fallback', voice: voice ? { name: voice.name, lang: voice.lang, voiceURI: voice.voiceURI } : null });
    if (voice) { currentVoice.value.voiceName = voice.name; currentVoice.value.voiceURI = voice.voiceURI; }
    if (voice) utterance.voice = voice;
    utterance.onstart = () => setState('SPEAKING'); utterance.onend = () => setState('IDLE'); utterance.onerror = () => { audioBlocked.value = true; setState('IDLE'); };
    window.speechSynthesis.speak(utterance);
    return;
  }
  if (!audio || !props.audioUrl) return;
  context ||= new AudioContext(); lipSync ||= new LipSyncController(audio, (value) => { mouth.value = value; }); lipSync.connect(context); await context.resume();
  try { await audio.play(); audioBlocked.value = false; setState('SPEAKING'); } catch { audioBlocked.value = true; }
}
function tick(): void { lipSync?.update(); frame = requestAnimationFrame(tick); }
onMounted(() => { audio = new Audio(props.audioUrl || ''); audio.preload = 'auto'; audio.addEventListener('error', () => { audioBlocked.value = true; console.error('[Audio] playback error', audio?.error); }); audio.addEventListener('ended', () => { console.info('[Audio] play ended'); lipSync?.reset(); setState('IDLE'); }); expression = new ExpressionController((value) => { blink.value = value; }); if (typeof localStorage !== 'undefined') { const saved = localStorage.getItem('lingling-voice-profile'); const match = voices.find((item) => item.id === saved); if (match) currentVoice.value = match; } if (typeof window !== 'undefined' && 'speechSynthesis' in window) window.speechSynthesis.addEventListener('voiceschanged', () => console.info('[TTS] available Chinese voices', availableChineseVoices().map((item) => ({ name: item.name, lang: item.lang, voiceURI: item.voiceURI })))); tick(); handleLoaded(); emit('ready'); });
onBeforeUnmount(() => { cancelAnimationFrame(frame); expression?.dispose(); audio?.pause(); context?.close(); });
</script>

<style scoped lang="scss">
.digital-human-2d { position:relative; width:100%; height:100%; min-height:0; overflow:hidden; display:flex; align-items:flex-end; justify-content:center; background:transparent; }
.digital-human-2d__glow { position:absolute; width:72%; height:34%; bottom:4%; border-radius:50%; background:radial-gradient(ellipse, rgba(116,220,188,.28), transparent 68%); filter:blur(16px); }
.digital-human-2d__image { position:relative; z-index:1; display:block; flex:1 1 auto; min-width:1px; min-height:1px; width:100%; height:100%; object-fit:contain; transform-origin:center bottom; opacity:1; transition:opacity .26s ease, transform .35s ease; }
.digital-human-2d__image--switching { opacity:.2; }
.digital-human-2d--speaking .digital-human-2d__image { animation:lingling-speak 1.8s ease-in-out infinite; }
.digital-human-2d__placeholder { position:relative; z-index:1; display:flex; flex-direction:column; align-items:center; gap:8px; padding-bottom:80px; color:rgba(255,255,255,.9); text-shadow:0 2px 12px rgba(0,0,0,.28); }.digital-human-2d__placeholder text:first-child { font-size:32px; font-weight:700; }.digital-human-2d__placeholder text:last-child { font-size:13px; }
.digital-human-2d__status { position:absolute; z-index:2; right:12px; bottom:12px; padding:7px 12px; border-radius:16px; background:rgba(7,68,54,.72); color:#fff; font-size:13px; }.digital-human-2d__audio-hint { position:absolute; z-index:2; bottom:52px; padding:7px 12px; border-radius:14px; background:rgba(255,255,255,.88); color:#07543f; font-size:13px; }.digital-human-2d__settings { position:absolute; z-index:3; top:12px; right:12px; padding:6px 10px; border-radius:14px; background:rgba(255,255,255,.78); color:#07543f; font-size:12px; }.digital-human-2d__panel { position:absolute; z-index:4; top:44px; right:12px; width:150px; padding:12px; border-radius:14px; background:rgba(255,255,255,.96); box-shadow:0 12px 28px rgba(0,0,0,.16); color:#174d40; }.digital-human-2d__panel-title { display:block; margin:4px 0 6px; font-size:12px; font-weight:700; }.digital-human-2d__option { padding:7px 4px; font-size:13px; }
.digital-human-2d--missing { background:linear-gradient(180deg, rgba(255,255,255,.03), rgba(4,57,45,.2)); }
@media (prefers-reduced-motion: reduce) { .digital-human-2d__image { transition:none; } }
@keyframes lingling-speak { 0%,100% { transform:translateY(0) scale(1); } 50% { transform:translateY(-4px) scale(1.006); } }
</style>
