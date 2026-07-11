<template>
  <view class="page chat-page">
    <AvatarBox
      :connected="Boolean(sessionNo)"
      :stream-url="lastAnswer?.streamUrl || null"
      :audio-url="lastAnswer?.audioUrl || null"
      :avatar="store.selectedAvatar"
      :text="lastAnswer?.answer || ''"
    />

    <scroll-view class="message-list" scroll-y>
      <ChatBubble
        v-for="item in messages"
        :key="item.id"
        :role="item.role"
        :text="item.text"
        :avatar-image="assistantAvatarImage"
        :user-initial="userInitial"
      />
      <view v-if="messages.length === 0" class="empty">输入问题后，导游会结合知识库回答。</view>
    </scroll-view>

    <view class="source-card" v-if="lastAnswer">
      <text class="tag">{{ lastAnswer.hitKb === 1 ? '知识库命中' : '兜底回答' }}</text>
      <text class="muted">耗时 {{ lastAnswer.costMs }} ms / 情绪 {{ emotionText(lastAnswer.emotion) }}</text>
    </view>

    <view class="composer">
      <input v-model="question" class="input" placeholder="问问景点历史、路线、开放时间" />
      <button class="button" :loading="loading" @tap="sendText">发送</button>
      <VoiceButton :recording="recording" :loading="voiceLoading" @toggle="toggleRecord" />
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';
import { onLoad } from '@dcloudio/uni-app';

import { getCurrentAvatar } from '../../api/avatar';
import { askTextStream, askVoiceStream, createSession, getSessionMessages, type ChatStreamHandlers } from '../../api/chat';
import AvatarBox from '../../components/AvatarBox/index.vue';
import ChatBubble from '../../components/ChatBubble/index.vue';
import VoiceButton from '../../components/VoiceButton/index.vue';
import { useTouristStore } from '../../stores';
import type { ChatAnswerVO, MessageVO, SourceVO } from '../../types';
import { requireLogin } from '../../utils/auth';
import { emotionText } from '../../utils/display';

interface MessageItem {
  id: string;
  role: 'user' | 'ai';
  text: string;
}

const store = useTouristStore();
const loading = ref(false);
const voiceLoading = ref(false);
const recording = ref(false);
const question = ref('');
const messages = ref<MessageItem[]>([]);
const lastAnswer = ref<ChatAnswerVO | null>(null);
const sessionNo = computed(() => store.currentSessionNo);
const assistantAvatarImage = computed(() => store.selectedAvatar?.avatarImage || store.selectedAvatar?.outfitImage || '');
const userInitial = computed(() => (store.profile?.nickname || store.touristInfo?.nickname || '我').slice(0, 1));
let uniRecorder: ReturnType<typeof uni.getRecorderManager> | null = null;
let mediaRecorder: MediaRecorder | null = null;
let mediaStream: MediaStream | null = null;
let mediaChunks: BlobPart[] = [];

onLoad((query) => {
  if (typeof query.question === 'string') {
    question.value = decodeURIComponent(query.question);
  }
});

onMounted(() => {
  if (requireLogin()) {
    void loadAvatarState()
      .then(() => restoreCurrentSession())
      .catch((error: unknown) => {
        const message = error instanceof Error ? error.message : '数字人形象加载失败';
        uni.showToast({ title: message, icon: 'none' });
      });
  }
});

onBeforeUnmount(() => {
  stopBrowserTracks();
});

async function ensureSession(): Promise<string> {
  await ensureAvatarLoaded();
  const avatarId = store.selectedAvatar?.id;
  if (store.currentSessionNo) {
    if (!store.currentSessionAvatarId || !avatarId || store.currentSessionAvatarId === avatarId) {
      return store.currentSessionNo;
    }
  }
  const session = await createSession(store.currentScenicId, avatarId);
  const selected = session.selectedAvatar || session.defaultAvatar || store.selectedAvatar;
  store.setSession(session.sessionNo, session.scenicId, selected?.id || null);
  store.setSelectedAvatar(selected || null);
  return session.sessionNo;
}

async function ensureAvatarLoaded(): Promise<void> {
  if (!store.selectedAvatar) {
    await loadAvatarState();
  }
}

