export interface LinglingOutfit { id: string; name: string; image: string; city?: string; season?: string; theme?: string; }
// lingling-default.png is an RGB preview with a baked checkerboard. Use the
// verified RGBA jade export as the production default until a transparent
// default-color export is provided.
export const defaultLinglingOutfit: LinglingOutfit = { id: 'lingling-default', name: '灵灵·默认制服', image: '/static/digital-human/lingling-jade.png', theme: '现代新中式旅行顾问' };
export const linglingOutfits: LinglingOutfit[] = [
  defaultLinglingOutfit,
  { id: 'lingling-jade', name: '灵灵·青绿', image: '/static/digital-human/lingling-jade.png', theme: '西湖青' },
  { id: 'lingling-blue', name: '灵灵·蓝白', image: '/static/digital-human/lingling-blue.png', theme: '清远蓝白' },
  { id: 'lingling-gold', name: '灵灵·暖金', image: '/static/digital-human/lingling-gold-transparent.png', theme: '城市暖金' },
  { id: 'lingling-pink', name: '灵灵·粉彩', image: '/static/digital-human/lingling-pink-transparent.png', theme: '柔和粉彩' },
];
