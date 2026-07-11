package com.guido.scenicai.module.tourist.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TouristLoginVO {

    private String token;
    private TouristInfoVO touristInfo;
}
