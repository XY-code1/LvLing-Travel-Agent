package com.guido.scenicai.module.avatar.controller;

import com.guido.scenicai.common.result.Result;
import com.guido.scenicai.integration.aliyun.AliyunSpeechClient;
import com.guido.scenicai.module.avatar.dto.TouristTtsDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tourist/tts")
@RequiredArgsConstructor
@Slf4j
public class TouristTtsController {
    private final AliyunSpeechClient speechClient;

    @PostMapping("/synthesize")
    public Result<?> synthesize(@Valid @RequestBody TouristTtsDTO dto) {
        log.info("[TTS Controller] received voiceId={}, received rate={}, received pitch={}",
                dto.getVoiceId(), dto.getSpeechRate(), dto.getPitchRate());
        return Result.ok(speechClient.synthesize(
                dto.getText(), dto.getVoiceId(), dto.getSpeechRate(), dto.getPitchRate()));
    }
}
