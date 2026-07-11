package com.guido.scenicai.module.feature.dto;

import com.guido.scenicai.common.base.BasePageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class FeatureItemPageQueryDTO extends BasePageQuery {

    private String moduleType;
    private String scenicName;
    private String keyword;
    private String category;
    private Integer status;
}
