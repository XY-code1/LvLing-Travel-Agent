package com.guido.scenicai.module.scenic.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ScenicVO {

    private Long id;
    private String name;
    private String intro;
    private String address;
    private String openTime;
    private String ticketInfo;
    private String trafficInfo;
    private String servicePhone;
    private String notice;
    private String coverImage;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
