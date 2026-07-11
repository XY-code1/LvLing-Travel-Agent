package com.guido.scenicai.module.log.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.guido.scenicai.common.result.PageResult;
import com.guido.scenicai.module.log.dto.SysLogPageQueryDTO;
import com.guido.scenicai.module.log.entity.SysLogEntity;
import com.guido.scenicai.module.log.vo.AiLogStatVO;
import com.guido.scenicai.module.log.vo.SysLogVO;

import java.util.List;

/**
 * Service interface for sys_log.
 */
public interface SysLogService extends IService<SysLogEntity> {

    /**
     * Paginated log query with filters.
     */
    PageResult<SysLogVO> pageQuery(SysLogPageQueryDTO query);

    List<AiLogStatVO> aiStat(SysLogPageQueryDTO query);
}
