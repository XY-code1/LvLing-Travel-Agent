package com.guido.scenicai.module.sentiment.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class SentimentReportVO {

    private Long id;
    private LocalDate reportDate;
    private Integer positiveCount;
    private Integer neutralCount;
    private Integer negativeCount;
    private Integer complaintCount;
    private String hotQuestions;
    private String hotSpots;
    private String unanswered;
    private String aiSuggestion;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
