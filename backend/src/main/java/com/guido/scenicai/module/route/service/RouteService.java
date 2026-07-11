package com.guido.scenicai.module.route.service;

import com.guido.scenicai.common.result.PageResult;
import com.guido.scenicai.module.route.dto.RoutePageQueryDTO;
import com.guido.scenicai.module.route.dto.RouteSaveDTO;
import com.guido.scenicai.module.route.dto.RouteSpotsDTO;
import com.guido.scenicai.module.route.dto.RouteStatusDTO;
import com.guido.scenicai.module.route.dto.RouteUpdateDTO;
import com.guido.scenicai.module.route.vo.RouteVO;

public interface RouteService {

    PageResult<RouteVO> pageQuery(RoutePageQueryDTO query);

    RouteVO getDetail(Long id);

    RouteVO create(RouteSaveDTO dto);

    RouteVO modify(RouteUpdateDTO dto);

    void remove(Long id);

    void changeStatus(RouteStatusDTO dto);

    void configureSpots(RouteSpotsDTO dto);
}
