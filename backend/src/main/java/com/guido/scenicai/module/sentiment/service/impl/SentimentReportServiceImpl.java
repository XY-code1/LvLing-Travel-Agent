package com.guido.scenicai.module.sentiment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guido.scenicai.common.exception.BizException;
import com.guido.scenicai.module.dashboard.entity.DemoSwitch;
import com.guido.scenicai.module.dashboard.mapper.DemoSwitchMapper;
import com.guido.scenicai.module.feedback.entity.TouristFeedback;
import com.guido.scenicai.module.feedback.mapper.TouristFeedbackMapper;
import com.guido.scenicai.module.sentiment.dto.SentimentGenerateDTO;
import com.guido.scenicai.module.sentiment.entity.SentimentReport;
import com.guido.scenicai.module.sentiment.mapper.SentimentReportMapper;
import com.guido.scenicai.module.sentiment.service.SentimentReportService;
import com.guido.scenicai.module.sentiment.vo.SentimentReportVO;
import com.guido.scenicai.module.tourist.entity.ChatMessage;
import com.guido.scenicai.module.tourist.mapper.ChatMessageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
public class SentimentReportServiceImpl implements SentimentReportService {

    private static final String REPORT_DEMO = "REPORT_DEMO";

    private final SentimentReportMapper sentimentReportMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final TouristFeedbackMapper touristFeedbackMapper;
    private final DemoSwitchMapper demoSwitchMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public SentimentReportVO generate(SentimentGenerateDTO dto) {
        LocalDate date = parseDate(dto.getDate());
        if (isReportDemoEnabled()) {
            return demoReport(date);
        }
        TimeRange range = TimeRange.of(date);
        List<ChatMessage> messages = chatMessageMapper.selectList(new LambdaQueryWrapper<ChatMessage>()
                .ge(ChatMessage::getCreateTime, range.start())
                .lt(ChatMessage::getCreateTime, range.end()));
        List<TouristFeedback> feedbacks = touristFeedbackMapper.selectList(
                new LambdaQueryWrapper<TouristFeedback>()
                        .ge(TouristFeedback::getCreateTime, range.start())
                        .lt(TouristFeedback::getCreateTime, range.end()));
        SentimentReport report = findOrCreate(date);
        fillCounts(report, messages, feedbacks);
        report.setHotQuestions(toJson(topQuestions(messages)));
        report.setHotSpots(toJson(topSpots(messages)));
        report.setUnanswered(toJson(unanswered(messages)));
        report.setAiSuggestion(buildSuggestion(report, messages));
        if (report.getId() == null) {
            sentimentReportMapper.insert(report);
        } else {
            sentimentReportMapper.updateById(report);
        }
        return toVO(report);
    }

    @Override
    public SentimentReportVO report(String date) {
        LocalDate reportDate = parseDate(date);
        if (isReportDemoEnabled()) {
            return demoReport(reportDate);
        }
        SentimentReport report = sentimentReportMapper.selectOne(new LambdaQueryWrapper<SentimentReport>()
                .eq(SentimentReport::getReportDate, reportDate)
                .last("LIMIT 1"));
        if (report == null) {
            throw BizException.notFound("感受度报告不存在");
        }
        return toVO(report);
    }

    @Override
    @Transactional
    public byte[] generateDocx(SentimentGenerateDTO dto) {
        SentimentReportVO report = generate(dto);
        return buildDocx(report);
    }

    private boolean isReportDemoEnabled() {
        DemoSwitch demo = demoSwitchMapper.selectOne(new LambdaQueryWrapper<DemoSwitch>()
                .eq(DemoSwitch::getSwitchKey, REPORT_DEMO)
                .last("LIMIT 1"));
        return demo != null && demo.getEnabled() != null && demo.getEnabled() == 1;
    }

    private SentimentReportVO demoReport(LocalDate date) {
        SentimentReport report = new SentimentReport();
        report.setId(0L);
        report.setReportDate(date);
        report.setPositiveCount(210);
        report.setNeutralCount(480);
        report.setNegativeCount(30);
        report.setComplaintCount(5);
        report.setHotQuestions(toJson(List.of(
                mapOf("question", "灵山大佛多高", "count", 45),
                mapOf("question", "九龙灌浴几点开放", "count", 28))));
        report.setHotSpots(toJson(List.of(
                mapOf("spotName", "灵山大佛", "count", 120),
                mapOf("spotName", "九龙灌浴", "count", 86))));
        report.setUnanswered(toJson(List.of(
                mapOf("messageId", 0, "question", "夜游路线是否有无障碍通道"))));
        report.setAiSuggestion("演示数据：优先补齐夜游、无障碍和排队疏导相关知识，并在游客端增加高峰提示话术。");
        return toVO(report);
    }

