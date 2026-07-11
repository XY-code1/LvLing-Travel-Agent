package com.guido.scenicai.common.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Desensitization utility. Masks API keys, passwords, secrets in log content.
 */
public final class MaskUtil {

    private MaskUtil() {
    }

    /** JSON key pattern for sensitive fields. */
    private static final Pattern SENSITIVE_JSON_KEY = Pattern.compile(
            "(?i)\"(password|apiKey|accessKeySecret|accessKeyId|secret|token|appKey|imageBase64)\"\\s*:\\s*\"([^\"]*)\""
    );

    /** Plain-text sk- API key pattern. */
    private static final Pattern SK_KEY = Pattern.compile(
            "(?i)sk-[a-zA-Z0-9]{20,}"
    );

    /**
     * Mask sensitive fields in a string (typically JSON or log content).
     * <ul>
     *   <li>password / accessKeySecret / secret → fully masked: "****"</li>
     *   <li>apiKey / accessKeyId / token / appKey → masked, keep last 4 chars</li>
     *   <li>sk-xxx... → sk-****</li>
     * </ul>
     */
    public static String maskSensitive(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }
        // Mask sensitive JSON key-value pairs
        Matcher m = SENSITIVE_JSON_KEY.matcher(content);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String field = m.group(1).toLowerCase();
            String value = m.group(2);
            String replacement;
            if (value.isEmpty()) {
                replacement = m.group(0);
            } else if (field.contains("secret") || field.equals("password") || field.equals("imagebase64")) {
                replacement = "\"" + m.group(1) + "\":\"****\"";
            } else if (value.length() <= 4) {
                replacement = "\"" + m.group(1) + "\":\"****\"";
            } else {
                replacement = "\"" + m.group(1) + "\":\"****"
                        + value.substring(value.length() - 4) + "\"";
            }
            m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(sb);
        content = sb.toString();

        // Mask sk- prefixed keys
        content = SK_KEY.matcher(content).replaceAll("sk-****");

        return content;
    }

    /**
     * Keep only the last 4 characters, prefix with ****.
     */
    public static String maskKeepLast4(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        if (input.length() <= 4) {
            return "****";
        }
        return "****" + input.substring(input.length() - 4);
    }

    /**
     * Full mask — replace entire value with ****.
     */
    public static String maskAll(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        return "****";
    }

    /**
     * Mask phone number: 138****0000.
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() != 11) {
            return maskAll(phone);
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }
}
