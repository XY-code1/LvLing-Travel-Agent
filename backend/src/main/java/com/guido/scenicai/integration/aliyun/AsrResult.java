package com.guido.scenicai.integration.aliyun;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AsrResult {

    private String text;
    private String rawResponse;
}
