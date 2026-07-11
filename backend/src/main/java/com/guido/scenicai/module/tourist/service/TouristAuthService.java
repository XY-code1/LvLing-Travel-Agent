package com.guido.scenicai.module.tourist.service;

import com.guido.scenicai.module.tourist.dto.TouristLoginDTO;
import com.guido.scenicai.module.tourist.dto.TouristRegisterDTO;
import com.guido.scenicai.module.tourist.vo.TouristLoginVO;

public interface TouristAuthService {

    /**
     * 游客注册，返回游客 ID。
     */
    Long register(TouristRegisterDTO dto);

    /**
     * 游客登录，返回 token + 游客信息。
     */
    TouristLoginVO login(TouristLoginDTO dto);

    /**
     * 游客退出。
     */
    void logout();
}
