package com.guido.scenicai.integration.aliyun;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AvatarStreamResult {

    private String streamUrl;
    private String rawResponse;
}
