package com.guido.scenicai.module.feedback.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TouristFeedbackVO {

    private Long id;
    private String sessionNo;
    private Integer score;
    private String content;
    private String emotion;
    private Integer handleStatus;
    private String replyContent;
    private LocalDateTime replyTime;
    private LocalDateTime createTime;
}
