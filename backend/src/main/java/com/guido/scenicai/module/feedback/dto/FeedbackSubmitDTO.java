package com.guido.scenicai.module.feedback.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class FeedbackSubmitDTO {

    private String sessionNo;

    @Min(value = 1, message = "评分最小为1")
    @Max(value = 5, message = "评分最大为5")
    private Integer score;

    @Size(max = 1000, message = "反馈内容不能超过1000字")
    private String content;
}
