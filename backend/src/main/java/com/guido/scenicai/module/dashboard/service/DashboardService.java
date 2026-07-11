package com.guido.scenicai.module.dashboard.service;

import com.guido.scenicai.module.dashboard.dto.DemoSwitchDTO;
import com.guido.scenicai.module.dashboard.vo.DashboardOverviewVO;

import java.time.LocalDate;

public interface DashboardService {

    DashboardOverviewVO overview();

    void updateDemoSwitch(DemoSwitchDTO dto);

    void generateDailyStat(LocalDate date);
}
