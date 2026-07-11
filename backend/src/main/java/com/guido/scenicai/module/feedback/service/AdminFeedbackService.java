package com.guido.scenicai.module.feedback.service;

import com.guido.scenicai.common.result.PageResult;
import com.guido.scenicai.module.feedback.dto.AdminFeedbackPageQueryDTO;
import com.guido.scenicai.module.feedback.dto.FeedbackReplyDTO;
import com.guido.scenicai.module.feedback.vo.AdminFeedbackVO;

public interface AdminFeedbackService {

    PageResult<AdminFeedbackVO> page(AdminFeedbackPageQueryDTO query);

    void reply(FeedbackReplyDTO dto);
}
