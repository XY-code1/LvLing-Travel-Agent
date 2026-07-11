package com.guido.scenicai.module.sentiment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guido.scenicai.module.sentiment.entity.SentimentReport;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SentimentReportMapper extends BaseMapper<SentimentReport> {
}
