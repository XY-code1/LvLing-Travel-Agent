export interface VoiceProfile { id: string; name: string; provider: string; voiceId: string; voiceName?: string; voiceURI?: string; gender: 'FEMALE' | 'MALE'; style: string; rate: number; pitch: number; previewText: string; enabled: boolean; description?: string; }
export const linglingVoiceProfiles: VoiceProfile[] = [
  { id: 'lingling-warm', name: '灵灵·温柔', provider: 'aliyun', voiceId: 'xiaoyun', gender: 'FEMALE', style: '温柔亲和', rate: 0.9, pitch: 1.08, previewText: '你好呀，我是灵灵。', enabled: true },
  { id: 'lingling-active', name: '灵灵·活力', provider: 'aliyun', voiceId: 'xiaomei', gender: 'FEMALE', style: '自然有活力', rate: 0.98, pitch: 1.12, previewText: '出发吧，今天去发现新的风景。', enabled: true },
  { id: 'lingling-calm', name: '灵灵·沉稳', provider: 'aliyun', voiceId: 'ruoxi', gender: 'FEMALE', style: '专业沉稳', rate: 0.86, pitch: 1.02, previewText: '我会为你整理一份舒适的行程。', enabled: true },
];
