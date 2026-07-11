package com.guido.scenicai.module.tourist.service;

import com.guido.scenicai.module.tourist.dto.TouristProfileUpdateDTO;
import com.guido.scenicai.module.tourist.vo.TouristProfileVO;
import org.springframework.web.multipart.MultipartFile;

public interface TouristUserService {

    /**
     * 获取当前登录游客的完整资料。
     */
    TouristProfileVO getProfile();

    /**
     * 修改当前登录游客的资料（昵称/性别/兴趣标签）。
     */
    TouristProfileVO updateProfile(TouristProfileUpdateDTO dto);

    /**
     * 上传并更新当前登录游客头像。
     */
    TouristProfileVO uploadAvatar(MultipartFile file);
}
