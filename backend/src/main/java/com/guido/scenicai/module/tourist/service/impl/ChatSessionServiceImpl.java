package com.guido.scenicai.module.tourist.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guido.scenicai.common.config.StpTouristUtil;
import com.guido.scenicai.module.avatar.vo.AvatarConfigVO;
import com.guido.scenicai.module.avatar.service.AvatarConfigService;
import com.guido.scenicai.module.tourist.dto.SessionCreateDTO;
import com.guido.scenicai.module.tourist.entity.ChatMessage;
import com.guido.scenicai.module.tourist.entity.ChatSession;
import com.guido.scenicai.module.tourist.mapper.ChatMessageMapper;
import com.guido.scenicai.module.tourist.mapper.ChatSessionMapper;
import com.guido.scenicai.module.tourist.service.ChatSessionService;
import com.guido.scenicai.module.tourist.vo.MessageVO;
import com.guido.scenicai.module.tourist.vo.SessionCreateVO;
import com.guido.scenicai.module.tourist.vo.SessionVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatSessionServiceImpl implements ChatSessionService {

    private final ChatSessionMapper chatSessionMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final AvatarConfigService avatarConfigService;

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    @Transactional
    public SessionCreateVO createSession(SessionCreateDTO dto) {
        Long touristId = StpTouristUtil.stpLogic.getLoginIdAsLong();
        AvatarConfigVO selectedAvatar = avatarConfigService.resolveForTourist(touristId, dto.getAvatarId());

        ChatSession session = new ChatSession();
        session.setSessionNo(UUID.randomUUID().toString());
        session.setTouristUserId(touristId);
        session.setScenicId(dto.getScenicId());
        session.setAvatarConfigId(selectedAvatar == null ? null : selectedAvatar.getId());
        session.setStartTime(LocalDateTime.now());
        session.setLastTime(LocalDateTime.now());
        session.setMessageCount(0);
        session.setTitle("新导览会话");

        chatSessionMapper.insert(session);

        SessionCreateVO vo = new SessionCreateVO();
        vo.setSessionNo(session.getSessionNo());
        vo.setScenicId(dto.getScenicId());
        vo.setWelcomeText(selectedAvatar != null && selectedAvatar.getWelcomeText() != null
                ? selectedAvatar.getWelcomeText()
                : "你好，我是你的专属导游，有什么可以帮你的？");
        vo.setDefaultAvatar(selectedAvatar);
        vo.setSelectedAvatar(selectedAvatar);

        log.info("导览会话创建成功: sessionNo={}, touristId={}", session.getSessionNo(), touristId);
        return vo;
    }

    @Override
    public List<SessionVO> getHistory() {
        Long touristId = StpTouristUtil.stpLogic.getLoginIdAsLong();

        List<ChatSession> sessions = chatSessionMapper.selectList(
                new LambdaQueryWrapper<ChatSession>()
                        .eq(ChatSession::getTouristUserId, touristId)
                        .orderByDesc(ChatSession::getLastTime)
        );

        return sessions.stream().map(s -> {
            SessionVO vo = new SessionVO();
            vo.setSessionNo(s.getSessionNo());
            vo.setScenicId(s.getScenicId());
            vo.setTitle(s.getTitle());
            vo.setLastTime(s.getLastTime() != null ? s.getLastTime().format(DT_FMT) : null);
            vo.setMessageCount(s.getMessageCount());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public List<MessageVO> getMessages(String sessionNo) {
        Long touristId = StpTouristUtil.stpLogic.getLoginIdAsLong();

        List<ChatMessage> messages = chatMessageMapper.selectList(
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getSessionNo, sessionNo)
                        .eq(ChatMessage::getTouristUserId, touristId)
                        .orderByAsc(ChatMessage::getCreateTime)
        );

        return messages.stream().map(m -> {
            MessageVO vo = new MessageVO();
            vo.setMessageId(m.getId());
            vo.setSessionNo(m.getSessionNo());
            vo.setInputType(m.getInputType());
            vo.setQuestion(m.getQuestion());
            vo.setAsrText(m.getAsrText());
            vo.setImageUrl(m.getImageUrl());
            vo.setAnswer(m.getAnswer());
            vo.setSources(m.getSources());
            vo.setHitKb(m.getHitKb());
            vo.setEmotion(m.getEmotion());
            vo.setAudioUrl(m.getAudioUrl());
            vo.setStreamUrl(m.getStreamUrl());
            vo.setCostMs(m.getCostMs());
            vo.setSuccess(m.getSuccess());
            vo.setCreateTime(m.getCreateTime() != null ? m.getCreateTime().format(DT_FMT) : null);
            return vo;
        }).collect(Collectors.toList());
    }
}
