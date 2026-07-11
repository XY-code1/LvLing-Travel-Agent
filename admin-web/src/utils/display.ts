import type { FeatureItemVO } from '../types';

export interface LabelOption<T extends string | number = string> {
  label: string;
  value: T;
}

const EMOTION_LABELS: Record<string, string> = {
  POSITIVE: '正向',
  NEUTRAL: '中性',
  NEGATIVE: '负向',
  COMPLAINT: '投诉'
};

const INPUT_TYPE_LABELS: Record<string, string> = {
  TEXT: '文本',
  VOICE: '语音',
  IMAGE: '图片'
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
  deep: '深度游览',
  architecture: '建筑艺术',
  food: '美食体验',
  worship: '祈福礼佛'
};

const ROUTE_TYPE_LABELS: Record<number, string> = {
  1: '经典必游',
  2: '亲子家庭',
  3: '长者轻松',
  4: '历史文化',
  5: '摄影打卡',
  6: '休闲轻松',
  7: '深度游览'
};

export function emotionText(value: string | null | undefined): string {
  if (!value) {
    return '未识别';
  }
  return EMOTION_LABELS[value.toUpperCase()] || value;
}

export function inputTypeText(value: string | null | undefined): string {
  if (!value) {
    return '未设置';
  }
  return INPUT_TYPE_LABELS[value.toUpperCase()] || value;
}

export function routeTypeFallbackText(value: number | null | undefined): string {
  if (value === null || value === undefined) {
    return '未设置';
  }
  return ROUTE_TYPE_LABELS[value] || `类型 ${value}`;
}

export function splitCodes(value: string | null | undefined): string[] {
  if (!value) {
    return [];
  }
  return value
    .split(/[,，、;；\s]+/)
    .map((item) => item.trim())
    .filter(Boolean);
}

export function featureCode(item: FeatureItemVO): string {
  return (item.category || item.title || '').trim();
}

export function featureLabelMap(items: FeatureItemVO[]): Record<string, string> {
  const result: Record<string, string> = {};
  items.forEach((item) => {
    const code = featureCode(item);
    if (code) {
      result[code] = item.title;
    }
    if (item.title) {
      result[item.title] = item.title;
    }
  });
  return result;
}

export function managedTagOptions(items: FeatureItemVO[], useTitleAsValue = true): LabelOption[] {
  return items
    .map((item) => ({
      label: item.title,
      value: useTitleAsValue ? item.title : featureCode(item)
    }))
    .filter((item) => item.label && item.value);
}

export function routeTypeOptions(items: FeatureItemVO[]): LabelOption<number>[] {
  const dynamic = items
    .map((item) => {
      const code = Number(featureCode(item));
      return Number.isInteger(code) ? { label: item.title, value: code } : null;
    })
    .filter((item): item is LabelOption<number> => Boolean(item));
  if (dynamic.length > 0) {
    return dynamic;
  }
  return Object.entries(ROUTE_TYPE_LABELS).map(([value, label]) => ({
    label,
    value: Number(value)
  }));
}

export function routeTypeText(value: number | null | undefined, options: LabelOption<number>[] = []): string {
  const matched = options.find((item) => item.value === value);
  return matched?.label || routeTypeFallbackText(value);
}

export function tagText(value: string | null | undefined, labels: Record<string, string> = {}): string {
  if (!value) {
    return '未设置';
  }
  return splitCodes(value)
    .map((code) => labels[code] || INTEREST_LABELS[code] || code)
    .join('、') || '未设置';
}

export function tagLabel(value: string | null | undefined, labels: Record<string, string> = {}): string {
  if (!value) {
    return '未设置';
  }
  return labels[value] || INTEREST_LABELS[value] || value;
}
