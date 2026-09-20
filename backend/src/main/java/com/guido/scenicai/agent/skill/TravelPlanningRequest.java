package com.guido.scenicai.agent.skill;

import java.math.BigDecimal;
import java.util.List;

public record TravelPlanningRequest(
        String city,
        String date,
        Integer duration,
        String travelers,
        BigDecimal budget,
        List<String> interests,
        List<String> mustVisit,
        String mobility,
        List<String> preferences,
        String message,
        Double longitude,
        Double latitude) {
    public TravelPlanningRequest {
        interests = interests == null ? List.of() : List.copyOf(interests);
        mustVisit = mustVisit == null ? List.of() : List.copyOf(mustVisit);
        preferences = preferences == null ? List.of() : List.copyOf(preferences);
    }
}
