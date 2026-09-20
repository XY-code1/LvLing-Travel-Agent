package com.guido.scenicai.agent.skill;

import com.guido.scenicai.agent.intent.IntentExtractor;
import com.guido.scenicai.agent.context.TravelContext;

import java.math.BigDecimal;
import java.util.List;

/** Stable Skill-level view; parsing remains owned by the existing IntentExtractor. */
public record TravelIntent(
        String city,
        String date,
        Integer duration,
        String travelers,
        BigDecimal budget,
        List<String> interests,
        List<String> mustVisit,
        String mobility,
        List<String> preferences) {
    public static TravelIntent from(com.guido.scenicai.agent.intent.TravelIntent source) {
        return new TravelIntent(source.city(), source.date(), source.durationDays(), source.travelers(), source.budget(),
                source.preferences(), source.requestedPois(), source.mobilityConstraint() ? "LOW" : "NORMAL", source.constraints());
    }
}
