package com.guido.scenicai.integration.vlm;

import com.guido.scenicai.integration.common.AiConfigSnapshot;

public interface VlmClient {

    String protocol();

    VlmRecognizeResponse recognize(AiConfigSnapshot config, VlmRecognizeRequest request);
}
