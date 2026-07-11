package com.guido.scenicai.module.tourist.dto;

import com.guido.scenicai.common.base.BasePageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AdminTouristUserPageQueryDTO extends BasePageQuery {

    private String phone;
    private String nickname;
    private Integer status;
}
