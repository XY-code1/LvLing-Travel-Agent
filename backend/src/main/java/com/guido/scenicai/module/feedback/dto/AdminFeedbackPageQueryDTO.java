package com.guido.scenicai.module.feedback.dto;

import com.guido.scenicai.common.base.BasePageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AdminFeedbackPageQueryDTO extends BasePageQuery {

    private Integer score;
    private String emotion;
    private Integer handleStatus;
    private String keyword;
}
