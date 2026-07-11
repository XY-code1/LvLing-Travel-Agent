package com.guido.scenicai.module.dashboard.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrendVO {

    private String date;
    private Integer serviceCount;
    private BigDecimal kbHitRate;
}
