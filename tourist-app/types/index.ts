export interface ApiResult<T> {
  code: number;
  msg: string;
  data: T;
}

export interface TouristRegisterDTO {
  phone: string;
  password: string;
  nickname?: string;
}

export interface TouristLoginDTO {
  phone: string;
  password: string;
}

export interface RegisterVO {
  touristId: number;
}

export interface TouristInfoVO {
  id: number;
  phone: string;
  nickname: string | null;
  interestTags: string | null;
  avatarConfigId?: number | null;
}

export interface TouristLoginVO {
  token: string;
  touristInfo: TouristInfoVO;
}

export interface TouristProfileVO {
  id: number;
  phone: string;
  nickname: string | null;
  avatar: string | null;
  gender: number | null;
  interestTags: string | null;
  avatarConfigId: number | null;
}

export interface TouristProfileUpdateDTO {
  nickname?: string;
  gender?: number;
  interestTags?: string;
}

export interface SessionCreateVO {
  sessionNo: string;
  scenicId: number;
  welcomeText: string;
  defaultAvatar: AvatarConfigVO | null;
  selectedAvatar?: AvatarConfigVO | null;
}

export interface AvatarMouthConfig {
  x?: number;
  y?: number;
  width?: number;
  height?: number;
}

export interface AvatarCropConfig {
  x?: number;
  y?: number;
  scale?: number;
}

export interface AvatarRenderConfig {
  mouth?: AvatarMouthConfig;
  crop?: AvatarCropConfig;
  breath?: number;
  blink?: boolean;
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

export interface SessionVO {
  sessionNo: string;
  scenicId: number;
  title: string;
  lastTime: string;
  messageCount: number;
}

export interface MessageVO {
  messageId: number;
  sessionNo: string;
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
  createTime: string;
}

export interface SourceVO {
  docName?: string;
  spotName?: string;
  segment?: string;
  score?: number;
}

export interface ChatAnswerVO {
  messageId: number;
  answer: string;
  hitKb: number;
  sources: SourceVO[];
  emotion: string;
  streamUrl: string | null;
  audioUrl: string | null;
  costMs: number;
  asrText?: string;
}

export interface RecognizedSpotVO {
  spotId: number;
  spotName: string;
  confidence: number;
  name: string;
  intro: string | null;
}

export interface VisionRecognizeVO extends ChatAnswerVO {
  recognizedSpot: RecognizedSpotVO | null;
}

export interface RouteRecommendSpotVO {
  spotId: number;
  name: string;
  sortOrder: number;
  longitude: number | null;
  latitude: number | null;
}

export interface RouteRecommendVO {
  routeId: number;
  name: string;
  type: number;
  estimateMinutes: number | null;
  recommendReason: string;
  spots: RouteRecommendSpotVO[];
}

export type PlannerCheckStatus = 'PASS' | 'WARN' | 'PENDING' | 'FAIL';

export interface TravelRequest {
  destination: string | null;
  startDate: string | null;
  days: number | null;
  budget: number | null;
  travelers: number | null;
  preferences: string[];
  requiredPlaces: string[];
  mobilityPreference: string | null;
  transportPreference: string | null;
  rawText: string;
}

export interface TravelPlanBudget {
  budget: number | null;
  estimatedTotal: number | null;
  transport: number | null;
  dining: number | null;
  tickets: number | null;
  totalDistanceKm: number | null;
  walkingDistanceKm: number | null;
  note: string;
}

export interface TravelPlanCheck {
  status: PlannerCheckStatus;
  message: string;
  source: string;
}

export interface TravelPlanChecks {
  weather: TravelPlanCheck;
  openingHours: TravelPlanCheck;
  route: TravelPlanCheck;
  timeConflict: TravelPlanCheck;
  budget: TravelPlanCheck;
}

export interface TravelActivity {
  time: string;
  poi: { id: number | null; name: string };
  coordinates: { longitude: number | null; latitude: number | null };
  duration: number | null;
  transport: string | null;
  estimatedCost: number | null;
  reason: string;
}

export interface TravelDay {
  day: number;
  title: string;
  activities: TravelActivity[];
}

export type PlannerStepStatus = 'DONE' | 'RUNNING' | 'PENDING' | 'ERROR';

export interface PlannerStep {
  id: string;
  label: string;
  status: PlannerStepStatus;
  detail: string;
}

export interface TravelPlan {
  request: TravelRequest;
  summary: string;
  budget: TravelPlanBudget;
  checks: TravelPlanChecks;
  days: TravelDay[];
  steps: PlannerStep[];
  routeId: number | null;
}

export interface HotSpotVO {
  spotId: number;
  name: string;
  coverImage: string | null;
}

export interface NearbySpotVO extends HotSpotVO {
  distanceMeters: number;
  canGuide: boolean;
}

export interface TouristHomeHotVO {
  hotSpots: HotSpotVO[];
  recommendQuestions: string[];
}

export interface CityVO {
  id: number; cityCode: string; cityName: string; province: string | null;
  country: string | null; description: string | null; slogan: string | null;
  coverImage: string | null; heroImages: string | null; themeConfig: string | null;
  longitude: number | null; latitude: number | null; weatherCode?: string | null;
  status: number; sortOrder: number;
}

export interface CityContextVO {
  city: CityVO | null;
  scenicAreas: Array<{ id: number; name: string }>;
  pois: Array<{ id: number; name: string; images?: string | null }>;
  services: unknown[]; announcements: unknown[]; knowledgeSources: string[];
  discovered: boolean; fallback: boolean; message: string | null;
}

export interface TouristSpotDetailVO {
  id: number;
  scenicId: number;
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
  canGuide: boolean;
}

export interface FeedbackSubmitDTO {
  sessionNo?: string;
  score?: number;
  content?: string;
}

export interface TouristFeedbackVO {
  id: number;
  sessionNo: string | null;
  score: number | null;
  content: string | null;
  emotion: string | null;
  handleStatus: number | null;
  replyContent: string | null;
  replyTime: string | null;
  createTime: string | null;
}
