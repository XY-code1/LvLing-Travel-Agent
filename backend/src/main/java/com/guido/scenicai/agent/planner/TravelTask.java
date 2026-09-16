package com.guido.scenicai.agent.planner;

public record TravelTask(int order, Type type, String description, boolean required) {
    public enum Type {
        RESOLVE_CITY,
        CHECK_WEATHER,
        SEARCH_POI,
        CHECK_POI_OPENING,
        PLAN_ROUTE,
        CHECK_BUDGET,
        VALIDATE_PLAN
    }
}
