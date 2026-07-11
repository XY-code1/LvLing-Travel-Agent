package com.guido.scenicai.module.feedback.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guido.scenicai.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tourist_feedback")
public class TouristFeedback extends BaseEntity {

    private Long touristUserId;
    private String sessionNo;
    private Integer score;
    private String content;
    private String emotion;
    private Integer handleStatus;
    private String replyContent;
    private LocalDateTime replyTime;
    private String handler;
}
