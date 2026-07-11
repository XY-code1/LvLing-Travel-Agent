package com.guido.scenicai.module.route.dto;

import com.guido.scenicai.common.base.BasePageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class RoutePageQueryDTO extends BasePageQuery {

    private Long scenicId;
    private String scenicName;
    private String name;
    private Integer type;
    private Integer status;
}
