package com.guido.scenicai.module.log.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.guido.scenicai.common.exception.BizException;
import com.guido.scenicai.common.result.PageResult;
import com.guido.scenicai.module.log.dto.SysLogPageQueryDTO;
import com.guido.scenicai.module.log.entity.SysLogEntity;
import com.guido.scenicai.module.log.mapper.SysLogMapper;
import com.guido.scenicai.module.log.service.SysLogService;
import com.guido.scenicai.module.log.vo.AiLogStatVO;
import com.guido.scenicai.module.log.vo.SysLogVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * SysLog service implementation.
 */
@Slf4j
@Service
public class SysLogServiceImpl extends ServiceImpl<SysLogMapper, SysLogEntity> implements SysLogService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public PageResult<SysLogVO> pageQuery(SysLogPageQueryDTO query) {
        LambdaQueryWrapper<SysLogEntity> wrapper = buildWrapper(query);
        wrapper.orderByDesc(SysLogEntity::getId);

        Page<SysLogEntity> page = page(
                new Page<>(query.getPageNum(), query.getPageSize()),
                wrapper
        );

        List<SysLogVO> records = page.getRecords().stream()
                .map(this::toVO)
                .toList();

        return PageResult.of(
                page.getTotal(),
                query.getPageNum(),
                query.getPageSize(),
                records
        );
    }

    @Override
    public List<AiLogStatVO> aiStat(SysLogPageQueryDTO query) {
        List<SysLogEntity> logs = list(buildWrapper(query));
        return logs.stream()
                .filter(log -> StringUtils.hasText(log.getLogType()))
                .filter(log -> !"LOGIN".equals(log.getLogType()) && !"OPERATION".equals(log.getLogType()))
                .collect(Collectors.groupingBy(this::statKey))
                .values().stream()
                .map(this::toStatVO)
                .toList();
    }

    private LambdaQueryWrapper<SysLogEntity> buildWrapper(SysLogPageQueryDTO query) {
        LambdaQueryWrapper<SysLogEntity> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getLogType())) {
            wrapper.eq(SysLogEntity::getLogType, query.getLogType());
        }
        if (query.getSuccess() != null) {
            wrapper.eq(SysLogEntity::getSuccess, query.getSuccess());
        }
        if (StringUtils.hasText(query.getOperator())) {
            wrapper.like(SysLogEntity::getOperator, query.getOperator());
        }
        if (StringUtils.hasText(query.getStartTime())) {
            wrapper.ge(SysLogEntity::getCreateTime, parseDateTime(query.getStartTime()));
        }
        if (StringUtils.hasText(query.getEndTime())) {
            wrapper.le(SysLogEntity::getCreateTime, parseDateTime(query.getEndTime()));
        }
        return wrapper;
    }

    private String statKey(SysLogEntity log) {
        String provider = StringUtils.hasText(log.getServiceProvider()) ? log.getServiceProvider() : "UNKNOWN";
        return log.getLogType() + "#" + provider;
    }

    private AiLogStatVO toStatVO(List<SysLogEntity> logs) {
        SysLogEntity first = logs.get(0);
        AiLogStatVO vo = new AiLogStatVO();
        vo.setLogType(first.getLogType());
        vo.setServiceProvider(StringUtils.hasText(first.getServiceProvider()) ? first.getServiceProvider() : "UNKNOWN");
        vo.setTotalCount((long) logs.size());
        vo.setSuccessCount(logs.stream().filter(log -> Objects.equals(log.getSuccess(), 1)).count());
        vo.setFailureCount(logs.stream().filter(log -> Objects.equals(log.getSuccess(), 0)).count());
        vo.setAvgCostMs((int) Math.round(logs.stream()
                .map(SysLogEntity::getCostMs)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0)));
        vo.setMaxCostMs(logs.stream()
                .map(SysLogEntity::getCostMs)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0));
        return vo;
    }

    private LocalDateTime parseDateTime(String timeStr) {
        try {
            return LocalDateTime.parse(timeStr, FORMATTER);
        } catch (DateTimeParseException e) {
            log.warn("时间格式解析失败: {}", timeStr, e);
            throw new BizException(400, "时间格式必须为 yyyy-MM-dd HH:mm:ss");
        }
    }

    private SysLogVO toVO(SysLogEntity entity) {
        SysLogVO vo = new SysLogVO();
        vo.setId(entity.getId());
        vo.setLogType(entity.getLogType());
        vo.setBizDesc(entity.getBizDesc());
        vo.setOperator(entity.getOperator());
        vo.setServiceProvider(entity.getServiceProvider());
        vo.setRequestSummary(entity.getRequestSummary());
        vo.setResponseSummary(entity.getResponseSummary());
        vo.setCostMs(entity.getCostMs());
        vo.setSuccess(entity.getSuccess());
        vo.setErrorMsg(entity.getErrorMsg());
        vo.setIp(entity.getIp());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }
}
