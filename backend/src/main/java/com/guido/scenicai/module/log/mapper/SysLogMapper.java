package com.guido.scenicai.module.log.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guido.scenicai.module.log.entity.SysLogEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * Mapper for sys_log table.
 */
@Mapper
public interface SysLogMapper extends BaseMapper<SysLogEntity> {
}
