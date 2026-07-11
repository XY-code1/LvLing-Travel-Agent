package com.guido.scenicai.module.spot.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SpotVO {

    private Long id;
    private Long scenicId;
    private String scenicName;
    private String name;
    private String alias;
    private String intro;
    private String historyCulture;
    private String guideText;
    private String tags;
    private String images;
    private Integer stayMinutes;
    private String suitCrowd;
    private Integer isHot;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
