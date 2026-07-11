package com.guido.scenicai.module.chat.vo;

import lombok.Data;

@Data
public class RecognizedSpotVO {

    private Long spotId;
    private String spotName;
    private Double confidence;
    private String name;
    private String intro;
}
