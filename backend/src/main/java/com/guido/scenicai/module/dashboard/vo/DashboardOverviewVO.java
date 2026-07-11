package com.guido.scenicai.module.dashboard.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class DashboardOverviewVO {

    private TodayStatVO today;
    private List<Map<String, Object>> hotQuestions;
    private List<Map<String, Object>> hotSpots;
    private Map<String, Integer> emotionDist;
    private List<TrendVO> trend7d;
    private Integer demo;
}
