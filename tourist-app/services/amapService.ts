import { get, post } from '../utils/request';

export interface AmapCoordinate { longitude: number; latitude: number; }
export interface AmapPoi {
  id: string; name: string; address: string; type?: string; distanceMeters?: number | null;
  coordinates: AmapCoordinate;
}
export interface AmapGeocode { formattedAddress: string; coordinates: AmapCoordinate; }
export interface AmapRoute { distanceMeters: number; durationSeconds: number; polyline: AmapCoordinate[]; }
export interface AmapWeather {
  province: string;
  city: string;
  weather: string;
  temperature: string;
  windDirection: string;
  windPower: string;
  humidity: string;
  reportTime: string;
}

function query(value: string): string { return encodeURIComponent(value.trim()); }

export function searchAmapPoi(keywords: string, city: string): Promise<AmapPoi[]> {
  return get(`/api/tourist/amap/poi?keywords=${query(keywords)}&city=${query(city)}`);
}

export function geocodeAmap(address: string, city: string): Promise<AmapGeocode | null> {
  return get(`/api/tourist/amap/geocode?address=${query(address)}&city=${query(city)}`);
}

export function getAmapWeather(city: string): Promise<AmapWeather | null> {
  return get(`/api/tourist/amap/weather?city=${query(city)}`);
}

export function planAmapWalkingRoute(points: AmapCoordinate[]): Promise<AmapRoute> {
  return post('/api/tourist/amap/route/walking', { points });
}
