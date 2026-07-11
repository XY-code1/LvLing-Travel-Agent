package com.guido.scenicai.module.tourist.vo;

import lombok.Data;

/**
 * 游客简要信息（登录返回和列表展示用）。
 * ⛔ 不包含 password 字段。
 */
@Data
public class TouristInfoVO {

    private Long id;
    private String phone;
    private String nickname;
    private String interestTags;
}
