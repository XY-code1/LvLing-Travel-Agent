package com.guido.scenicai.module.spot.dto;

import com.guido.scenicai.common.base.BasePageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SpotPageQueryDTO extends BasePageQuery {

    private Long scenicId;
    private String scenicName;
    private String name;
    private String tag;
    private Integer status;
    private Integer isHot;
}
