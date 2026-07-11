package com.guido.scenicai.module.chat.vo;

import lombok.Data;

import java.util.List;

@Data
public class RouteRecommendVO {

    private Long routeId;
    private String name;
    private Integer type;
    private Integer estimateMinutes;
    private String recommendReason;
    private List<RouteRecommendSpotVO> spots;
}
