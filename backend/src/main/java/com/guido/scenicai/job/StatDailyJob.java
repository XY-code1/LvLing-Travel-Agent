package com.guido.scenicai.job;

import com.guido.scenicai.module.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class StatDailyJob {

    private final DashboardService dashboardService;

    @Scheduled(cron = "0 5 0 * * ?")
    public void generateYesterdayStat() {
        dashboardService.generateDailyStat(LocalDate.now().minusDays(1));
    }
}
