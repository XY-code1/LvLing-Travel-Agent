package com.guido.scenicai.module.chat.controller;

import com.guido.scenicai.common.result.Result;
import com.guido.scenicai.module.chat.dto.TextChatDTO;
import com.guido.scenicai.module.chat.service.TouristChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/tourist/chat")
@RequiredArgsConstructor
public class TouristChatController {

    private final TouristChatService touristChatService;

    @PostMapping("/text")
    public Result<?> text(@Valid @RequestBody TextChatDTO dto) {
        return Result.ok(touristChatService.answerText(dto));
    }

    @PostMapping(value = "/text/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter textStream(@Valid @RequestBody TextChatDTO dto) {
        return touristChatService.streamText(dto);
    }

    @PostMapping(value = "/voice", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<?> voice(@RequestParam String sessionNo, @RequestPart MultipartFile audio) {
        return Result.ok(touristChatService.answerVoice(sessionNo, audio));
    }

    @PostMapping(value = "/voice/stream", consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter voiceStream(@RequestParam String sessionNo, @RequestPart MultipartFile audio) {
        return touristChatService.streamVoice(sessionNo, audio);
    }
}
