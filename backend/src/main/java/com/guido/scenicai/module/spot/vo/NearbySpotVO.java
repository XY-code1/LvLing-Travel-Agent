package com.guido.scenicai.module.spot.vo;

import lombok.Data;

@Data
public class NearbySpotVO {

    private Long spotId;
    private String name;
    private Long distanceMeters;
    private String coverImage;
    private Boolean canGuide;
}
