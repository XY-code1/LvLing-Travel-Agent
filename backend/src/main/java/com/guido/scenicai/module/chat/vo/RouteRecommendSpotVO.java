package com.guido.scenicai.module.chat.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class RouteRecommendSpotVO {

    private Long spotId;
    private String name;
    private Integer sortOrder;
    private BigDecimal longitude;
    private BigDecimal latitude;
}
