package com.guido.scenicai.module.sentiment.controller;

import com.guido.scenicai.common.interceptor.OperationLog;
import com.guido.scenicai.common.result.Result;
import com.guido.scenicai.module.sentiment.dto.SentimentGenerateDTO;
import com.guido.scenicai.module.sentiment.service.SentimentReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/admin/sentiment")
@RequiredArgsConstructor
public class SentimentReportController {

    private final SentimentReportService sentimentReportService;

    @PostMapping("/generate")
    @OperationLog("生成感受度分析报告")
    public Result<?> generate(@Valid @RequestBody SentimentGenerateDTO dto) {
        return Result.ok(sentimentReportService.generate(dto));
    }

    @GetMapping("/report")
    public Result<?> report(@RequestParam String date) {
        return Result.ok(sentimentReportService.report(date));
    }

    @PostMapping("/generate-docx")
    @OperationLog("生成感受度 Word 报告")
    public ResponseEntity<byte[]> generateDocx(@Valid @RequestBody SentimentGenerateDTO dto) {
        byte[] body = sentimentReportService.generateDocx(dto);
        String fileName = "感受度分析报告-" + dto.getDate() + ".docx";
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(fileName, StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .body(body);
    }
}
