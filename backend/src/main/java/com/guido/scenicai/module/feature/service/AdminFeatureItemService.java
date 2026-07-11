package com.guido.scenicai.module.feature.service;

import com.guido.scenicai.common.result.PageResult;
import com.guido.scenicai.module.feature.dto.FeatureItemPageQueryDTO;
import com.guido.scenicai.module.feature.dto.FeatureItemSaveDTO;
import com.guido.scenicai.module.feature.dto.FeatureItemStatusDTO;
import com.guido.scenicai.module.feature.dto.FeatureItemUpdateDTO;
import com.guido.scenicai.module.feature.vo.FeatureItemVO;

public interface AdminFeatureItemService {

    PageResult<FeatureItemVO> page(FeatureItemPageQueryDTO query);

    FeatureItemVO create(FeatureItemSaveDTO dto);

    FeatureItemVO modify(FeatureItemUpdateDTO dto);

    void remove(Long id);

    void changeStatus(FeatureItemStatusDTO dto);
}
