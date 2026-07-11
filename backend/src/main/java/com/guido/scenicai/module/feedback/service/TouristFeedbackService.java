package com.guido.scenicai.module.feedback.service;

import com.guido.scenicai.module.feedback.dto.FeedbackSubmitDTO;
import com.guido.scenicai.module.feedback.vo.TouristFeedbackVO;

import java.util.List;

public interface TouristFeedbackService {

    void submit(FeedbackSubmitDTO dto);

    List<TouristFeedbackVO> listMine();
}
