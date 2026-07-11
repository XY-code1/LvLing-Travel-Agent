package com.guido.scenicai.module.chat.vo;

import lombok.Data;

import java.util.List;

@Data
public class VisionRecognizeVO {

    private Long messageId;
    private RecognizedSpotVO recognizedSpot;
    private String answer;
    private Integer hitKb;
    private List<SourceVO> sources;
    private String emotion;
    private String streamUrl;
    private String audioUrl;
    private Integer costMs;
}
