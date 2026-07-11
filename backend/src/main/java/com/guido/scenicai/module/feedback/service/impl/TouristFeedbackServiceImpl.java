package com.guido.scenicai.module.feedback.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guido.scenicai.common.config.StpTouristUtil;
import com.guido.scenicai.module.chat.support.EmotionAnalyzer;
import com.guido.scenicai.module.feedback.dto.FeedbackSubmitDTO;
import com.guido.scenicai.module.feedback.entity.TouristFeedback;
import com.guido.scenicai.module.feedback.mapper.TouristFeedbackMapper;
import com.guido.scenicai.module.feedback.service.TouristFeedbackService;
import com.guido.scenicai.module.feedback.vo.TouristFeedbackVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TouristFeedbackServiceImpl implements TouristFeedbackService {

    private final TouristFeedbackMapper touristFeedbackMapper;
    private final EmotionAnalyzer emotionAnalyzer;

    @Override
    @Transactional
    public void submit(FeedbackSubmitDTO dto) {
        Long touristId = StpTouristUtil.stpLogic.getLoginIdAsLong();
        TouristFeedback feedback = new TouristFeedback();
        feedback.setTouristUserId(touristId);
        feedback.setSessionNo(dto.getSessionNo());
        feedback.setScore(dto.getScore());
        feedback.setContent(dto.getContent());
        feedback.setEmotion(emotionAnalyzer.analyze(dto.getContent()));
        feedback.setHandleStatus(0);
        touristFeedbackMapper.insert(feedback);
    }

    @Override
    public List<TouristFeedbackVO> listMine() {
        Long touristId = StpTouristUtil.stpLogic.getLoginIdAsLong();
        return touristFeedbackMapper.selectList(new LambdaQueryWrapper<TouristFeedback>()
                        .eq(TouristFeedback::getTouristUserId, touristId)
                        .orderByDesc(TouristFeedback::getId)
                        .last("LIMIT 20"))
                .stream()
                .map(this::toVO)
                .toList();
    }

    private TouristFeedbackVO toVO(TouristFeedback feedback) {
        TouristFeedbackVO vo = new TouristFeedbackVO();
        vo.setId(feedback.getId());
        vo.setSessionNo(feedback.getSessionNo());
        vo.setScore(feedback.getScore());
        vo.setContent(feedback.getContent());
        vo.setEmotion(feedback.getEmotion());
        vo.setHandleStatus(feedback.getHandleStatus());
        vo.setReplyContent(feedback.getReplyContent());
        vo.setReplyTime(feedback.getReplyTime());
        vo.setCreateTime(feedback.getCreateTime());
        return vo;
    }
}