    private SentimentReport findOrCreate(LocalDate date) {
        SentimentReport report = sentimentReportMapper.selectOne(new LambdaQueryWrapper<SentimentReport>()
                .eq(SentimentReport::getReportDate, date)
                .last("LIMIT 1"));
        if (report == null) {
            report = new SentimentReport();
            report.setReportDate(date);
        }
        return report;
    }

    private void fillCounts(SentimentReport report, List<ChatMessage> messages,
                            List<TouristFeedback> feedbacks) {
        List<String> emotions = new ArrayList<>();
        messages.stream().map(ChatMessage::getEmotion).filter(StringUtils::hasText).forEach(emotions::add);
        feedbacks.stream().map(TouristFeedback::getEmotion).filter(StringUtils::hasText).forEach(emotions::add);
        report.setPositiveCount(count(emotions, "POSITIVE"));
        report.setNeutralCount(count(emotions, "NEUTRAL"));
        report.setNegativeCount(count(emotions, "NEGATIVE"));
        report.setComplaintCount(count(emotions, "COMPLAINT"));
    }

    private List<Map<String, Object>> topQuestions(List<ChatMessage> messages) {
        return messages.stream()
                .map(ChatMessage::getQuestion)
                .filter(StringUtils::hasText)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .map(entry -> mapOf("question", entry.getKey(), "count", entry.getValue()))
                .toList();
    }

    private List<Map<String, Object>> topSpots(List<ChatMessage> messages) {
        return messages.stream()
                .map(ChatMessage::getSources)
                .filter(StringUtils::hasText)
                .flatMap(value -> extractSpotNames(value).stream())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .map(entry -> mapOf("spotName", entry.getKey(), "count", entry.getValue()))
                .toList();
    }

    private List<Map<String, Object>> unanswered(List<ChatMessage> messages) {
        return messages.stream()
                .filter(message -> message.getNeedSupplement() != null && message.getNeedSupplement() == 1)
                .filter(message -> StringUtils.hasText(message.getQuestion()))
                .sorted(Comparator.comparing(ChatMessage::getCreateTime).reversed())
                .limit(20)
                .map(message -> mapOf("messageId", message.getId(), "question", message.getQuestion()))
                .toList();
    }

    private String buildSuggestion(SentimentReport report, List<ChatMessage> messages) {
        List<String> suggestions = new ArrayList<>();
        if (report.getComplaintCount() > 0 || report.getNegativeCount() > 0) {
            suggestions.add("优先复盘消极和投诉对话，补充服务解释与安抚话术。");
        }
        long unansweredCount = messages.stream()
                .filter(message -> message.getNeedSupplement() != null && message.getNeedSupplement() == 1)
                .count();
        if (unansweredCount > 0) {
            suggestions.add("将未命中问题整理进知识库，优先补齐高频景点资料。");
        }
        if (suggestions.isEmpty()) {
            suggestions.add("当日情绪整体平稳，建议保持知识库更新和热门问题巡检。");
        }
        return String.join("", suggestions);
    }

    private List<String> extractSpotNames(String sources) {
        List<String> names = new ArrayList<>();
        try {
            objectMapper.readTree(sources).forEach(node -> {
                String spotName = node.path("spotName").asText(null);
                if (StringUtils.hasText(spotName)) {
                    names.add(spotName);
                }
            });
        } catch (Exception e) {
            return names;
        }
        return names;
    }

    private int count(List<String> values, String target) {
        return Math.toIntExact(values.stream().filter(target::equals).count());
    }

    private LocalDate parseDate(String date) {
        try {
            return LocalDate.parse(date);
        } catch (DateTimeParseException e) {
            throw new BizException(400, "日期格式必须为 yyyy-MM-dd");
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new BizException(500, "JSON 序列化失败", e);
        }
    }

