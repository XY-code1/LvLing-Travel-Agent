package com.guido.scenicai.module.spot.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class TouristSpotDetailVO extends SpotVO {

    private Boolean canGuide;
}
