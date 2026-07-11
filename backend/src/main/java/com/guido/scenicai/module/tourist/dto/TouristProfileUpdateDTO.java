package com.guido.scenicai.module.tourist.dto;

import lombok.Data;

@Data
public class TouristProfileUpdateDTO {

    private String nickname;
    private Integer gender;
    private String interestTags;
}