async function loadAvatarState(): Promise<void> {
  const current = await getCurrentAvatar();
  store.setSelectedAvatar(current || null);
}

async function restoreCurrentSession(): Promise<void> {
  if (store.currentSessionNo) {
    await loadSessionMessages(store.currentSessionNo);
  }
}

async function loadSessionMessages(sessionNoValue: string): Promise<void> {
  if (messages.value.length > 0) {
    return;
  }
  const records = await getSessionMessages(sessionNoValue);
  if (records.length === 0) {
    return;
  }
  const restored: MessageItem[] = [];
  records.forEach((item) => {
    const questionText = historyQuestionText(item);
    if (questionText) {
      restored.push({ id: `u-${item.messageId}`, role: 'user', text: questionText });
    }
    if (item.answer) {
      restored.push({ id: `a-${item.messageId}`, role: 'ai', text: item.answer });
    }
  });
  messages.value = restored;
  const latest = [...records].reverse().find((item) => item.answer);
  if (latest) {
    lastAnswer.value = historyAnswer(latest);
  }
}

async function sendText(): Promise<void> {
  const text = question.value.trim();
  if (!text) {
    uni.showToast({ title: '请输入问题', icon: 'none' });
    return;
  }
  loading.value = true;
  const aiMessageId = `a-${Date.now()}`;
  messages.value.push({ id: `u-${Date.now()}`, role: 'user', text });
  messages.value.push({ id: aiMessageId, role: 'ai', text: '正在思考…' });
  beginAnswer();
  question.value = '';
  try {
    const answer = await askTextStream(await ensureSession(), text, undefined, streamHandlers(aiMessageId));
    finishAnswer(aiMessageId, answer);
  } catch (error: unknown) {
    removeMessage(aiMessageId);
    const message = error instanceof Error ? error.message : '问答失败';
    uni.showToast({ title: message, icon: 'none' });
  } finally {
    loading.value = false;
  }
}

function toggleRecord(): void {
  if (recording.value) {
    stopRecord();
    return;
  }
  void startRecord();
}

async function startRecord(): Promise<void> {
  if (voiceLoading.value) {
    return;
  }
  if (canUseBrowserRecorder()) {
    await startBrowserRecord();
    return;
  }
  startUniRecord();
}

function stopRecord(): void {
  if (mediaRecorder && mediaRecorder.state !== 'inactive') {
    mediaRecorder.stop();
    return;
  }
  if (uniRecorder) {
    uniRecorder.stop();
  }
}

async function startBrowserRecord(): Promise<void> {
  try {
    mediaStream = await navigator.mediaDevices.getUserMedia({ audio: true });
    mediaChunks = [];
    const mimeType = preferredMimeType();
    mediaRecorder = new MediaRecorder(mediaStream, mimeType ? { mimeType } : undefined);
    mediaRecorder.ondataavailable = (event) => {
      if (event.data && event.data.size > 0) {
        mediaChunks.push(event.data);
      }
    };
    mediaRecorder.onstop = () => {
      recording.value = false;
      const blob = new Blob(mediaChunks, { type: mimeType || 'audio/webm' });
      stopBrowserTracks();
      void toAsrWavBlob(blob).then((audio) => sendVoice(audio));
    };
    mediaRecorder.onerror = () => {
      recording.value = false;
      stopBrowserTracks();
      uni.showToast({ title: '录音失败', icon: 'none' });
    };
    recording.value = true;
    mediaRecorder.start();
    window.setTimeout(() => {
      if (mediaRecorder && mediaRecorder.state === 'recording') {
        mediaRecorder.stop();
      }
    }, 60000);
  } catch {
    recording.value = false;
    stopBrowserTracks();
    uni.showToast({ title: '无法打开麦克风，请检查浏览器权限', icon: 'none' });
  }
}

function startUniRecord(): void {
  const recorder = getUniRecorder();
  if (!recorder) {
    uni.showToast({ title: '当前环境不支持录音', icon: 'none' });
    return;
  }
  recording.value = true;
  recorder.start({ duration: 60000, format: 'mp3' });
}

