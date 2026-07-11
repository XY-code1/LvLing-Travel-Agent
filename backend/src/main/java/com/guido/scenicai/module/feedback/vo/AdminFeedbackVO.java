package com.guido.scenicai.module.feedback.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminFeedbackVO {

    private Long id;
    private Long touristUserId;
    private String touristName;
    private String touristPhone;
    private String sessionNo;
    private Integer score;
    private String content;
    private String emotion;
    private Integer handleStatus;
    private String replyContent;
    private LocalDateTime replyTime;
    private String handler;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
