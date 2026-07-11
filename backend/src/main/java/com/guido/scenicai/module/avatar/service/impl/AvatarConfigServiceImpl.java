package com.guido.scenicai.module.avatar.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guido.scenicai.common.exception.BizException;
import com.guido.scenicai.integration.aliyun.AliyunSpeechClient;
import com.guido.scenicai.integration.aliyun.TtsResult;
import com.guido.scenicai.module.avatar.dto.AvatarSaveDTO;
import com.guido.scenicai.module.avatar.dto.AvatarTestDTO;
import com.guido.scenicai.module.avatar.entity.AvatarConfig;
import com.guido.scenicai.module.avatar.mapper.AvatarConfigMapper;
import com.guido.scenicai.module.avatar.service.AvatarConfigService;
import com.guido.scenicai.module.avatar.vo.AvatarConfigVO;
import com.guido.scenicai.module.avatar.vo.AvatarTestVO;
import com.guido.scenicai.module.tourist.entity.TouristUser;
import com.guido.scenicai.module.tourist.mapper.TouristUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AvatarConfigServiceImpl implements AvatarConfigService {

    private static final String LOCAL_CANVAS_PROVIDER = "本地Canvas数字人";
    private static final String GENDER_MALE = "MALE";
    private static final String GENDER_FEMALE = "FEMALE";
    private static final String DEFAULT_MALE_VOICE = "xiaogang";
    private static final String DEFAULT_FEMALE_VOICE = "xiaoyun";
    private static final Set<String> MALE_VOICES = Set.of("xiaogang", "aicheng", "aida", "aitong", "aijia");
    private static final Set<String> FEMALE_VOICES = Set.of("xiaoyun", "xiaomei", "xiaoxue", "ruoxi", "aixia", "aiqi", "aixiaobei");
    private static final String DEFAULT_RENDER_CONFIG = """
            {"mouth":{"x":0.5,"y":0.13,"width":0.048,"height":0.018},"crop":{"x":0.5,"y":0.24,"scale":2.6},"breath":0.012,"blink":true}
            """.trim();

    private final AvatarConfigMapper avatarConfigMapper;
    private final TouristUserMapper touristUserMapper;
    private final AliyunSpeechClient aliyunSpeechClient;

    @Override
    public List<AvatarConfigVO> list() {
        return avatarConfigMapper.selectList(new LambdaQueryWrapper<AvatarConfig>()
                        .orderByDesc(AvatarConfig::getEnabled)
                        .orderByDesc(AvatarConfig::getIsDefault)
                        .orderByDesc(AvatarConfig::getId))
                .stream()
                .map(this::toNormalizedVO)
                .toList();
    }

    @Override
    @Transactional
    public AvatarConfigVO save(AvatarSaveDTO dto) {
        AvatarConfig entity = dto.getId() == null ? new AvatarConfig() : getRequired(dto.getId());
        copyFields(dto, entity);
        if (!StringUtils.hasText(entity.getProvider())) {
            entity.setProvider(LOCAL_CANVAS_PROVIDER);
        }
        entity.setGender(normalizeGender(entity.getGender()));
        if (!StringUtils.hasText(entity.getOutfitImage())) {
            entity.setOutfitImage(entity.getAvatarImage());
        }
        if (!StringUtils.hasText(entity.getRenderConfig())) {
            entity.setRenderConfig(DEFAULT_RENDER_CONFIG);
        }
        if (entity.getSpeechRate() == null) {
            entity.setSpeechRate(0);
        }
        if (entity.getEnabled() == null) {
            entity.setEnabled(1);
        }
        if (entity.getIsDefault() == null) {
            entity.setIsDefault(0);
        }
        normalizeVoice(entity);
        if (dto.getId() == null) {
            avatarConfigMapper.insert(entity);
        } else {
            avatarConfigMapper.updateById(entity);
        }
        return toVO(entity);
    }

    @Override
    @Transactional
    public void updateEnabled(Long id, Integer enabled) {
        AvatarConfig target = getRequired(id);
        int nextEnabled = enabled != null && enabled == 1 ? 1 : 0;
        if (nextEnabled == 0 && enabledCount() <= 1 && target.getEnabled() != null && target.getEnabled() == 1) {
            throw new BizException(400, "至少保留一个可供游客选择的数字人形象");
        }
        target.setEnabled(nextEnabled);
        avatarConfigMapper.updateById(target);
    }

    @Override
    public AvatarTestVO test(AvatarTestDTO dto) {
        AvatarConfig avatar = getRequired(dto.getId());
        if (avatar.getEnabled() == null || avatar.getEnabled() != 1) {
            throw new BizException(400, "数字人形象未启用");
        }
        normalizeVoice(avatar);
        long start = System.nanoTime();
        try {
            TtsResult result = aliyunSpeechClient.synthesize(dto.getText(), avatar.getVoice(), avatar.getSpeechRate());
            return new AvatarTestVO(null, result.getAudioUrl(), elapsedMs(start),
                    "测试音频已生成，游客端将用本地 Canvas 数字人播放口型");
        } catch (BizException e) {
            return new AvatarTestVO(null, null, elapsedMs(start),
                    "本地数字人配置有效，TTS 未生成：" + e.getMessage());
        }
    }

    @Override
    public AvatarConfigVO getDefault() {
        AvatarConfig avatar = avatarConfigMapper.selectOne(new LambdaQueryWrapper<AvatarConfig>()
                .eq(AvatarConfig::getEnabled, 1)
                .eq(AvatarConfig::getIsDefault, 1)
                .last("LIMIT 1"));
        return avatar == null ? null : toNormalizedVO(avatar);
    }

    @Override
    public AvatarConfigVO getDefaultWithDetail() {
        return getDefault();
    }

    @Override
    public AvatarConfigVO getEnabledById(Long id) {
        if (id == null) {
            return null;
        }
        AvatarConfig avatar = avatarConfigMapper.selectOne(new LambdaQueryWrapper<AvatarConfig>()
                .eq(AvatarConfig::getId, id)
                .eq(AvatarConfig::getEnabled, 1)
                .last("LIMIT 1"));
        return avatar == null ? null : toNormalizedVO(avatar);
    }

    @Override
    public List<AvatarConfigVO> listEnabled() {
        return avatarConfigMapper.selectList(new LambdaQueryWrapper<AvatarConfig>()
                        .eq(AvatarConfig::getEnabled, 1)
                        .orderByDesc(AvatarConfig::getIsDefault)
                        .orderByAsc(AvatarConfig::getGender)
                        .orderByDesc(AvatarConfig::getId))
                .stream()
                .map(this::toNormalizedVO)
                .toList();
    }

    @Override
    @Transactional
    public AvatarConfigVO getCurrentForTourist(Long touristId) {
        TouristUser user = getTourist(touristId);
        AvatarConfigVO selected = getEnabledById(user.getAvatarConfigId());
        if (selected != null) {
            return selected;
        }
        AvatarConfigVO fallback = enabledFallback();
        if (fallback == null) {
            throw new BizException(400, "暂无可用数字人形象");
        }
        user.setAvatarConfigId(fallback.getId());
        touristUserMapper.updateById(user);
        return fallback;
    }

    @Override
    @Transactional
    public AvatarConfigVO selectForTourist(Long touristId, Long avatarId) {
        TouristUser user = getTourist(touristId);
        AvatarConfigVO avatar = getEnabledById(avatarId);
        if (avatar == null) {
            throw new BizException(400, "该数字人形象未启用，不能选择");
        }
        user.setAvatarConfigId(avatar.getId());
        touristUserMapper.updateById(user);
        return avatar;
    }

    @Override
    @Transactional
    public AvatarConfigVO resolveForTourist(Long touristId, Long requestedAvatarId) {
        if (requestedAvatarId != null) {
            return selectForTourist(touristId, requestedAvatarId);
        }
        return getCurrentForTourist(touristId);
    }

    private AvatarConfig getRequired(Long id) {
        AvatarConfig entity = avatarConfigMapper.selectById(id);
        if (entity == null) {
            throw BizException.notFound("数字人形象配置不存在");
        }
        return entity;
    }

    private void copyFields(AvatarSaveDTO dto, AvatarConfig entity) {
        entity.setName(dto.getName());
        entity.setProvider(dto.getProvider());
        entity.setInstanceId(dto.getInstanceId());
        entity.setAvatarImage(dto.getAvatarImage());
        entity.setGender(dto.getGender());
        entity.setAppearance(dto.getAppearance());
        entity.setOutfit(dto.getOutfit());
        entity.setOutfitImage(dto.getOutfitImage());
        entity.setRenderConfig(dto.getRenderConfig());
        entity.setVoice(dto.getVoice());
        entity.setSpeechRate(dto.getSpeechRate());
        entity.setWelcomeText(dto.getWelcomeText());
        entity.setEnabled(dto.getEnabled());
        entity.setRemark(dto.getRemark());
    }

    private AvatarConfigVO toVO(AvatarConfig entity) {
        AvatarConfigVO vo = new AvatarConfigVO();
        vo.setId(entity.getId());
        vo.setName(entity.getName());
        vo.setProvider(entity.getProvider());
        vo.setInstanceId(entity.getInstanceId());
        vo.setAvatarImage(entity.getAvatarImage());
        vo.setGender(entity.getGender());
        vo.setAppearance(entity.getAppearance());
        vo.setOutfit(entity.getOutfit());
        vo.setOutfitImage(entity.getOutfitImage());
        vo.setRenderConfig(entity.getRenderConfig());
        vo.setVoice(entity.getVoice());
        vo.setSpeechRate(entity.getSpeechRate());
        vo.setWelcomeText(entity.getWelcomeText());
        vo.setIsDefault(entity.getIsDefault());
        vo.setEnabled(entity.getEnabled());
        vo.setRemark(entity.getRemark());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }

    private AvatarConfigVO toNormalizedVO(AvatarConfig entity) {
        normalizeVoice(entity);
        return toVO(entity);
    }

    private TouristUser getTourist(Long touristId) {
        TouristUser user = touristUserMapper.selectById(touristId);
        if (user == null) {
            throw BizException.notFound("游客不存在");
        }
        return user;
    }

    private AvatarConfigVO enabledFallback() {
        AvatarConfigVO defaultAvatar = getDefault();
        if (defaultAvatar != null) {
            return defaultAvatar;
        }
        AvatarConfig avatar = avatarConfigMapper.selectOne(new LambdaQueryWrapper<AvatarConfig>()
                .eq(AvatarConfig::getEnabled, 1)
                .orderByDesc(AvatarConfig::getId)
                .last("LIMIT 1"));
        return avatar == null ? null : toNormalizedVO(avatar);
    }

    private long enabledCount() {
        return avatarConfigMapper.selectCount(new LambdaQueryWrapper<AvatarConfig>()
                .eq(AvatarConfig::getEnabled, 1));
    }

    private String normalizeGender(String gender) {
        if (GENDER_MALE.equalsIgnoreCase(gender)) {
            return GENDER_MALE;
        }
        return GENDER_FEMALE;
    }

    private void normalizeVoice(AvatarConfig avatar) {
        avatar.setGender(normalizeGender(avatar.getGender()));
        if (!StringUtils.hasText(avatar.getVoice())) {
            avatar.setVoice(defaultVoice(avatar.getGender()));
            return;
        }
        String normalizedVoice = normalizeVoiceCode(avatar.getVoice());
        if (!voiceMatchesGender(avatar.getGender(), normalizedVoice)) {
            throw new BizException(400, "数字人形象和音色性别不匹配，请选择对应性别的音色");
        }
        avatar.setVoice(normalizedVoice);
    }

    private String normalizeVoiceCode(String voice) {
        String raw = voice == null ? "" : voice.trim();
        if (raw.contains("男")) {
            return DEFAULT_MALE_VOICE;
        }
        if (raw.contains("女")) {
            return DEFAULT_FEMALE_VOICE;
        }
        return raw;
    }

    private String defaultVoice(String gender) {
        return GENDER_MALE.equals(gender) ? DEFAULT_MALE_VOICE : DEFAULT_FEMALE_VOICE;
    }

    private boolean voiceMatchesGender(String gender, String voice) {
        String normalized = voice.toLowerCase(Locale.ROOT);
        if (GENDER_MALE.equals(gender)) {
            return MALE_VOICES.contains(normalized) || normalized.contains("male") || normalized.contains("男");
        }
        return FEMALE_VOICES.contains(normalized) || normalized.contains("female") || normalized.contains("女");
    }

    private int elapsedMs(long start) {
        return Math.toIntExact((System.nanoTime() - start) / 1_000_000L);
    }
}