function getUniRecorder(): ReturnType<typeof uni.getRecorderManager> | null {
  if (typeof uni.getRecorderManager !== 'function') {
    return null;
  }
  if (uniRecorder) {
    return uniRecorder;
  }
  uniRecorder = uni.getRecorderManager();
  uniRecorder.onStop((result: { tempFilePath: string }) => {
    recording.value = false;
    void sendVoice(result.tempFilePath);
  });
  uniRecorder.onError(() => {
    recording.value = false;
    uni.showToast({ title: '录音失败', icon: 'none' });
  });
  return uniRecorder;
}

function canUseBrowserRecorder(): boolean {
  return typeof navigator !== 'undefined'
    && Boolean(navigator.mediaDevices?.getUserMedia)
    && typeof MediaRecorder !== 'undefined';
}

function preferredMimeType(): string | undefined {
  const candidates = ['audio/webm;codecs=opus', 'audio/webm', 'audio/mp4', 'audio/ogg;codecs=opus'];
  return candidates.find((item) => MediaRecorder.isTypeSupported(item));
}

function stopBrowserTracks(): void {
  mediaStream?.getTracks().forEach((track) => track.stop());
  mediaStream = null;
  mediaRecorder = null;
}

async function toAsrWavBlob(blob: Blob): Promise<Blob> {
  const AudioContextCtor = window.AudioContext || (window as Window & { webkitAudioContext?: typeof AudioContext }).webkitAudioContext;
  if (!AudioContextCtor) {
    return blob;
  }
  let audioContext: AudioContext | null = null;
  try {
    audioContext = new AudioContextCtor();
    const decoded = await audioContext.decodeAudioData(await blob.arrayBuffer());
    const pcm = resampleToMono(decoded, 16000);
    return new Blob([encodeWav(pcm, 16000)], { type: 'audio/wav' });
  } catch {
    return blob;
  } finally {
    await audioContext?.close().catch(() => undefined);
  }
}

function resampleToMono(buffer: AudioBuffer, targetSampleRate: number): Float32Array {
  const sourceRate = buffer.sampleRate;
  const sourceLength = buffer.length;
  const mono = new Float32Array(sourceLength);
  for (let channel = 0; channel < buffer.numberOfChannels; channel += 1) {
    const data = buffer.getChannelData(channel);
    for (let i = 0; i < sourceLength; i += 1) {
      mono[i] += data[i] / buffer.numberOfChannels;
    }
  }
  if (sourceRate === targetSampleRate) {
    return mono;
  }
  const targetLength = Math.max(1, Math.round(sourceLength * targetSampleRate / sourceRate));
  const output = new Float32Array(targetLength);
  const ratio = sourceRate / targetSampleRate;
  for (let i = 0; i < targetLength; i += 1) {
    const sourceIndex = i * ratio;
    const left = Math.floor(sourceIndex);
    const right = Math.min(left + 1, sourceLength - 1);
    const fraction = sourceIndex - left;
    output[i] = mono[left] * (1 - fraction) + mono[right] * fraction;
  }
  return output;
}

function encodeWav(samples: Float32Array, sampleRate: number): ArrayBuffer {
  const bytesPerSample = 2;
  const dataLength = samples.length * bytesPerSample;
  const buffer = new ArrayBuffer(44 + dataLength);
  const view = new DataView(buffer);
  writeAscii(view, 0, 'RIFF');
  view.setUint32(4, 36 + dataLength, true);
  writeAscii(view, 8, 'WAVE');
  writeAscii(view, 12, 'fmt ');
  view.setUint32(16, 16, true);
  view.setUint16(20, 1, true);
  view.setUint16(22, 1, true);
  view.setUint32(24, sampleRate, true);
  view.setUint32(28, sampleRate * bytesPerSample, true);
  view.setUint16(32, bytesPerSample, true);
  view.setUint16(34, 16, true);
  writeAscii(view, 36, 'data');
  view.setUint32(40, dataLength, true);
  let offset = 44;
  for (const sample of samples) {
    const clamped = Math.max(-1, Math.min(1, sample));
    view.setInt16(offset, clamped < 0 ? clamped * 0x8000 : clamped * 0x7fff, true);
    offset += bytesPerSample;
  }
  return buffer;
}

