package com.guido.scenicai.module.feedback.controller;

import com.guido.scenicai.common.result.Result;
import com.guido.scenicai.module.feedback.dto.FeedbackSubmitDTO;
import com.guido.scenicai.module.feedback.service.TouristFeedbackService;
import com.guido.scenicai.module.feedback.vo.TouristFeedbackVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tourist/feedback")
@RequiredArgsConstructor
public class TouristFeedbackController {

    private final TouristFeedbackService touristFeedbackService;

    @PostMapping("/submit")
    public Result<?> submit(@Valid @RequestBody FeedbackSubmitDTO dto) {
        touristFeedbackService.submit(dto);
        return Result.ok();
    }

    @GetMapping("/mine")
    public Result<List<TouristFeedbackVO>> mine() {
        return Result.ok(touristFeedbackService.listMine());
    }
}
