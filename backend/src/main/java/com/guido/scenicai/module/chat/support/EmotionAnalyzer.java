package com.guido.scenicai.module.chat.support;

import org.springframework.stereotype.Component;

@Component
public class EmotionAnalyzer {

    public String analyze(String text) {
        if (text == null || text.isBlank()) {
            return "NEUTRAL";
        }
        String value = text.toLowerCase();
        if (containsAny(value, "投诉", "骗人", "垃圾", "欺骗", "退票")) {
            return "COMPLAINT";
        }
        if (containsAny(value, "生气", "失望", "难受", "不好", "差评", "太差", "糟糕", "排队太久")) {
            return "NEGATIVE";
        }
        if (containsAny(value, "开心", "喜欢", "满意", "漂亮", "震撼")) {
            return "POSITIVE";
        }
        return "NEUTRAL";
    }

    public String analyzeInteraction(String userInput, String answer) {
        String inputEmotion = analyze(userInput);
        return "NEUTRAL".equals(inputEmotion) ? analyze(answer) : inputEmotion;
    }

    public String adjustTone(String answer, String emotion) {
        String lead = comfortLead(emotion);
        if (lead.isBlank() || answer == null || answer.startsWith(lead)) {
            return answer;
        }
        return lead + answer;
    }

    public String comfortLead(String emotion) {
        if ("COMPLAINT".equals(emotion)) {
            return "非常抱歉给你带来困扰，我会尽量用明确的信息帮你解决。";
        }
        if ("NEGATIVE".equals(emotion)) {
            return "很抱歉让你有不好的体验，我先帮你把问题处理清楚。";
        }
        return "";
    }

    private boolean containsAny(String value, String... words) {
        for (String word : words) {
            if (value.contains(word)) {
                return true;
            }
        }
        return false;
    }
}
