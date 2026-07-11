package com.guido.scenicai.module.chat.service;

import com.guido.scenicai.module.chat.vo.RouteRecommendVO;

import java.util.List;

public interface TouristRouteRecommendService {

    List<RouteRecommendVO> recommend(Long scenicId, String interest, Integer routeType);
}
