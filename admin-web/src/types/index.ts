export interface ApiResult<T> {
  code: number;
  msg: string;
  data: T;
}

export interface PageResult<T> {
  total: number;
  pageNum: number;
  pageSize: number;
  list: T[];
}

export interface AdminLoginDTO {
  username: string;
  password: string;
  captchaId: string;
  captchaCode: string;
}
export interface CaptchaVO { captchaId: string; image: string; }

export interface AdminInfoVO {
  id: number;
  username: string;
  realName: string;
  role: string;
}

export interface CityVO { id: number; cityCode: string; cityName: string; province: string | null; country: string | null; description: string | null; slogan: string | null; coverImage: string | null; status: number; sortOrder: number; }

export interface AdminLoginVO {
  token: string;
  adminInfo: AdminInfoVO;
}

export interface TodayStatVO {
  serviceCount: number;
  qaCount: number;
  voiceCount: number;
  imageCount: number;
  avatarCount: number;
  avgCostMs: number;
  kbHitRate: number;
  errorCount: number;
}

export interface TrendVO {
  date: string;
  serviceCount: number;
  kbHitRate: number;
}

export interface DashboardOverviewVO {
  today: TodayStatVO;
  hotQuestions: Array<Record<string, unknown>>;
  hotSpots: Array<Record<string, unknown>>;
  emotionDist: Record<string, number>;
  trend7d: TrendVO[];
  demo: number;
}

export interface QueryParams {
  [key: string]: string | number | boolean | null | undefined;
}

export interface BasePageQuery extends QueryParams {
  pageNum: number;
  pageSize: number;
}

export interface StatusDTO {
  id: number;
  status: number;
}

export interface FileUploadVO {
  url: string;
  fileName: string;
}

export interface ScenicVO {
  id: number;
  name: string;
  intro: string | null;
  address: string | null;
  openTime: string | null;
  ticketInfo: string | null;
  trafficInfo: string | null;
  servicePhone: string | null;
  notice: string | null;
  coverImage: string | null;
  longitude: number | null;
  latitude: number | null;
  status: number;
  createTime: string;
  updateTime: string;
}

export interface ScenicPageQuery extends BasePageQuery {
  name?: string;
  status?: number | null;
}

export interface ScenicFormDTO {
  id?: number;
  name: string;
  intro: string;
  address: string;
  openTime: string;
  ticketInfo: string;
  trafficInfo: string;
  servicePhone: string;
  notice: string;
  coverImage: string;
  longitude: number | null;
  latitude: number | null;
  status: number;
}

export interface SpotVO {
  id: number;
  scenicId: number;
  scenicName: string | null;
  name: string;
  alias: string | null;
  intro: string | null;
  historyCulture: string | null;
  guideText: string | null;
  tags: string | null;
  images: string | null;
  stayMinutes: number | null;
  suitCrowd: string | null;
  isHot: number;
  longitude: number | null;
  latitude: number | null;
  status: number;
  createTime: string;
  updateTime: string;
}

export interface SpotPageQuery extends BasePageQuery {
  scenicId?: number | null;
  scenicName?: string;
  name?: string;
  tag?: string;
  status?: number | null;
  isHot?: number | null;
}

export interface SpotFormDTO {
  id?: number;
  scenicId: number | null;
  name: string;
  alias: string;
  intro: string;
  historyCulture: string;
  guideText: string;
  tags: string;
  images: string;
  stayMinutes: number | null;
  suitCrowd: string;
  isHot: number;
  longitude: number | null;
  latitude: number | null;
  status: number;
}

export interface RouteSpotVO {
  spotId: number;
  name: string;
  sortOrder: number;
}

export interface RouteVO {
  id: number;
  scenicId: number;
  scenicName: string | null;
  name: string;
  type: number;
  intro: string | null;
  estimateMinutes: number | null;
  suitCrowd: string | null;
  interestTags: string | null;
  recommendReason: string | null;
  notice: string | null;
  status: number;
  createTime: string;
  updateTime: string;
  spots?: RouteSpotVO[];
}

export interface RoutePageQuery extends BasePageQuery {
  scenicId?: number | null;
  scenicName?: string;
  name?: string;
  type?: number | null;
  status?: number | null;
}

export interface RouteFormDTO {
  id?: number;
  scenicId: number | null;
  name: string;
  type: number | null;
  intro: string;
  estimateMinutes: number | null;
  suitCrowd: string;
  interestTags: string;
  recommendReason: string;
  notice: string;
  status: number;
}

export interface KnowledgeDocumentVO {
  id: number;
  scenicId: number | null;
  scenicName: string | null;
  fileName: string;
  fileType: string | null;
  filePath: string;
  fileSize: number;
  chunkCount: number | null;
  embedStatus: number | null;
  difyDocumentId: string | null;
  difyDatasetId: string | null;
  syncStatus: number;
  syncMsg: string | null;
  status: number;
  createTime: string;
  updateTime: string;
}

export interface KnowledgePageQuery extends BasePageQuery {
  scenicId?: number | null;
  scenicName?: string;
  syncStatus?: number | null;
}

export interface KnowledgeUploadVO {
  docId: number;
  syncStatus: number;
}

export interface KnowledgeSyncVO {
  syncStatus: number;
  syncMsg: string;
}

export interface KnowledgeTestSourceVO {
  docName: string;
  segment: string;
  score: number;
}

export interface KnowledgeTestVO {
  answer: string;
  hit: number;
  sources: KnowledgeTestSourceVO[];
  costMs: number;
}

