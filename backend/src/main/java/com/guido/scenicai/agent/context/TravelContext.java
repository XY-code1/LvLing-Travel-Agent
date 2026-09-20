package com.guido.scenicai.agent.context;

import java.math.BigDecimal;
import java.util.List;

public record TravelContext(
        String city,
        String date,
        Integer durationDays,
        BigDecimal budget,
        String travelers,
        List<String> preferences,
        List<String> constraints,
        String conversationId,
        Double longitude,
        Double latitude) {
}
