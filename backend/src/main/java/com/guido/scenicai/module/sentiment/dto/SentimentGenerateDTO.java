package com.guido.scenicai.module.sentiment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SentimentGenerateDTO {

    @NotBlank(message = "日期不能为空")
    private String date;
}
