const EMOTION_LABELS: Record<string, string> = {
  POSITIVE: '正向',
  NEUTRAL: '中性',
  NEGATIVE: '负向',
  COMPLAINT: '投诉'
};

const INTEREST_LABELS: Record<string, string> = {
  history: '历史文化',
  nature: '自然风光',
  photo: '摄影打卡',
  parent: '亲子家庭',
  family: '亲子家庭',
  culture: '文化体验',
  classic: '经典必游',
  leisure: '轻松休闲',
  deep: '深度游览'
};

export function emotionText(value: string | null | undefined): string {
  if (!value) {
    return '未识别';
  }
  return EMOTION_LABELS[value.toUpperCase()] || value;
}

export function interestText(value: string | null | undefined): string {
  if (!value) {
    return '';
  }
  return value
    .split(/[,，、;；\s]+/)
    .map((item) => item.trim())
    .filter(Boolean)
    .map((item) => INTEREST_LABELS[item] || item)
    .join('、');
}
