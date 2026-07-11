package com.guido.scenicai.module.chat.controller;

import com.guido.scenicai.common.result.Result;
import com.guido.scenicai.module.chat.service.TouristChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/tourist/vision")
@RequiredArgsConstructor
public class TouristVisionController {

    private final TouristChatService touristChatService;

    @PostMapping(value = "/recognize", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<?> recognize(@RequestParam String sessionNo, @RequestPart MultipartFile image) {
        return Result.ok(touristChatService.recognizeImage(sessionNo, image));
    }
}
