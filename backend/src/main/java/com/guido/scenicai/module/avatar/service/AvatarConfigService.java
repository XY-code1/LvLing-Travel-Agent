package com.guido.scenicai.module.avatar.service;

import com.guido.scenicai.module.avatar.dto.AvatarSaveDTO;
import com.guido.scenicai.module.avatar.dto.AvatarTestDTO;
import com.guido.scenicai.module.avatar.vo.AvatarConfigVO;
import com.guido.scenicai.module.avatar.vo.AvatarTestVO;

import java.util.List;

public interface AvatarConfigService {

    List<AvatarConfigVO> list();

    AvatarConfigVO save(AvatarSaveDTO dto);

    void updateEnabled(Long id, Integer enabled);

    AvatarTestVO test(AvatarTestDTO dto);

    AvatarConfigVO getDefault();

    AvatarConfigVO getDefaultWithDetail();

    AvatarConfigVO getEnabledById(Long id);

    List<AvatarConfigVO> listEnabled();

    AvatarConfigVO getCurrentForTourist(Long touristId);

    AvatarConfigVO selectForTourist(Long touristId, Long avatarId);

    AvatarConfigVO resolveForTourist(Long touristId, Long requestedAvatarId);
}
