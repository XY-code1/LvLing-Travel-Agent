package com.guido.scenicai.module.tourist.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guido.scenicai.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tourist_user")
public class TouristUser extends BaseEntity {

    private String phone;
    private String password;
    private String nickname;
    private String avatar;
    private Integer gender;
    private String interestTags;
    private Long avatarConfigId;
    private Integer status;
    private LocalDateTime lastLoginTime;
}