function writeAscii(view: DataView, offset: number, value: string): void {
  for (let i = 0; i < value.length; i += 1) {
    view.setUint8(offset + i, value.charCodeAt(i));
  }
}

async function sendVoice(file: string | Blob): Promise<void> {
  const userMessageId = `v-${Date.now()}`;
  const aiMessageId = `a-${Date.now() + 1}`;
  voiceLoading.value = true;
  messages.value.push({ id: userMessageId, role: 'user', text: '正在识别语音…' });
  messages.value.push({ id: aiMessageId, role: 'ai', text: '正在思考…' });
  beginAnswer();
  try {
    const answer = await askVoiceStream(await ensureSession(), file, {
      ...streamHandlers(aiMessageId),
      onAsr: (text) => {
        updateMessage(userMessageId, text || '语音提问');
      }
    });
    if (answer.asrText) {
      updateMessage(userMessageId, answer.asrText);
    }
    finishAnswer(aiMessageId, answer);
  } catch (error: unknown) {
    removeMessage(aiMessageId);
    updateMessage(userMessageId, '语音提问失败');
    const message = error instanceof Error ? error.message : '语音问答失败';
    uni.showToast({ title: message, icon: 'none' });
  } finally {
    voiceLoading.value = false;
  }
}

function beginAnswer(): void {
  lastAnswer.value = {
    messageId: 0,
    answer: '',
    hitKb: 0,
    sources: [],
    emotion: 'NEUTRAL',
    streamUrl: null,
    audioUrl: null,
    costMs: 0
  };
}

function streamHandlers(messageId: string): ChatStreamHandlers {
  return {
    onMeta: (id) => patchAnswer({ messageId: id }),
    onDelta: (delta) => {
      const next = `${lastAnswer.value?.answer || ''}${delta}`;
      patchAnswer({ answer: next });
      updateMessage(messageId, next || '正在思考…');
    },
    onSources: (hitKb, sources) => patchAnswer({ hitKb, sources }),
    onEmotion: (emotion) => patchAnswer({ emotion }),
    onAudio: (audioUrl) => patchAnswer({ audioUrl }),
    onAvatar: (streamUrl) => patchAnswer({ streamUrl })
  };
}

function patchAnswer(patch: Partial<ChatAnswerVO>): void {
  lastAnswer.value = { ...(lastAnswer.value || emptyAnswer()), ...patch };
}

function finishAnswer(messageId: string, answer: ChatAnswerVO): void {
  lastAnswer.value = answer;
  updateMessage(messageId, answer.answer || '暂无回复');
}

function emptyAnswer(): ChatAnswerVO {
  return {
    messageId: 0,
    answer: '',
    hitKb: 0,
    sources: [],
    emotion: 'NEUTRAL',
    streamUrl: null,
    audioUrl: null,
    costMs: 0
  };
}

function historyQuestionText(item: MessageVO): string {
  if (item.inputType === 'VOICE') {
    return item.asrText || item.question || '语音提问';
  }
  if (item.inputType === 'IMAGE') {
    return item.question || '图片讲解';
  }
  return item.question || '';
}

function historyAnswer(item: MessageVO): ChatAnswerVO {
  return {
    messageId: item.messageId,
    answer: item.answer || '',
    hitKb: item.hitKb || 0,
    sources: parseSources(item.sources),
    emotion: item.emotion || 'NEUTRAL',
    streamUrl: item.streamUrl,
    audioUrl: item.audioUrl,
    costMs: item.costMs || 0
  };
}

function parseSources(raw: string | null): SourceVO[] {
  if (!raw) {
    return [];
  }
  try {
    const parsed = JSON.parse(raw) as unknown;
    return Array.isArray(parsed) ? parsed as SourceVO[] : [];
  } catch {
    return [];
  }
}

function updateMessage(id: string, text: string): void {
  const item = messages.value.find((message) => message.id === id);
  if (item) {
    item.text = text;
  }
}

function removeMessage(id: string): void {
  messages.value = messages.value.filter((message) => message.id !== id);
}
</script>

<style scoped lang="scss">
.chat-page {
  display: flex;
  flex-direction: column;
  gap: 24rpx;
}

.message-list {
  height: 52vh;
}

.source-card,
.composer {
  display: grid;
  gap: 16rpx;
}
</style>
