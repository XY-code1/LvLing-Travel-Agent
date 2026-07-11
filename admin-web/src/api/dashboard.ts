import { apiGet, apiPut } from './request';

import type { DashboardOverviewVO } from '../types';

export function getDashboardOverview(): Promise<DashboardOverviewVO> {
  return apiGet<DashboardOverviewVO>('/admin/dashboard/overview');
}

export function updateDemoSwitch(switchKey: string, enabled: boolean): Promise<null> {
  return apiPut<null>('/admin/dashboard/demo-switch', { switchKey, enabled });
}
