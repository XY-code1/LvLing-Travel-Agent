package com.guido.scenicai.module.sentiment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guido.scenicai.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sentiment_report")
public class SentimentReport extends BaseEntity {

    private LocalDate reportDate;
    private Integer positiveCount;
    private Integer neutralCount;
    private Integer negativeCount;
    private Integer complaintCount;
    private String hotQuestions;
    private String hotSpots;
    private String unanswered;
    private String aiSuggestion;
}
