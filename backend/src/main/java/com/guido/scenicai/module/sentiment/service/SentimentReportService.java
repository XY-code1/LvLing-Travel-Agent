package com.guido.scenicai.module.sentiment.service;

import com.guido.scenicai.module.sentiment.dto.SentimentGenerateDTO;
import com.guido.scenicai.module.sentiment.vo.SentimentReportVO;

public interface SentimentReportService {

    SentimentReportVO generate(SentimentGenerateDTO dto);

    SentimentReportVO report(String date);

    byte[] generateDocx(SentimentGenerateDTO dto);
}
