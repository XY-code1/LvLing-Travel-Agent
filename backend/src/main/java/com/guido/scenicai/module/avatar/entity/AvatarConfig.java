package com.guido.scenicai.module.avatar.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guido.scenicai.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("avatar_config")
public class AvatarConfig extends BaseEntity {

    private String name;
    private String provider;
    private String instanceId;
    private String avatarImage;
    private String gender;
    private String appearance;
    private String outfit;
    private String outfitImage;
    private String renderConfig;
    private String voice;
    private Integer speechRate;
    private String welcomeText;
    private Integer isDefault;
    private Integer enabled;
    private String remark;
}
