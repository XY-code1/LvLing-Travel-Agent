package com.guido.scenicai.module.chat.service;

import com.guido.scenicai.common.result.PageResult;
import com.guido.scenicai.module.chat.dto.AdminChatPageQueryDTO;
import com.guido.scenicai.module.chat.vo.AdminChatMessageVO;

public interface AdminChatService {

    PageResult<AdminChatMessageVO> page(AdminChatPageQueryDTO query);

    AdminChatMessageVO detail(Long id);

    void markSupplement(Long messageId);
}
