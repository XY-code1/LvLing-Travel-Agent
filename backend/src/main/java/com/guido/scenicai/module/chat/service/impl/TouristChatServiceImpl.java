package com.guido.scenicai.module.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guido.scenicai.common.config.StpTouristUtil;
import com.guido.scenicai.common.exception.BizException;
import com.guido.scenicai.integration.aliyun.AliyunSpeechClient;
import com.guido.scenicai.integration.aliyun.AsrResult;
import com.guido.scenicai.integration.aliyun.TtsResult;
import com.guido.scenicai.integration.llm.LlmChatRequest;
import com.guido.scenicai.integration.llm.LlmChatResponse;
import com.guido.scenicai.integration.llm.LlmClientRouter;
import com.guido.scenicai.integration.vlm.VlmClientRouter;
import com.guido.scenicai.integration.vlm.VlmRecognizeRequest;
import com.guido.scenicai.integration.vlm.VlmRecognizeResponse;
import com.guido.scenicai.module.chat.dto.TextChatDTO;
import com.guido.scenicai.module.chat.service.TouristChatService;
import com.guido.scenicai.module.chat.support.EmotionAnalyzer;
import com.guido.scenicai.module.chat.support.LocalKnowledgeHit;
import com.guido.scenicai.module.chat.support.LocalKnowledgeService;
import com.guido.scenicai.module.knowledge.service.LocalRagService;
import com.guido.scenicai.module.avatar.service.AvatarConfigService;
import com.guido.scenicai.module.avatar.vo.AvatarConfigVO;
import com.guido.scenicai.module.chat.vo.ChatAnswerVO;
import com.guido.scenicai.module.chat.vo.RecognizedSpotVO;
import com.guido.scenicai.module.chat.vo.SourceVO;
import com.guido.scenicai.module.chat.vo.VisionRecognizeVO;
import com.guido.scenicai.module.spot.entity.Spot;
import com.guido.scenicai.module.tourist.entity.ChatMessage;
import com.guido.scenicai.module.tourist.entity.ChatSession;
import com.guido.scenicai.module.tourist.mapper.ChatMessageMapper;
import com.guido.scenicai.module.tourist.mapper.ChatSessionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class TouristChatServiceImpl implements TouristChatService {

    private final ChatSessionMapper chatSessionMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final LocalRagService localRagService;
    private final LlmClientRouter llmClientRouter;
    private final VlmClientRouter vlmClientRouter;
    private final AliyunSpeechClient aliyunSpeechClient;
    private final AvatarConfigService avatarConfigService;
    private final LocalKnowledgeService localKnowledgeService;
    private final EmotionAnalyzer emotionAnalyzer;
    private final ObjectMapper objectMapper;
    private final TouristTextStreamService touristTextStreamService;
    private final TouristVoiceStreamService touristVoiceStreamService;

    @Value("${file.upload.path:./uploads}")
    private String uploadPath;

    @Override
    @Transactional
    public ChatAnswerVO answerText(TextChatDTO dto) {
        long start = System.nanoTime();
        Long touristId = StpTouristUtil.stpLogic.getLoginIdAsLong();
        ChatSession session = getSession(dto.getSessionNo(), touristId);
        List<SourceVO> sources = new ArrayList<>();
        String context = retrieveContext(session, dto.getSpotId(), dto.getQuestion(), sources);
        int hitKb = sources.isEmpty() ? 0 : 1;
        String inputEmotion = emotionAnalyzer.analyze(dto.getQuestion());
        String answer = emotionAnalyzer.adjustTone(buildAnswer(dto.getQuestion(), context, hitKb, inputEmotion),
                inputEmotion);
        String emotion = emotionAnalyzer.analyzeInteraction(dto.getQuestion(), answer);
        String audioUrl = tryTts(answer, avatarForSession(session));
        ChatMessage message = saveMessage(session, touristId, "TEXT", dto.getQuestion(),
                null, null, answer, sources, hitKb, emotion, audioUrl, null, start, null);
        return toAnswerVO(message, sources, null);
    }

    @Override
    public SseEmitter streamText(TextChatDTO dto) {
        return touristTextStreamService.stream(dto);
    }

    @Override
    @Transactional
    public ChatAnswerVO answerVoice(String sessionNo, MultipartFile audio) {
        long start = System.nanoTime();
        Long touristId = StpTouristUtil.stpLogic.getLoginIdAsLong();
        ChatSession session = getSession(sessionNo, touristId);
        String asrText = recognizeAudio(audio);
        TextChatDTO dto = new TextChatDTO();
        dto.setSessionNo(sessionNo);
        dto.setQuestion(asrText);
        List<SourceVO> sources = new ArrayList<>();
        String context = retrieveContext(session, null, asrText, sources);
        int hitKb = sources.isEmpty() ? 0 : 1;
        String inputEmotion = emotionAnalyzer.analyze(asrText);
        String answer = emotionAnalyzer.adjustTone(buildAnswer(asrText, context, hitKb, inputEmotion), inputEmotion);
        String emotion = emotionAnalyzer.analyzeInteraction(asrText, answer);
        String audioUrl = tryTts(answer, avatarForSession(session));
        ChatMessage message = saveMessage(session, touristId, "VOICE", asrText, asrText,
                null, answer, sources, hitKb, emotion, audioUrl, null, start, null);
        return toAnswerVO(message, sources, asrText);
    }

    @Override
    public SseEmitter streamVoice(String sessionNo, MultipartFile audio) {
        return touristVoiceStreamService.stream(sessionNo, audio);
    }

    @Override
    @Transactional
    public VisionRecognizeVO recognizeImage(String sessionNo, MultipartFile image) {
        long start = System.nanoTime();
        Long touristId = StpTouristUtil.stpLogic.getLoginIdAsLong();
        ChatSession session = getSession(sessionNo, touristId);
        String imageUrl = storeImage(image);
        String recognizedText = recognizeImageText(image);
        String question = buildVisionQuestion(recognizedText);
        List<SourceVO> sources = new ArrayList<>();
        String context = retrieveContext(session, null, question, sources);
        int hitKb = sources.isEmpty() ? 0 : 1;
        String inputEmotion = emotionAnalyzer.analyze(question);
        String answer = emotionAnalyzer.adjustTone(buildAnswer(question, context, hitKb, inputEmotion), inputEmotion);
        String audioUrl = tryTts(answer, avatarForSession(session));
        String emotion = emotionAnalyzer.analyzeInteraction(question, answer);
        Optional<LocalKnowledgeHit> recognizedSpot = localKnowledgeService
                .findByRecognizedText(session.getScenicId(), recognizedText);
        ChatMessage message = saveMessage(session, touristId, "IMAGE", recognizedText, null,
                imageUrl, answer, sources, hitKb, emotion, audioUrl, null, start, null);
        return toVisionVO(message, sources, recognizedSpot.map(LocalKnowledgeHit::getSpot).orElse(null));
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

    private String buildAnswer(String question, String context, int hitKb, String inputEmotion) {
        try {
            LlmChatRequest request = new LlmChatRequest();
            request.setSystemPrompt("你是景区导览数字人，语言自然、简洁、可靠。"
                    + "如果提供了景区资料，优先依据资料回答；如果没有资料或资料不足，直接使用文本大模型的通用知识回答，"
                    + "但不要编造具体票价、开放时间、政策承诺，必要时提醒以景区现场公告为准。"
                    + "当前游客情绪为" + inputEmotion + "，若为NEGATIVE或COMPLAINT，先安抚再回答。");
            request.setUserMessage((StringUtils.hasText(context) ? "可用景区资料：" + context + "\n" : "")
                    + "游客问题：" + question);
            request.setMaxTokens(512);
            LlmChatResponse response = llmClientRouter.chat(request);
            if (StringUtils.hasText(response.getContent())) {
                return response.getContent();
            }
        } catch (BizException ignored) {
            if (StringUtils.hasText(context)) {
                return context;
            }
        }
        return "文本大模型暂时不可用，当前无法生成完整回答。";
    }

    private String recognizeAudio(MultipartFile audio) {
        try {
            AsrResult result = aliyunSpeechClient.recognize(audio.getBytes(), audio.getOriginalFilename());
            if (StringUtils.hasText(result.getText())) {
                return result.getText();
            }
        } catch (Exception ignored) {
            return "语音识别服务尚未配置，当前语音问题无法转写。";
        }
        return "语音识别结果为空。";
    }

    private String recognizeImageText(MultipartFile image) {
        try {
            VlmRecognizeRequest request = new VlmRecognizeRequest();
            request.setImageBase64(Base64.getEncoder().encodeToString(image.getBytes()));
            request.setMimeType(StringUtils.hasText(image.getContentType()) ? image.getContentType() : "image/jpeg");
            request.setPrompt("请识别图片中的景点或建筑名称，只返回名称。");
            request.setMaxTokens(64);
            VlmRecognizeResponse response = vlmClientRouter.recognize(request);
            if (StringUtils.hasText(response.getContent())) {
                return response.getContent();
            }
        } catch (Exception ignored) {
            return image.getOriginalFilename();
        }
        return image.getOriginalFilename();
    }

    private String buildVisionQuestion(String recognizedText) {
        if (!StringUtils.hasText(recognizedText)) {
            return "游客上传了一张景区图片，但视觉模型没有输出明确文字，请结合景区知识库给出谨慎讲解。";
        }
        return "游客上传了一张景区图片，多模态模型识别结果为：" + recognizedText
                + "。请先判断对应景点，再结合知识库给出自然、简洁的导览讲解。";
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
        } catch (BizException ignored) {
            return null;
        }
    }

    private ChatMessage saveMessage(ChatSession session, Long touristId, String inputType,
                                    String question, String asrText, String imageUrl,
                                    String answer, List<SourceVO> sources, int hitKb,
                                    String emotion, String audioUrl, String streamUrl,
                                    long start, String errorMsg) {
        ChatMessage message = new ChatMessage();
        message.setSessionId(session.getId());
        message.setSessionNo(session.getSessionNo());
        message.setTouristUserId(touristId);
        message.setInputType(inputType);
        message.setQuestion(question);
        message.setAsrText(asrText);
        message.setImageUrl(imageUrl);
        message.setAnswer(answer);
        message.setSources(toJson(sources));
        message.setHitKb(hitKb);
        message.setEmotion(emotion);
        message.setAudioUrl(audioUrl);
        message.setStreamUrl(streamUrl);
        message.setCostMs(elapsedMs(start));
        message.setSuccess(errorMsg == null ? 1 : 0);
        message.setErrorMsg(errorMsg);
        message.setNeedSupplement(hitKb == 1 ? 0 : 1);
        chatMessageMapper.insert(message);
        updateSession(session, question);
        return message;
    }

    private void updateSession(ChatSession session, String question) {
        session.setLastTime(LocalDateTime.now());
        session.setMessageCount((session.getMessageCount() == null ? 0 : session.getMessageCount()) + 1);
        if (!StringUtils.hasText(session.getTitle()) || "新导览会话".equals(session.getTitle())) {
            session.setTitle(question == null ? "导览会话" : question.substring(0, Math.min(question.length(), 20)));
        }
        chatSessionMapper.updateById(session);
    }

    private ChatAnswerVO toAnswerVO(ChatMessage message, List<SourceVO> sources, String asrText) {
        ChatAnswerVO vo = new ChatAnswerVO();
        vo.setMessageId(message.getId());
        vo.setAnswer(message.getAnswer());
        vo.setHitKb(message.getHitKb());
        vo.setSources(sources);
        vo.setEmotion(message.getEmotion());
        vo.setStreamUrl(message.getStreamUrl());
        vo.setAudioUrl(message.getAudioUrl());
        vo.setCostMs(message.getCostMs());
        vo.setAsrText(asrText);
        return vo;
    }

    private VisionRecognizeVO toVisionVO(ChatMessage message, List<SourceVO> sources, Spot spot) {
        VisionRecognizeVO vo = new VisionRecognizeVO();
        vo.setMessageId(message.getId());
        vo.setRecognizedSpot(toRecognizedSpot(spot));
        vo.setAnswer(message.getAnswer());
        vo.setHitKb(message.getHitKb());
        vo.setSources(sources);
        vo.setEmotion(message.getEmotion());
        vo.setStreamUrl(message.getStreamUrl());
        vo.setAudioUrl(message.getAudioUrl());
        vo.setCostMs(message.getCostMs());
        return vo;
    }

    private RecognizedSpotVO toRecognizedSpot(Spot spot) {
        if (spot == null) {
            return null;
        }
        RecognizedSpotVO vo = new RecognizedSpotVO();
        vo.setSpotId(spot.getId());
        vo.setSpotName(spot.getName());
        vo.setConfidence(1.0D);
        vo.setName(spot.getName());
        vo.setIntro(spot.getIntro());
        return vo;
    }

    private void sendChatEvents(SseEmitter emitter, ChatAnswerVO result, String sessionNo) {
        try {
            sendEvent(emitter, "meta", toJson(MapData.of("sessionNo", sessionNo, "messageId", result.getMessageId())));
            for (String chunk : splitAnswer(result.getAnswer())) {
                sendEvent(emitter, "delta", toJson(MapData.of("text", chunk)));
            }
            sendEvent(emitter, "sources", toJson(MapData.of("hit", result.getHitKb(), "sources", result.getSources())));
            sendEvent(emitter, "emotion", toJson(MapData.of("emotion", result.getEmotion())));
            sendEvent(emitter, "audio", toJson(MapData.of("audioUrl", result.getAudioUrl())));
            sendEvent(emitter, "avatar", toJson(MapData.of("streamUrl", result.getStreamUrl())));
            sendEvent(emitter, "done", toJson(MapData.of("costMs", result.getCostMs(), "success", true)));
            emitter.complete();
        } catch (Exception e) {
            emitter.completeWithError(e);
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

    private String storeImage(MultipartFile image) {
        try {
            String original = image.getOriginalFilename();
            String suffix = original != null && original.contains(".")
                    ? original.substring(original.lastIndexOf('.')) : ".jpg";
            String fileName = UUID.randomUUID() + suffix;
            Path dir = Path.of(uploadPath, "vision");
            Files.createDirectories(dir);
            Files.write(dir.resolve(fileName), image.getBytes());
            return "/files/vision/" + fileName;
        } catch (Exception e) {
            throw new BizException(500, "图片保存失败", e);
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

    private void sendEvent(SseEmitter emitter, String name, String data) {
        try {
            emitter.send(SseEmitter.event().name(name).data(data));
        } catch (Exception e) {
            throw new BizException(500, "SSE 推送失败", e);
        }
    }

    private String quote(String value) {
        return toJson(value == null ? "" : value);
    }
}
