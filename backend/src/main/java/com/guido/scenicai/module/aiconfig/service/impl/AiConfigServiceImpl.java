package com.guido.scenicai.module.aiconfig.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guido.scenicai.common.constant.ServiceType;
import com.guido.scenicai.common.exception.BizException;
import com.guido.scenicai.common.security.CredentialCrypto;
import com.guido.scenicai.common.util.MaskUtil;
import com.guido.scenicai.integration.common.AiConfigSnapshot;
import com.guido.scenicai.integration.embedding.EmbeddingClientRouter;
import com.guido.scenicai.integration.llm.LlmChatRequest;
import com.guido.scenicai.integration.llm.LlmChatResponse;
import com.guido.scenicai.integration.llm.LlmClient;
import com.guido.scenicai.integration.vlm.VlmClientRouter;
import com.guido.scenicai.integration.vlm.VlmRecognizeRequest;
import com.guido.scenicai.integration.vlm.VlmRecognizeResponse;
import com.guido.scenicai.module.aiconfig.dto.AiConfigListQueryDTO;
import com.guido.scenicai.module.aiconfig.dto.AiConfigTestDTO;
import com.guido.scenicai.module.aiconfig.dto.AiConfigUpsertDTO;
import com.guido.scenicai.module.aiconfig.entity.AiServiceConfig;
import com.guido.scenicai.module.aiconfig.mapper.AiServiceConfigMapper;
import com.guido.scenicai.module.aiconfig.service.AiConfigService;
import com.guido.scenicai.module.aiconfig.vo.AiConfigTestVO;
import com.guido.scenicai.module.aiconfig.vo.AiConfigVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AiConfigServiceImpl implements AiConfigService {

    private static final Set<String> SINGLE_TOKEN_TYPES = Set.of(
            ServiceType.LLM, ServiceType.VISION, ServiceType.EMBEDDING
    );
    private static final Set<String> ALI_TYPES = Set.of(
            ServiceType.ASR, ServiceType.TTS
    );

    private final AiServiceConfigMapper aiServiceConfigMapper;
    private final CredentialCrypto credentialCrypto;
    private final VlmClientRouter vlmClientRouter;
    private final EmbeddingClientRouter embeddingClientRouter;
    private final List<LlmClient> llmClients;

    @Override
    public List<AiConfigVO> list(AiConfigListQueryDTO query) {
        LambdaQueryWrapper<AiServiceConfig> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getServiceType())) {
            wrapper.eq(AiServiceConfig::getServiceType, query.getServiceType());
        }
        wrapper.orderByAsc(AiServiceConfig::getServiceType).orderByDesc(AiServiceConfig::getId);
        return aiServiceConfigMapper.selectList(wrapper).stream().map(this::toVO).toList();
    }

    @Override
    @Transactional
    public AiConfigVO save(AiConfigUpsertDTO dto) {
        validateServiceIdentity(dto);
        AiServiceConfig entity = dto.getId() == null ? new AiServiceConfig() : getRequired(dto.getId());
        boolean creating = dto.getId() == null;
        copyPlainFields(dto, entity);
        applyCredential(dto.getApiKey(), entity.getApiKey(), entity::setApiKey, creating);
        applyCredential(dto.getAccessKeyId(), entity.getAccessKeyId(), entity::setAccessKeyId, creating);
        applyCredential(dto.getAccessKeySecret(), entity.getAccessKeySecret(), entity::setAccessKeySecret, creating);
        if (entity.getTimeoutMs() == null) {
            entity.setTimeoutMs(10000);
        }
        if (entity.getRetryCount() == null) {
            entity.setRetryCount(1);
        }
        if (entity.getCapabilityVerified() == null) {
            entity.setCapabilityVerified(0);
        }
        validateStoredRequired(entity);
        entity.setEnabled(entity.getEnabled() == null ? 0 : entity.getEnabled());
        entity.setIsDefault(entity.getIsDefault() == null ? 0 : entity.getIsDefault());
        if (entity.getEnabled() == 1 && entity.getCapabilityVerified() != 1) {
            throw new BizException(400, "能力校验未通过，不能启用");
        }
        if (creating) {
            aiServiceConfigMapper.insert(entity);
        } else {
            aiServiceConfigMapper.updateById(entity);
        }
        return toVO(entity);
    }

    @Override
    @Transactional
    public void remove(Long id) {
        getRequired(id);
        aiServiceConfigMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void enableDefault(Long id) {
        AiServiceConfig entity = getRequired(id);
        if (entity.getCapabilityVerified() == null || entity.getCapabilityVerified() != 1) {
            throw new BizException(400, "能力校验未通过，不能启用");
        }
        aiServiceConfigMapper.selectList(new LambdaQueryWrapper<AiServiceConfig>()
                .eq(AiServiceConfig::getServiceType, entity.getServiceType()))
                .forEach(item -> {
                    item.setEnabled(0);
                    item.setIsDefault(0);
                    aiServiceConfigMapper.updateById(item);
                });
        entity.setEnabled(1);
        entity.setIsDefault(1);
        aiServiceConfigMapper.updateById(entity);
    }

    @Override
    public AiConfigTestVO test(Long id, AiConfigTestDTO dto) {
        AiServiceConfig entity = getRequired(id);
        validateStoredRequired(entity);
        if (ServiceType.VISION.equals(entity.getServiceType())) {
            return testVision(entity, dto);
        }
        if (ServiceType.EMBEDDING.equals(entity.getServiceType())) {
            return testEmbedding(entity, dto);
        }
        if (ServiceType.LLM.equals(entity.getServiceType())) {
            return testLlm(entity, dto);
        }
        if (ALI_TYPES.contains(entity.getServiceType())) {
            return testAliCredentials(entity);
        }
        return new AiConfigTestVO(false, 0,
                "未支持的测试类型：" + entity.getServiceType(),
                entity.getCapabilityVerified());
    }

    @Override
    public AiConfigTestVO testVisionFile(Long id, MultipartFile image, String prompt) {
        AiServiceConfig entity = getRequired(id);
        if (!ServiceType.VISION.equals(entity.getServiceType())) {
            throw new BizException(400, "只有多模态模型支持图片文件测试");
        }
        validateStoredRequired(entity);
        if (image == null || image.isEmpty()) {
            throw new BizException(400, "请选择测试图片");
        }
        try {
            AiConfigTestDTO dto = new AiConfigTestDTO();
            dto.setPrompt(prompt);
            dto.setMimeType(StringUtils.hasText(image.getContentType()) ? image.getContentType() : "image/png");
            dto.setImageBase64(Base64.getEncoder().encodeToString(image.getBytes()));
            return testVision(entity, dto);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException(500, "测试图片读取失败", e);
        }
    }

    private AiConfigTestVO testLlm(AiServiceConfig entity, AiConfigTestDTO dto) {
        String userMsg = dto != null && StringUtils.hasText(dto.getQuestion())
                ? dto.getQuestion()
                : "你好，请回复一个字：好";
        long start = System.nanoTime();
        try {
            LlmChatRequest req = new LlmChatRequest();
            req.setUserMessage(userMsg);
            req.setSystemPrompt("你是连通性测试助手。请直接回复一句简短中文，不要输出思考过程。");
            req.setMaxTokens(256);
            LlmClient llmClient = llmClients.stream()
                    .filter(c -> c.protocol().equalsIgnoreCase(entity.getProtocol()))
                    .findFirst()
                    .orElseThrow(() -> new BizException(400, "不支持的 LLM 协议：" + entity.getProtocol()));
            LlmChatResponse response = llmClient.chat(toSnapshot(entity), req);
            int costMs = elapsedMs(start);
            if (!StringUtils.hasText(response.getContent())) {
                String msg = "LLM 返回内容为空，原始返回：" + limit(response.getRawResponse(), 180);
                markVerified(entity, 0, msg);
                return new AiConfigTestVO(false, costMs, msg, 0);
            }
            String msg = "LLM 连通成功，响应：" + limit(response.getContent(), 80);
            markVerified(entity, 1, msg);
            return new AiConfigTestVO(true, costMs, msg, 1);
        } catch (BizException e) {
            int costMs = elapsedMs(start);
            markVerified(entity, 0, e.getMessage());
            return new AiConfigTestVO(false, costMs, e.getMessage(), 0);
        } catch (Exception e) {
            int costMs = elapsedMs(start);
            String errMsg = "LLM 调用异常：" + e.getMessage();
            markVerified(entity, 0, errMsg);
            return new AiConfigTestVO(false, costMs, errMsg, 0);
        }
    }

    private AiConfigTestVO testAliCredentials(AiServiceConfig entity) {
        // 阿里云凭证完整性校验：验证必填字段均已配置，不发送真实网络请求。
        // 竞赛环境在填写凭证后直接标记通过；实际连通在业务调用时验证。
        long start = System.nanoTime();
        String accessKeyId = credentialCrypto.decrypt(entity.getAccessKeyId());
        String accessKeySecret = credentialCrypto.decrypt(entity.getAccessKeySecret());
        if (!StringUtils.hasText(accessKeyId) || !StringUtils.hasText(accessKeySecret)
                || !StringUtils.hasText(entity.getAppKey()) || !StringUtils.hasText(entity.getRegion())) {
            int costMs = elapsedMs(start);
            markVerified(entity, 0, "凭证不完整：请确认 AccessKeyId / AccessKeySecret / AppKey / Region 均已填写");
            return new AiConfigTestVO(false, costMs, "凭证不完整", 0);
        }
        int costMs = elapsedMs(start);
        String msg = entity.getServiceType() + " 凭证已配置，字段校验通过（实际连通在业务调用时验证）";
        markVerified(entity, 1, msg);
        return new AiConfigTestVO(true, costMs, msg, 1);
    }

    private AiServiceConfig getRequired(Long id) {
        AiServiceConfig entity = aiServiceConfigMapper.selectById(id);
        if (entity == null) {
            throw BizException.notFound("AI 服务配置不存在");
        }
        return entity;
    }

    private void validateServiceIdentity(AiConfigUpsertDTO dto) {
        if (!SINGLE_TOKEN_TYPES.contains(dto.getServiceType()) && !ALI_TYPES.contains(dto.getServiceType())) {
            throw new BizException(400, "不支持的服务类型");
        }
    }

    private void validateStoredRequired(AiServiceConfig entity) {
        if (SINGLE_TOKEN_TYPES.contains(entity.getServiceType())) {
            requireText(entity.getBaseUrl(), "接口地址不能为空");
            requireText(entity.getModelName(), "模型名不能为空");
            if (ServiceType.EMBEDDING.equals(entity.getServiceType())) {
                if (!StringUtils.hasText(entity.getProtocol())) {
                    entity.setProtocol("OPENAI_COMPATIBLE");
                }
            } else {
                requireText(entity.getApiKey(), "API Key 不能为空");
                requireText(entity.getProtocol(), "协议不能为空");
            }
        } else if (ALI_TYPES.contains(entity.getServiceType())) {
            requireText(entity.getAccessKeyId(), "AccessKeyId 不能为空");
            requireText(entity.getAccessKeySecret(), "AccessKeySecret 不能为空");
            requireText(entity.getAppKey(), "appKey/appId 不能为空");
            requireText(entity.getRegion(), "服务区域不能为空");
        }
    }

    private void requireText(String value, String msg) {
        if (!StringUtils.hasText(value)) {
            throw new BizException(400, msg);
        }
    }

    private AiConfigTestVO testVision(AiServiceConfig entity, AiConfigTestDTO dto) {
        if (dto == null || (!StringUtils.hasText(dto.getImageBase64())
                && !StringUtils.hasText(dto.getImageUrl()))) {
            throw new BizException(400, "VISION 校验必须传测试图片");
        }
        long start = System.nanoTime();
        try {
            VlmRecognizeResponse response = vlmClientRouter.recognize(toSnapshot(entity), toVisionRequest(dto));
            if (!StringUtils.hasText(response.getContent())) {
                throw new BizException(400, "视觉模型返回内容为空");
            }
            int costMs = elapsedMs(start);
            markVerified(entity, 1, "图片校验通过：" + limit(response.getContent(), 120));
            return new AiConfigTestVO(true, costMs, limit(response.getContent(), 200), 1);
        } catch (BizException e) {
            int costMs = elapsedMs(start);
            markVerified(entity, 0, e.getMessage());
            return new AiConfigTestVO(false, costMs, e.getMessage(), 0);
        }
    }

    private VlmRecognizeRequest toVisionRequest(AiConfigTestDTO dto) {
        VlmRecognizeRequest request = new VlmRecognizeRequest();
        request.setPrompt(StringUtils.hasText(dto.getPrompt()) ? dto.getPrompt() : "请识别这张图片，简短回答。");
        request.setImageBase64(dto.getImageBase64());
        request.setImageUrl(dto.getImageUrl());
        request.setMimeType(StringUtils.hasText(dto.getMimeType()) ? dto.getMimeType() : "image/png");
        request.setMaxTokens(128);
        return request;
    }

    private AiConfigTestVO testEmbedding(AiServiceConfig entity, AiConfigTestDTO dto) {
        String text = dto != null && StringUtils.hasText(dto.getQuestion())
                ? dto.getQuestion()
                : "灵山大佛";
        long start = System.nanoTime();
        try {
            List<Float> vector = embeddingClientRouter.embed(toSnapshot(entity), text);
            int costMs = elapsedMs(start);
            if (vector == null || vector.isEmpty()) {
                markVerified(entity, 0, "本地向量模型返回空向量");
                return new AiConfigTestVO(false, costMs, "本地向量模型返回空向量", 0);
            }
            String msg = "本地向量模型连通成功，向量维度：" + vector.size();
            markVerified(entity, 1, msg);
            return new AiConfigTestVO(true, costMs, msg, 1);
        } catch (BizException e) {
            int costMs = elapsedMs(start);
            markVerified(entity, 0, e.getMessage());
            return new AiConfigTestVO(false, costMs, e.getMessage(), 0);
        } catch (Exception e) {
            int costMs = elapsedMs(start);
            String errMsg = "本地向量模型调用异常：" + e.getMessage();
            markVerified(entity, 0, errMsg);
            return new AiConfigTestVO(false, costMs, errMsg, 0);
        }
    }

    private AiConfigSnapshot toSnapshot(AiServiceConfig entity) {
        AiConfigSnapshot snapshot = new AiConfigSnapshot();
        snapshot.setId(entity.getId());
        snapshot.setServiceType(entity.getServiceType());
        snapshot.setProvider(entity.getProvider());
        snapshot.setProtocol(entity.getProtocol());
        snapshot.setBaseUrl(entity.getBaseUrl());
        snapshot.setApiKey(credentialCrypto.decrypt(entity.getApiKey()));
        snapshot.setAccessKeyId(credentialCrypto.decrypt(entity.getAccessKeyId()));
        snapshot.setAccessKeySecret(credentialCrypto.decrypt(entity.getAccessKeySecret()));
        snapshot.setAppKey(entity.getAppKey());
        snapshot.setRegion(entity.getRegion());
        snapshot.setModelName(entity.getModelName());
        snapshot.setDatasetId(entity.getDatasetId());
        snapshot.setExtraConfig(entity.getExtraConfig());
        snapshot.setTimeoutMs(entity.getTimeoutMs());
        snapshot.setRetryCount(entity.getRetryCount());
        return snapshot;
    }

    private void markVerified(AiServiceConfig entity, int capabilityVerified, String verifyMsg) {
        entity.setCapabilityVerified(capabilityVerified);
        entity.setVerifiedTime(LocalDateTime.now());
        entity.setVerifyMsg(limit(verifyMsg, 255));
        aiServiceConfigMapper.updateById(entity);
    }

    private int elapsedMs(long start) {
        return Math.toIntExact((System.nanoTime() - start) / 1_000_000L);
    }

    private String limit(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private void copyPlainFields(AiConfigUpsertDTO dto, AiServiceConfig entity) {
        entity.setServiceType(dto.getServiceType());
        entity.setProvider(dto.getProvider());
        entity.setProtocol(dto.getProtocol());
        entity.setBaseUrl(dto.getBaseUrl());
        entity.setAppKey(dto.getAppKey());
        entity.setRegion(dto.getRegion());
        entity.setModelName(dto.getModelName());
        entity.setDatasetId(dto.getDatasetId());
        entity.setExtraConfig(dto.getExtraConfig());
        entity.setTimeoutMs(dto.getTimeoutMs());
        entity.setRetryCount(dto.getRetryCount());
        entity.setEnabled(dto.getEnabled());
        entity.setRemark(dto.getRemark());
    }

    private void applyCredential(String input, String oldValue,
                                 java.util.function.Consumer<String> setter,
                                 boolean creating) {
        if (StringUtils.hasText(input)) {
            setter.accept(credentialCrypto.encrypt(input));
        } else if (creating) {
            setter.accept(null);
        } else {
            setter.accept(oldValue);
        }
    }

    private AiConfigVO toVO(AiServiceConfig entity) {
        AiConfigVO vo = new AiConfigVO();
        vo.setId(entity.getId());
        vo.setServiceType(entity.getServiceType());
        vo.setProvider(entity.getProvider());
        vo.setProtocol(entity.getProtocol());
        vo.setBaseUrl(entity.getBaseUrl());
        vo.setApiKey(maskEncrypted(entity.getApiKey()));
        vo.setAccessKeyId(maskEncrypted(entity.getAccessKeyId()));
        vo.setHasSecret(StringUtils.hasText(entity.getAccessKeySecret()));
        vo.setAppKey(entity.getAppKey());
        vo.setRegion(entity.getRegion());
        vo.setModelName(entity.getModelName());
        vo.setDatasetId(entity.getDatasetId());
        vo.setExtraConfig(entity.getExtraConfig());
        vo.setTimeoutMs(entity.getTimeoutMs());
        vo.setRetryCount(entity.getRetryCount());
        vo.setCapabilityVerified(entity.getCapabilityVerified());
        vo.setVerifiedTime(entity.getVerifiedTime());
        vo.setVerifyMsg(entity.getVerifyMsg());
        vo.setIsDefault(entity.getIsDefault());
        vo.setEnabled(entity.getEnabled());
        vo.setRemark(entity.getRemark());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }

    private String maskEncrypted(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return MaskUtil.maskKeepLast4(credentialCrypto.decrypt(value));
    }
}