export interface AiConfigVO {
  id: number;
  serviceType: string;
  provider: string;
  protocol: string | null;
  baseUrl: string | null;
  apiKey: string | null;
  accessKeyId: string | null;
  hasSecret: boolean;
  appKey: string | null;
  region: string | null;
  modelName: string | null;
  datasetId: string | null;
  extraConfig: string | null;
  timeoutMs: number | null;
  retryCount: number | null;
  capabilityVerified: number;
  verifiedTime: string | null;
  verifyMsg: string | null;
  isDefault: number;
  enabled: number;
  remark: string | null;
  createTime: string;
  updateTime: string;
}

export interface AiConfigFormDTO {
  id?: number;
  serviceType: string;
  provider: string;
  protocol: string;
  baseUrl: string;
  apiKey: string;
  accessKeyId: string;
  accessKeySecret: string;
  appKey: string;
  region: string;
  modelName: string;
  datasetId: string;
  extraConfig: string;
  timeoutMs: number | null;
  retryCount: number | null;
  enabled: number;
  remark: string;
}

export interface AiConfigTestDTO {
  imageBase64?: string;
  imageUrl?: string;
  mimeType?: string;
  prompt?: string;
  question?: string;
}

export interface AiConfigTestVO {
  success: boolean;
  costMs: number;
  msg: string;
  capabilityVerified: number;
}

export interface AvatarConfigVO {
  id: number;
  name: string;
  provider: string | null;
  instanceId: string | null;
  avatarImage: string | null;
  gender: string | null;
  appearance: string | null;
  outfit: string | null;
  outfitImage: string | null;
  renderConfig: string | null;
  voice: string | null;
  speechRate: number | null;
  welcomeText: string | null;
  isDefault: number;
  enabled: number;
  remark: string | null;
  createTime: string;
  updateTime: string;
}

export interface AvatarFormDTO {
  id?: number;
  name: string;
  provider: string;
  instanceId: string;
  avatarImage: string;
  gender: string;
  appearance: string;
  outfit: string;
  outfitImage: string;
  renderConfig: string;
  voice: string;
  speechRate: number | null;
  welcomeText: string;
  enabled: number;
  remark: string;
}

export interface AvatarTestVO {
  streamUrl: string | null;
  audioUrl: string | null;
  costMs: number;
  msg: string;
}

export interface AdminChatPageQuery extends BasePageQuery {
  startTime?: string;
  endTime?: string;
  inputType?: string;
  emotion?: string;
  success?: number | null;
  keyword?: string;
}

export interface AdminChatMessageVO {
  id: number;
  sessionId: number;
  sessionNo: string;
  touristUserId: number;
  inputType: string;
  question: string | null;
  asrText: string | null;
  imageUrl: string | null;
  answer: string | null;
  sources: string | null;
  hitKb: number;
  emotion: string | null;
  audioUrl: string | null;
  streamUrl: string | null;
  costMs: number | null;
  success: number;
  errorMsg: string | null;
  needSupplement: number;
  createTime: string;
}

export interface SentimentReportVO {
  id: number;
  reportDate: string;
  positiveCount: number;
  neutralCount: number;
  negativeCount: number;
  complaintCount: number;
  hotQuestions: string | null;
  hotSpots: string | null;
  unanswered: string | null;
  aiSuggestion: string | null;
  createTime: string;
  updateTime: string;
}

export interface TouristUserVO {
  id: number;
  phone: string;
  nickname: string | null;
  avatar: string | null;
  gender: number | null;
  interestTags: string | null;
  status: number;
  lastLoginTime: string | null;
  createTime: string;
  updateTime: string;
}

export interface TouristUserPageQuery extends BasePageQuery {
  phone?: string;
  nickname?: string;
  status?: number | null;
}

export interface FeatureItemVO {
  id: number;
  moduleType: string;
  scenicId: number | null;
  scenicName: string | null;
  relatedId: number | null;
  relatedName: string | null;
  title: string;
  category: string | null;
  content: string | null;
  mediaUrl: string | null;
  longitude: number | null;
  latitude: number | null;
  sortOrder: number | null;
  status: number;
  remark: string | null;
  createTime: string;
  updateTime: string;
}

export interface FeatureItemPageQuery extends BasePageQuery {
  moduleType?: string;
  scenicName?: string;
  keyword?: string;
  category?: string;
  status?: number | null;
}

export interface FeatureItemFormDTO {
  id?: number;
  moduleType: string;
  scenicId: number | null;
  relatedId: number | null;
  title: string;
  category: string;
  content: string;
  mediaUrl: string;
  longitude: number | null;
  latitude: number | null;
  sortOrder: number | null;
  status: number;
  remark: string;
}

export interface AdminFeedbackVO {
  id: number;
  touristUserId: number;
  touristName: string | null;
  touristPhone: string | null;
  sessionNo: string | null;
  score: number | null;
  content: string | null;
  emotion: string | null;
  handleStatus: number;
  replyContent: string | null;
  replyTime: string | null;
  handler: string | null;
  createTime: string;
  updateTime: string;
}

export interface AdminFeedbackPageQuery extends BasePageQuery {
  score?: number | null;
  emotion?: string;
  handleStatus?: number | null;
  keyword?: string;
}

export interface FeedbackReplyDTO {
  id: number;
  replyContent: string;
  handleStatus: number;
}

export interface SysLogVO {
  id: number;
  logType: string;
  bizDesc: string | null;
  operator: string | null;
  serviceProvider: string | null;
  requestSummary: string | null;
  responseSummary: string | null;
  costMs: number | null;
  success: number;
  errorMsg: string | null;
  ip: string | null;
  createTime: string;
}

export interface SysLogPageQuery extends BasePageQuery {
  logType?: string;
  success?: number | null;
  operator?: string;
  startTime?: string;
  endTime?: string;
}
