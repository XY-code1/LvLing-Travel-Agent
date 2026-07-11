package com.guido.scenicai.module.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guido.scenicai.common.exception.BizException;
import com.guido.scenicai.common.result.PageResult;
import com.guido.scenicai.module.chat.dto.AdminChatPageQueryDTO;
import com.guido.scenicai.module.chat.service.AdminChatService;
import com.guido.scenicai.module.chat.vo.AdminChatMessageVO;
import com.guido.scenicai.module.tourist.entity.ChatMessage;
import com.guido.scenicai.module.tourist.mapper.ChatMessageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminChatServiceImpl implements AdminChatService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ChatMessageMapper chatMessageMapper;

    @Override
    public PageResult<AdminChatMessageVO> page(AdminChatPageQueryDTO query) {
        LambdaQueryWrapper<ChatMessage> wrapper = buildWrapper(query);
        wrapper.orderByDesc(ChatMessage::getId);
        Page<ChatMessage> page = chatMessageMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        List<AdminChatMessageVO> records = page.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(page.getTotal(), query.getPageNum(), query.getPageSize(), records);
    }

    @Override
    public AdminChatMessageVO detail(Long id) {
        return toVO(getRequired(id));
    }

    @Override
    public void markSupplement(Long messageId) {
        ChatMessage message = getRequired(messageId);
        message.setNeedSupplement(1);
        chatMessageMapper.updateById(message);
    }

    private LambdaQueryWrapper<ChatMessage> buildWrapper(AdminChatPageQueryDTO query) {
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(query.getInputType()), ChatMessage::getInputType, query.getInputType());
        wrapper.eq(StringUtils.hasText(query.getEmotion()), ChatMessage::getEmotion, query.getEmotion());
        wrapper.eq(query.getSuccess() != null, ChatMessage::getSuccess, query.getSuccess());
        wrapper.ge(StringUtils.hasText(query.getStartTime()), ChatMessage::getCreateTime,
                parseDateTime(query.getStartTime()));
        wrapper.le(StringUtils.hasText(query.getEndTime()), ChatMessage::getCreateTime,
                parseDateTime(query.getEndTime()));
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.and(w -> w.like(ChatMessage::getQuestion, query.getKeyword())
                    .or().like(ChatMessage::getAnswer, query.getKeyword())
                    .or().like(ChatMessage::getAsrText, query.getKeyword()));
        }
        return wrapper;
    }

    private ChatMessage getRequired(Long id) {
        ChatMessage message = chatMessageMapper.selectById(id);
        if (message == null) {
            throw BizException.notFound("对话消息不存在");
        }
        return message;
    }

    private LocalDateTime parseDateTime(String timeStr) {
        if (!StringUtils.hasText(timeStr)) {
            return null;
        }
        try {
            return LocalDateTime.parse(timeStr, FORMATTER);
        } catch (DateTimeParseException e) {
            log.warn("对话时间格式解析失败: {}", timeStr, e);
            throw new BizException(400, "时间格式必须为 yyyy-MM-dd HH:mm:ss");
        }
    }

    private AdminChatMessageVO toVO(ChatMessage message) {
        AdminChatMessageVO vo = new AdminChatMessageVO();
        vo.setId(message.getId());
        vo.setSessionId(message.getSessionId());
        vo.setSessionNo(message.getSessionNo());
        vo.setTouristUserId(message.getTouristUserId());
        vo.setInputType(message.getInputType());
        vo.setQuestion(message.getQuestion());
        vo.setAsrText(message.getAsrText());
        vo.setImageUrl(message.getImageUrl());
        vo.setAnswer(message.getAnswer());
        vo.setSources(message.getSources());
        vo.setHitKb(message.getHitKb());
        vo.setEmotion(message.getEmotion());
        vo.setAudioUrl(message.getAudioUrl());
        vo.setStreamUrl(message.getStreamUrl());
        vo.setCostMs(message.getCostMs());
        vo.setSuccess(message.getSuccess());
        vo.setErrorMsg(message.getErrorMsg());
        vo.setNeedSupplement(message.getNeedSupplement());
        vo.setCreateTime(message.getCreateTime());
        return vo;
    }
}
