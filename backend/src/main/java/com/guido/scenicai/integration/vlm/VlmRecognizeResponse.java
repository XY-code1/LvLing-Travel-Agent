package com.guido.scenicai.integration.vlm;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VlmRecognizeResponse {

    private String content;
    private String rawResponse;
}
