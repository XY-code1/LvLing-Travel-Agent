package com.guido.scenicai.module.chat.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminChatMessageVO {

    private Long id;
    private Long sessionId;
    private String sessionNo;
    private Long touristUserId;
    private String inputType;
    private String question;
    private String asrText;
    private String imageUrl;
    private String answer;
    private String sources;
    private Integer hitKb;
    private String emotion;
    private String audioUrl;
    private String streamUrl;
    private Integer costMs;
    private Integer success;
    private String errorMsg;
    private Integer needSupplement;
    private LocalDateTime createTime;
}
