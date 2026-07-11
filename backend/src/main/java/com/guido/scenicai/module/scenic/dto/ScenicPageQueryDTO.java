package com.guido.scenicai.module.scenic.dto;

import com.guido.scenicai.common.base.BasePageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ScenicPageQueryDTO extends BasePageQuery {

    private String name;
    private Integer status;
}
