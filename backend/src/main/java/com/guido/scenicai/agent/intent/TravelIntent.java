package com.guido.scenicai.agent.intent;

import java.math.BigDecimal;
import java.util.List;

public record TravelIntent(
        String city,
        String date,
        Integer durationDays,
        BigDecimal budget,
        String travelers,
        String partyType,
        boolean mobilityConstraint,
        List<String> preferences,
        List<String> constraints,
        List<String> requestedPois,
        String routeOrigin,
        String routeDestination,
        String routeMode) {
}
