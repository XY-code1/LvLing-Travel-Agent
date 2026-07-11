package com.guido.scenicai.module.spot.service;

import com.guido.scenicai.common.result.PageResult;
import com.guido.scenicai.module.spot.dto.SpotPageQueryDTO;
import com.guido.scenicai.module.spot.dto.SpotSaveDTO;
import com.guido.scenicai.module.spot.dto.SpotStatusDTO;
import com.guido.scenicai.module.spot.dto.SpotUpdateDTO;
import com.guido.scenicai.module.spot.vo.NearbySpotVO;
import com.guido.scenicai.module.spot.vo.SpotVO;
import com.guido.scenicai.module.spot.vo.TouristHomeHotVO;
import com.guido.scenicai.module.spot.vo.TouristSpotDetailVO;

import java.math.BigDecimal;
import java.util.List;

public interface SpotService {

    PageResult<SpotVO> pageQuery(SpotPageQueryDTO query);

    SpotVO getDetail(Long id);

    TouristSpotDetailVO getTouristDetail(Long id);

    TouristHomeHotVO hotForTourist();

    List<NearbySpotVO> nearbyForTourist(Long scenicId, BigDecimal longitude, BigDecimal latitude);

    SpotVO create(SpotSaveDTO dto);

    SpotVO modify(SpotUpdateDTO dto);

    void remove(Long id);

    void changeStatus(SpotStatusDTO dto);
}
