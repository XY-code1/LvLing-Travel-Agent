package com.guido.scenicai.module.route.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class RouteVO {

    private Long id;
    private Long scenicId;
    private String scenicName;
    private String name;
    private Integer type;
    private String intro;
    private Integer estimateMinutes;
    private String suitCrowd;
    private String interestTags;
    private String recommendReason;
    private String notice;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private List<RouteSpotVO> spots;
}
