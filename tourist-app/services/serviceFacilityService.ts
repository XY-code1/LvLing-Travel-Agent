import type { CityContextVO, CityServiceVO } from '../types';
import { searchAmapPoi } from './amapService';

export type FacilitySource = 'local' | 'amap' | 'fallback' | 'demo';
export type FacilityStatus = 'OPEN' | 'BUSY' | 'ALL_DAY' | 'CLOSED' | 'UNKNOWN';

export interface ServiceFacility {
  id: string;
  name: string;
  type: string;
  description: string;
  status: FacilityStatus;
  location: string;
  distance: number | null;
  walkingTime: number | null;
  openingHours: string;
  latitude: number | null;
  longitude: number | null;
  cityCode: string;
  scenicId: number | null;
  source: FacilitySource;
  updatedAt: string | null;
}

const memoryCache = new Map<string, ServiceFacility[]>();

export async function loadServiceFacilities(context: CityContextVO | null): Promise<ServiceFacility[]> {
  const city = context?.city;
  const key = `${city?.cityCode || context?.adcode || 'unknown'}:${context?.scenicAreas?.[0]?.id || 'city'}:all`;
  const cached = memoryCache.get(key);
  if (cached) return cached;
  let services = context?.services || [];
  if (!services.length && city?.cityName) {
    const queries = [['公共厕所', '卫生间'], ['餐厅', '餐厅'], ['停车场', '停车场'],
      ['游客服务中心', '游客服务中心'], ['医院', '医务室'], ['充电站', '充电设施']] as const;
    let groups: CityServiceVO[][] = [];
    try { groups = await Promise.all(queries.map(async ([keyword, category]) =>
      (await searchAmapPoi(keyword, city.cityName)).slice(0, 6).map((poi) => ({
        id: poi.id, name: poi.name, category, address: poi.address || null,
        longitude: poi.coordinates.longitude, latitude: poi.coordinates.latitude,
        source: 'amap' as const, distanceMeters: poi.distanceMeters ?? null
      })))); services = groups.flat(); } catch (error) { console.warn('[DataSource] source=demo domain=services reason=provider-failed', error); services = demoHangzhouServices(); }
  }
  const result = services.map((item) => fromContext(item, context));
  memoryCache.set(key, result);
  return result;
}

function demoHangzhouServices(): CityServiceVO[] { return [['demo-service-restaurant','西湖湖畔餐厅','餐厅','西湖景区附近'],['demo-service-parking','西湖景区停车场','停车场','北山街游客停车区'],['demo-service-restroom','西湖公共卫生间','卫生间','苏堤入口附近'],['demo-service-hospital','西湖景区医疗点','医院/医疗','曲院风荷游客服务点']].map(([id,name,category,address]) => ({ id, name, category, address, longitude: 120.1551, latitude: 30.2741, source: 'demo' })); }

function fromContext(value: CityServiceVO, context: CityContextVO): ServiceFacility {
  const legacy = value as CityServiceVO & { title?: string; content?: string; distanceMeters?: number | null };
  const name = value.name || legacy.title || '服务设施';
  const type = normalizeType(value.category || name);
  return {
    id: value.id,
    name,
    type,
    description: descriptionFor(type, name),
    status: statusFor(type, name),
    location: value.address || legacy.content || '地址信息暂缺',
    distance: legacy.distanceMeters ?? null,
    walkingTime: null,
    openingHours: value.source === 'amap' ? '请以现场营业信息为准' : '以景区当日公告为准',
    latitude: value.latitude,
    longitude: value.longitude,
    cityCode: context.city?.cityCode || context.adcode || '',
    scenicId: context.scenicAreas?.[0]?.id || null,
    source: value.source || 'local',
    updatedAt: null
  };
}

function normalizeType(value: string): string {
  const match = ['卫生间', '餐厅', '停车场', '游客服务中心', '医务室', '母婴室', '充电宝租借点', '观光车站点', '售票处', '休息区', '饮水点', '纪念品商店'].find((item) => value.includes(item));
  if (match) return match;
  if (/医院|医疗/.test(value)) return '医务室';
  if (/厕所|洗手间/.test(value)) return '卫生间';
  if (/餐饮|美食/.test(value)) return '餐厅';
  return '其他设施';
}

function statusFor(type: string, name: string): FacilityStatus {
  if (/关闭|暂停/.test(name)) return 'CLOSED';
  if (/24小时|停车场/.test(name) || type === '停车场') return 'ALL_DAY';
  return 'UNKNOWN';
}

function descriptionFor(type: string, name: string): string {
  const descriptions: Record<string, string> = {
    卫生间: '公共卫生设施，具体开放状态以现场标识为准。', 餐厅: '提供餐饮服务，菜单与营业时间以现场为准。',
    停车场: '服务自驾游客与旅游车辆，收费以现场公示为准。', 游客服务中心: '提供咨询、导览与游客服务；具体服务项目以现场为准。',
    医务室: '提供基础医疗咨询与紧急情况协助。'
  };
  return descriptions[type] || `${name}服务信息，具体项目与开放情况以现场公告为准。`;
}
