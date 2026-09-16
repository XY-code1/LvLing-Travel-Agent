package com.guido.scenicai.module.tourist.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guido.scenicai.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("chat_session")
public class ChatSession extends BaseEntity {

    private Long cityId;
    private String sessionNo;
    private Long touristUserId;
    private Long scenicId;
    private Long avatarConfigId;
    private String title;
    private LocalDateTime startTime;
    private LocalDateTime lastTime;
    private Integer messageCount;
}
