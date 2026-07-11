package com.guido.scenicai.module.avatar.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AvatarConfigVO {

    private Long id;
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
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