    private Map<String, Object> mapOf(Object... values) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) {
            map.put(String.valueOf(values[i]), values[i + 1]);
        }
        return map;
    }

    private SentimentReportVO toVO(SentimentReport report) {
        SentimentReportVO vo = new SentimentReportVO();
        vo.setId(report.getId());
        vo.setReportDate(report.getReportDate());
        vo.setPositiveCount(report.getPositiveCount());
        vo.setNeutralCount(report.getNeutralCount());
        vo.setNegativeCount(report.getNegativeCount());
        vo.setComplaintCount(report.getComplaintCount());
        vo.setHotQuestions(report.getHotQuestions());
        vo.setHotSpots(report.getHotSpots());
        vo.setUnanswered(report.getUnanswered());
        vo.setAiSuggestion(report.getAiSuggestion());
        vo.setCreateTime(report.getCreateTime());
        vo.setUpdateTime(report.getUpdateTime());
        return vo;
    }

    private byte[] buildDocx(SentimentReportVO report) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (ZipOutputStream zip = new ZipOutputStream(out, StandardCharsets.UTF_8)) {
                writeZipEntry(zip, "[Content_Types].xml", """
                        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                        <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                          <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                          <Default Extension="xml" ContentType="application/xml"/>
                          <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
                        </Types>
                        """);
                writeZipEntry(zip, "_rels/.rels", """
                        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                        <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                          <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
                        </Relationships>
                        """);
                writeZipEntry(zip, "word/document.xml", buildDocumentXml(report));
            }
            return out.toByteArray();
        } catch (Exception e) {
            throw new BizException(500, "Word 报告生成失败：" + e.getMessage(), e);
        }
    }

    private void writeZipEntry(ZipOutputStream zip, String name, String content) throws Exception {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private String buildDocumentXml(SentimentReportVO report) {
        List<String> paragraphs = new ArrayList<>();
        paragraphs.add("景区游客感受度分析报告");
        paragraphs.add("报告日期：" + report.getReportDate());
        paragraphs.add("情绪统计：正向 " + report.getPositiveCount()
                + "，中性 " + report.getNeutralCount()
                + "，负向 " + report.getNegativeCount()
                + "，投诉 " + report.getComplaintCount());
        paragraphs.add("热点问题");
        paragraphs.addAll(jsonList(report.getHotQuestions(), "question"));
        paragraphs.add("热点景点");
        paragraphs.addAll(jsonList(report.getHotSpots(), "spotName"));
        paragraphs.add("未回答问题");
        paragraphs.addAll(jsonList(report.getUnanswered(), "question"));
        paragraphs.add("AI 建议：" + nullToEmpty(report.getAiSuggestion()));
        paragraphs.add("规则说明：热点问题按当前报告日期内游客对话问题文本分组计数并取前 10；热点景点按当前报告日期内对话来源 sources 中的 spotName 分组计数并取前 10；未回答问题来自 needSupplement=1 的对话。");

        String body = paragraphs.stream()
                .map(this::paragraphXml)
                .collect(Collectors.joining());
        return """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
                  <w:body>
                """ + body + """
                    <w:sectPr><w:pgSz w:w="11906" w:h="16838"/><w:pgMar w:top="1440" w:right="1440" w:bottom="1440" w:left="1440"/></w:sectPr>
                  </w:body>
                </w:document>
                """;
    }

    private String paragraphXml(String text) {
        return "<w:p><w:r><w:t>" + escapeXml(text) + "</w:t></w:r></w:p>";
    }

    private List<String> jsonList(String json, String nameField) {
        if (!StringUtils.hasText(json)) {
            return List.of("暂无");
        }
        try {
            JsonNode root = objectMapper.readTree(json);
            if (!root.isArray() || root.size() == 0) {
                return List.of("暂无");
            }
            List<String> items = new ArrayList<>();
            int index = 1;
            for (JsonNode node : root) {
                String name = node.path(nameField).asText("");
                long count = node.path("count").asLong(-1);
                if (!StringUtils.hasText(name)) {
                    name = node.path("question").asText(node.toString());
                }
                items.add(index++ + ". " + name + (count >= 0 ? "（" + count + " 次）" : ""));
            }
            return items;
        } catch (Exception e) {
            return List.of(json);
        }
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String escapeXml(String value) {
        return nullToEmpty(value)
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private record TimeRange(LocalDateTime start, LocalDateTime end) {
        static TimeRange of(LocalDate date) {
            return new TimeRange(date.atStartOfDay(), date.plusDays(1).atStartOfDay());
        }
    }
}
