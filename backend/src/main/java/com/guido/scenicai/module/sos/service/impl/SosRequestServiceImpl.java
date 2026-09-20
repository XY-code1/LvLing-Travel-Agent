package com.guido.scenicai.module.sos.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guido.scenicai.common.config.StpTouristUtil;
import com.guido.scenicai.common.exception.BizException;
import com.guido.scenicai.module.sos.dto.SosCreateDTO;
import com.guido.scenicai.module.sos.entity.SosRequest;
import com.guido.scenicai.module.sos.mapper.SosRequestMapper;
import com.guido.scenicai.module.sos.service.SosRequestService;
import com.guido.scenicai.module.sos.vo.SosRequestVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service @RequiredArgsConstructor
public class SosRequestServiceImpl implements SosRequestService {
    private static final Set<String> HELP_TYPES = Set.of("LOST", "MEDICAL", "ITEM_LOST", "FACILITY", "TRAFFIC", "SAFETY", "MISSING_PERSON", "OTHER");
    private static final Set<String> URGENCIES = Set.of("NORMAL", "URGENT", "EMERGENCY");
    private final SosRequestMapper mapper;
    @Override @Transactional public SosRequestVO create(SosCreateDTO dto) {
        if (!HELP_TYPES.contains(dto.getHelpType())) throw new BizException(400, "SOS_HELP_TYPE_INVALID");
        if (!URGENCIES.contains(dto.getUrgency())) throw new BizException(400, "SOS_URGENCY_INVALID");
        SosRequest entity = new SosRequest(); BeanUtils.copyProperties(dto, entity);
        entity.setRequestNo(nextRequestNo()); entity.setStatus("PENDING");
        entity.setUserId(StpTouristUtil.stpLogic.isLogin() ? StpTouristUtil.stpLogic.getLoginIdAsLong() : null);
        mapper.insert(entity); return toVO(entity);
    }
    @Override public SosRequestVO get(String requestNo) {
        SosRequest entity = mapper.selectOne(new LambdaQueryWrapper<SosRequest>().eq(SosRequest::getRequestNo, requestNo).last("LIMIT 1"));
        if (entity == null) throw new BizException(404, "SOS_REQUEST_NOT_FOUND"); return toVO(entity);
    }
    private String nextRequestNo() { return "SOS-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase(Locale.ROOT); }
    private SosRequestVO toVO(SosRequest value) { SosRequestVO vo = new SosRequestVO(); BeanUtils.copyProperties(value, vo); vo.setCreatedAt(value.getCreateTime()); vo.setUpdatedAt(value.getUpdateTime()); return vo; }
}
