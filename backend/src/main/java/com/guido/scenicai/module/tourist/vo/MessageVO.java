package com.guido.scenicai.module.tourist.vo;

import lombok.Data;

@Data
public class MessageVO {

    private Long messageId;
    private String sessionNo;
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
    private String createTime;
}
