package com.guido.scenicai.module.tourist.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminTouristUserVO {

    private Long id;
    private String phone;
    private String nickname;
    private String avatar;
    private Integer gender;
    private String interestTags;
    private Integer status;
    private LocalDateTime lastLoginTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
