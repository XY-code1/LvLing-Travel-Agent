package com.guido.scenicai.module.tourist.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guido.scenicai.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("chat_message")
public class ChatMessage extends BaseEntity {

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
}
