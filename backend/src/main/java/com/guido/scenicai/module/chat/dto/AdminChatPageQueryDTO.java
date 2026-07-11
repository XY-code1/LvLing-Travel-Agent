package com.guido.scenicai.module.chat.dto;

import com.guido.scenicai.common.base.BasePageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AdminChatPageQueryDTO extends BasePageQuery {

    private String startTime;
    private String endTime;
    private String inputType;
    private String emotion;
    private Integer success;
    private String keyword;
}
