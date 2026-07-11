package com.guido.scenicai.module.feedback.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guido.scenicai.common.config.StpAdminUtil;
import com.guido.scenicai.common.exception.BizException;
import com.guido.scenicai.common.result.PageResult;
import com.guido.scenicai.module.feedback.dto.AdminFeedbackPageQueryDTO;
import com.guido.scenicai.module.feedback.dto.FeedbackReplyDTO;
import com.guido.scenicai.module.feedback.entity.TouristFeedback;
import com.guido.scenicai.module.feedback.mapper.TouristFeedbackMapper;
import com.guido.scenicai.module.feedback.service.AdminFeedbackService;
import com.guido.scenicai.module.feedback.vo.AdminFeedbackVO;
import com.guido.scenicai.module.tourist.entity.TouristUser;
import com.guido.scenicai.module.tourist.mapper.TouristUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminFeedbackServiceImpl implements AdminFeedbackService {

    private final TouristFeedbackMapper touristFeedbackMapper;
    private final TouristUserMapper touristUserMapper;

    @Override
    public PageResult<AdminFeedbackVO> page(AdminFeedbackPageQueryDTO query) {
        LambdaQueryWrapper<TouristFeedback> wrapper = new LambdaQueryWrapper<>();
        if (query.getScore() != null) {
            wrapper.eq(TouristFeedback::getScore, query.getScore());
        }
        if (StringUtils.hasText(query.getEmotion())) {
            wrapper.eq(TouristFeedback::getEmotion, query.getEmotion());
        }
        if (query.getHandleStatus() != null) {
            wrapper.eq(TouristFeedback::getHandleStatus, query.getHandleStatus());
        }
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.like(TouristFeedback::getContent, query.getKeyword());
        }
        wrapper.orderByDesc(TouristFeedback::getId);
        Page<TouristFeedback> page = touristFeedbackMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        Map<Long, TouristUser> userMap = loadUserMap(page.getRecords());
        List<AdminFeedbackVO> records = page.getRecords().stream()
                .map(item -> toVO(item, userMap.get(item.getTouristUserId())))
                .toList();
        return PageResult.of(page.getTotal(), query.getPageNum(), query.getPageSize(), records);
    }

    @Override
    @Transactional
    public void reply(FeedbackReplyDTO dto) {
        TouristFeedback feedback = touristFeedbackMapper.selectById(dto.getId());
        if (feedback == null) {
            throw BizException.notFound("游客反馈不存在");
        }
        feedback.setHandleStatus(dto.getHandleStatus());
        feedback.setReplyContent(dto.getReplyContent());
        feedback.setReplyTime(LocalDateTime.now());
        feedback.setHandler(String.valueOf(StpAdminUtil.stpLogic.getLoginId()));
        touristFeedbackMapper.updateById(feedback);
    }

    private Map<Long, TouristUser> loadUserMap(List<TouristFeedback> feedbacks) {
        List<Long> userIds = feedbacks.stream()
                .map(TouristFeedback::getTouristUserId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return touristUserMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(TouristUser::getId, item -> item, (left, right) -> left));
    }

    private AdminFeedbackVO toVO(TouristFeedback feedback, TouristUser user) {
        AdminFeedbackVO vo = new AdminFeedbackVO();
        vo.setId(feedback.getId());
        vo.setTouristUserId(feedback.getTouristUserId());
        vo.setTouristName(user == null ? null : user.getNickname());
        vo.setTouristPhone(user == null ? null : user.getPhone());
        vo.setSessionNo(feedback.getSessionNo());
        vo.setScore(feedback.getScore());
        vo.setContent(feedback.getContent());
        vo.setEmotion(feedback.getEmotion());
        vo.setHandleStatus(feedback.getHandleStatus());
        vo.setReplyContent(feedback.getReplyContent());
        vo.setReplyTime(feedback.getReplyTime());
        vo.setHandler(feedback.getHandler());
        vo.setCreateTime(feedback.getCreateTime());
        vo.setUpdateTime(feedback.getUpdateTime());
        return vo;
    }
}
