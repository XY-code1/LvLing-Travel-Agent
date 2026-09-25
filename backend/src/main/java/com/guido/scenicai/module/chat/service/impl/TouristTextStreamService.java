package com.guido.scenicai.module.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guido.scenicai.common.config.StpTouristUtil;
import com.guido.scenicai.common.exception.BizException;
import com.guido.scenicai.integration.aliyun.AliyunSpeechClient;
import com.guido.scenicai.integration.aliyun.TtsResult;
import com.guido.scenicai.integration.llm.LlmChatRequest;
import com.guido.scenicai.module.knowledge.service.LocalRagService;
import com.guido.scenicai.integration.llm.LlmClientRouter;
import com.guido.scenicai.module.avatar.service.AvatarConfigService;
import com.guido.scenicai.module.avatar.vo.AvatarConfigVO;
import com.guido.scenicai.module.chat.dto.TextChatDTO;
import com.guido.scenicai.module.chat.support.EmotionAnalyzer;
import com.guido.scenicai.module.chat.support.LocalKnowledgeHit;
import com.guido.scenicai.module.chat.support.LocalKnowledgeService;
import com.guido.scenicai.module.chat.vo.SourceVO;
import com.guido.scenicai.module.tourist.entity.ChatMessage;
import com.guido.scenicai.module.tourist.entity.ChatSession;
import com.guido.scenicai.module.tourist.mapper.ChatMessageMapper;
import com.guido.scenicai.module.tourist.mapper.ChatSessionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class TouristTextStreamService {

    private final ChatSessionMapper chatSessionMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final LocalRagService localRagService;
    private final LlmClientRouter llmClientRouter;
    private final AliyunSpeechClient aliyunSpeechClient;
    private final AvatarConfigService avatarConfigService;
    private final LocalKnowledgeService localKnowledgeService;
    private final EmotionAnalyzer emotionAnalyzer;
    private final ObjectMapper objectMapper;

    public SseEmitter stream(TextChatDTO dto) {
        Long touristId = StpTouristUtil.stpLogic.getLoginIdAsLong();
        SseEmitter emitter = new SseEmitter(60000L);
        CompletableFuture.runAsync(() -> runStream(dto, touristId, emitter));
        return emitter;
    }

    private void runStream(TextChatDTO dto, Long touristId, SseEmitter emitter) {
        long start = System.nanoTime();
        try {
            ChatSession session = getSession(dto.getSessionNo(), touristId);
            List<SourceVO> sources = new ArrayList<>();
            String context = retrieveContext(session, dto.getSpotId(), dto.getQuestion(), sources);
            int hitKb = sources.isEmpty() ? 0 : 1;
            String inputEmotion = emotionAnalyzer.analyze(dto.getQuestion());
            ChatMessage message = createDraft(session, touristId, dto.getQuestion(), sources, hitKb);
            sendEvent(emitter, "meta", toJson(MapData.of("sessionNo", session.getSessionNo(),
                    "messageId", message.getId())));

            String answer = streamAnswer(emitter, dto.getQuestion(), context, hitKb, inputEmotion);
            String emotion = emotionAnalyzer.analyzeInteraction(dto.getQuestion(), answer);
            String audioUrl = tryTts(answer, avatarForSession(session));
            completeMessage(message, answer, emotion, audioUrl, null, start, null);
            updateSession(session, dto.getQuestion());

            sendEvent(emitter, "sources", toJson(MapData.of("hit", hitKb, "sources", sources)));
            sendEvent(emitter, "emotion", toJson(MapData.of("emotion", emotion)));
            sendEvent(emitter, "audio", toJson(MapData.of("audioUrl", audioUrl)));
            sendEvent(emitter, "avatar", toJson(MapData.of("streamUrl", null)));
            sendEvent(emitter, "done", toJson(MapData.of("costMs", message.getCostMs(), "success", true)));
            emitter.complete();
        } catch (Exception e) {
            completeWithError(emitter, e);
        }
    }

    private ChatSession getSession(String sessionNo, Long touristId) {
        ChatSession session = chatSessionMapper.selectOne(new LambdaQueryWrapper<ChatSession>()
                .eq(ChatSession::getSessionNo, sessionNo)
                .eq(ChatSession::getTouristUserId, touristId)
                .last("LIMIT 1"));
        if (session == null) {
            throw BizException.notFound("导览会话不存在");
        }
        return session;
    }

    private String retrieveContext(ChatSession session, Long spotId, String question, List<SourceVO> sources) {
        // 主路径：本地 RAG 向量/关键词检索（kb_chunk 表）
        LocalRagService.RagResult ragResult = localRagService.retrieve(session.getScenicId(), question);
        if (ragResult.hit()) {
            sources.addAll(ragResult.sources());
            return ragResult.context();
        }
        // 降级：景点数据库关键词匹配
        return localContext(session.getScenicId(), spotId, question, sources);
    }

    private String localContext(Long scenicId, Long spotId, String question, List<SourceVO> sources) {
        Optional<LocalKnowledgeHit> localHit = localKnowledgeService.findByQuestion(scenicId, spotId, question);
        localHit.ifPresent(hit -> sources.add(hit.getSource()));
        return localHit.map(LocalKnowledgeHit::getContent).orElse("");
    }

    private ChatMessage createDraft(ChatSession session, Long touristId,
                                    String question, List<SourceVO> sources, int hitKb) {
        ChatMessage message = new ChatMessage();
        message.setSessionId(session.getId());
        message.setSessionNo(session.getSessionNo());
        message.setTouristUserId(touristId);
        message.setInputType("TEXT");
        message.setQuestion(question);
        message.setAnswer("");
        message.setSources(toJson(sources));
        message.setHitKb(hitKb);
        message.setEmotion("NEUTRAL");
        message.setCostMs(0);
        message.setSuccess(0);
        message.setNeedSupplement(hitKb == 1 ? 0 : 1);
        chatMessageMapper.insert(message);
        return message;
    }

    private String streamAnswer(SseEmitter emitter, String question, String context, int hitKb, String inputEmotion) {
        StringBuilder answer = new StringBuilder();
        String lead = emotionAnalyzer.comfortLead(inputEmotion);
        if (StringUtils.hasText(lead)) {
            answer.append(lead);
            sendChunks(emitter, lead);
        }
        int answerLengthBeforeModel = answer.length();
        try {
            llmClientRouter.stream(toLlmRequest(question, context, inputEmotion), delta -> {
                answer.append(delta);
                sendEvent(emitter, "delta", toJson(MapData.of("text", delta)));
            });
        } catch (BizException e) {
            // Partial model output is still usable; otherwise surface the missing provider explicitly.
            if (answer.length() == answerLengthBeforeModel) {
                throw e;
            }
        }
        if (!StringUtils.hasText(answer.toString())) {
            if (StringUtils.hasText(context)) {
                sendChunks(emitter, context);
                return context;
            }
            throw new BizException(503, "文本大模型未返回内容：请确认管理后台「AI 配置 > 文本大模型」已启用，或稍后重试");
        }
        return answer.toString();
    }

    private LlmChatRequest toLlmRequest(String question, String context, String inputEmotion) {
        LlmChatRequest request = new LlmChatRequest();
        request.setSystemPrompt("你是景区导览数字人，语言自然、简洁、可靠。"
                + "如果提供了景区资料，优先依据资料回答；如果没有资料或资料不足，直接使用文本大模型的通用知识回答，"
                + "但不要编造具体票价、开放时间、政策承诺，必要时提醒以景区现场公告为准。"
                + "当前游客情绪为" + inputEmotion + "，若为NEGATIVE或COMPLAINT，先安抚再回答。");
        request.setUserMessage((StringUtils.hasText(context) ? "可用景区资料：" + context + "\n" : "")
                + "游客问题：" + question);
        request.setMaxTokens(512);
        return request;
    }

    private void sendChunks(SseEmitter emitter, String answer) {
        for (String chunk : splitAnswer(answer)) {
            sendEvent(emitter, "delta", toJson(MapData.of("text", chunk)));
        }
    }

    private List<String> splitAnswer(String answer) {
        if (!StringUtils.hasText(answer) || answer.length() <= 12) {
            return List.of(answer == null ? "" : answer);
        }
        List<String> chunks = new ArrayList<>();
        for (int i = 0; i < answer.length(); i += 12) {
            chunks.add(answer.substring(i, Math.min(answer.length(), i + 12)));
        }
        return chunks;
    }

    private AvatarConfigVO avatarForSession(ChatSession session) {
        AvatarConfigVO avatar = avatarConfigService.getEnabledById(session.getAvatarConfigId());
        return avatar != null ? avatar : avatarConfigService.getDefaultWithDetail();
    }

    private String tryTts(String answer, AvatarConfigVO avatar) {
        try {
            TtsResult result = aliyunSpeechClient.synthesize(answer,
                    avatar == null ? null : avatar.getVoice(),
                    avatar == null ? null : avatar.getSpeechRate());
            return result.getAudioUrl();
        } catch (BizException e) {
            return null;
        }
    }

    private void completeMessage(ChatMessage message, String answer, String emotion,
                                 String audioUrl, String streamUrl, long start, String errorMsg) {
        message.setAnswer(answer);
        message.setEmotion(emotion);
        message.setAudioUrl(audioUrl);
        message.setStreamUrl(streamUrl);
        message.setCostMs(elapsedMs(start));
        message.setSuccess(errorMsg == null ? 1 : 0);
        message.setErrorMsg(errorMsg);
        chatMessageMapper.updateById(message);
    }

    private void updateSession(ChatSession session, String question) {
        session.setLastTime(LocalDateTime.now());
        session.setMessageCount((session.getMessageCount() == null ? 0 : session.getMessageCount()) + 1);
        if (!StringUtils.hasText(session.getTitle()) || "新导览会话".equals(session.getTitle())) {
            session.setTitle(question == null ? "导览会话" : question.substring(0, Math.min(question.length(), 20)));
        }
        chatSessionMapper.updateById(session);
    }

    private void sendEvent(SseEmitter emitter, String name, String data) {
        try {
            emitter.send(SseEmitter.event().name(name).data(data));
        } catch (Exception e) {
            throw new BizException(500, "SSE 推送失败", e);
        }
    }

    private void completeWithError(SseEmitter emitter, Exception error) {
        String message = error instanceof BizException bizException
                ? bizException.getMsg()
                : "AI 服务暂不可用，请检查管理后台配置";
        try {
            sendEvent(emitter, "error", toJson(MapData.of("message", message)));
            emitter.complete();
        } catch (Exception ignored) {
            emitter.completeWithError(error);
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new BizException(500, "JSON 序列化失败", e);
        }
    }

    private int elapsedMs(long start) {
        return Math.toIntExact((System.nanoTime() - start) / 1_000_000L);
    }
}
