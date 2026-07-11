package com.guido.scenicai.module.tourist.service;

import com.guido.scenicai.common.result.PageResult;
import com.guido.scenicai.module.tourist.dto.AdminTouristUserPageQueryDTO;
import com.guido.scenicai.module.tourist.dto.TouristUserStatusDTO;
import com.guido.scenicai.module.tourist.vo.AdminTouristUserVO;

public interface AdminTouristUserService {

    PageResult<AdminTouristUserVO> page(AdminTouristUserPageQueryDTO query);

    void changeStatus(TouristUserStatusDTO dto);
}
