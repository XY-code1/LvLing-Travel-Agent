package com.guido.scenicai.module.chat.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guido.scenicai.module.chat.vo.SourceVO;
import com.guido.scenicai.module.spot.entity.Spot;
import com.guido.scenicai.module.spot.mapper.SpotMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class LocalKnowledgeService {

    private final SpotMapper spotMapper;

    public Optional<LocalKnowledgeHit> findByQuestion(Long scenicId, Long spotId, String question) {
        if (spotId != null) {
            Spot spot = spotMapper.selectById(spotId);
            if (spot != null) {
                return Optional.of(toHit(spot));
            }
        }
        List<Spot> spots = spotMapper.selectList(new LambdaQueryWrapper<Spot>()
                .eq(scenicId != null, Spot::getScenicId, scenicId)
                .eq(Spot::getStatus, 1));
        return spots.stream()
                .filter(spot -> matches(spot, question))
                .findFirst()
                .map(this::toHit);
    }

    public Optional<LocalKnowledgeHit> findByRecognizedText(Long scenicId, String text) {
        return findByQuestion(scenicId, null, text);
    }

    private boolean matches(Spot spot, String question) {
        if (!StringUtils.hasText(question)) {
            return false;
        }
        String normalized = question.toLowerCase();
        if (contains(normalized, spot.getName())) {
            return true;
        }
        if (StringUtils.hasText(spot.getAlias())) {
            return Arrays.stream(spot.getAlias().split("[,，]"))
                    .map(String::trim)
                    .anyMatch(alias -> contains(normalized, alias));
        }
        return StringUtils.hasText(spot.getTags()) && Arrays.stream(spot.getTags().split("[,，]"))
                .map(String::trim)
                .anyMatch(tag -> contains(normalized, tag));
    }

    private boolean contains(String source, String target) {
        return StringUtils.hasText(target) && source.contains(target.toLowerCase());
    }

    private LocalKnowledgeHit toHit(Spot spot) {
        String content = firstText(spot.getGuideText(), spot.getIntro(), spot.getHistoryCulture(),
                "已识别到景点：" + spot.getName());
        SourceVO source = new SourceVO("本地景点资料", spot.getName(), content, 1.0);
        return new LocalKnowledgeHit(spot, content, source);
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return "";
    }
}
