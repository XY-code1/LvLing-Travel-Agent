import { apiGet } from './request';

import type { PageResult, SysLogPageQuery, SysLogVO } from '../types';

export function pageSysLog(params: SysLogPageQuery): Promise<PageResult<SysLogVO>> {
  return apiGet<PageResult<SysLogVO>>('/admin/log/page', params);
}
