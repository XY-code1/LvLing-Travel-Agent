package com.guido.scenicai.module.tourist.vo;

import lombok.Data;

/**
 * 游客完整资料。
 * ⛔ 不包含 password 字段。
 */
@Data
public class TouristProfileVO {

    private Long id;
    private String phone;
    private String nickname;
    private String avatar;
    private Integer gender;
    private String interestTags;
    private Long avatarConfigId;
}
