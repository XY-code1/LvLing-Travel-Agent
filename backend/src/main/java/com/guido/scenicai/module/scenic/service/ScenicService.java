package com.guido.scenicai.module.scenic.service;

import com.guido.scenicai.common.result.PageResult;
import com.guido.scenicai.module.scenic.dto.ScenicPageQueryDTO;
import com.guido.scenicai.module.scenic.dto.ScenicSaveDTO;
import com.guido.scenicai.module.scenic.dto.ScenicStatusDTO;
import com.guido.scenicai.module.scenic.dto.ScenicUpdateDTO;
import com.guido.scenicai.module.scenic.vo.ScenicVO;

public interface ScenicService {

    PageResult<ScenicVO> pageQuery(ScenicPageQueryDTO query);

    ScenicVO getDetail(Long id);

    ScenicVO create(ScenicSaveDTO dto);

    ScenicVO modify(ScenicUpdateDTO dto);

    void remove(Long id);

    void changeStatus(ScenicStatusDTO dto);
}
