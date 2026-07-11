package com.guido.scenicai.integration.aliyun;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TtsResult {

    private String audioUrl;
    private String rawResponse;
}
