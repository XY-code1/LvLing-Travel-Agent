package com.guido.scenicai.module.tourist.service;

import com.guido.scenicai.module.tourist.dto.SessionCreateDTO;
import com.guido.scenicai.module.tourist.vo.MessageVO;
import com.guido.scenicai.module.tourist.vo.SessionCreateVO;
import com.guido.scenicai.module.tourist.vo.SessionVO;

import java.util.List;

public interface ChatSessionService {

    /**
     * 创建导览会话。
     */
    SessionCreateVO createSession(SessionCreateDTO dto);

    /**
     * 查询当前游客的历史会话列表。
     */
    List<SessionVO> getHistory();

    /**
     * 拉取某个会话的所有消息。
     */
    List<MessageVO> getMessages(String sessionNo);
}
