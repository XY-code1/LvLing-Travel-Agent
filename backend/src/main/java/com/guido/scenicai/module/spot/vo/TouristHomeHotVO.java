package com.guido.scenicai.module.spot.vo;

import lombok.Data;

import java.util.List;

@Data
public class TouristHomeHotVO {

    private List<HotSpotVO> hotSpots;
    private List<String> recommendQuestions;
}
