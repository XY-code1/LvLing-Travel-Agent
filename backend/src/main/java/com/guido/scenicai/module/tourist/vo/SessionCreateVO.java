package com.guido.scenicai.module.tourist.vo;

import com.guido.scenicai.module.avatar.vo.AvatarConfigVO;
import lombok.Data;

/**
 * 创建会话返回。
 */
@Data
public class SessionCreateVO {

    private String sessionNo;
    private Long scenicId;
    private String welcomeText;
    private AvatarConfigVO defaultAvatar;
    private AvatarConfigVO selectedAvatar;
}
