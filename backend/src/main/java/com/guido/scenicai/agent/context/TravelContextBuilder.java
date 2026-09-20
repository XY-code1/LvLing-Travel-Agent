package com.guido.scenicai.agent.context;

import com.guido.scenicai.agent.api.TravelAgentRequest;
import com.guido.scenicai.agent.intent.TravelIntent;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TravelContextBuilder {
    public TravelContext build(TravelAgentRequest request) {
        return new TravelContext(request.getCity(), request.getDate(), request.getDurationDays(),
                request.getBudget(), request.getTravelers(), copy(request.getPreferences()),
                copy(request.getConstraints()), request.getConversationId(), request.getLongitude(), request.getLatitude());
    }

    public TravelContext enrich(TravelContext context, TravelIntent intent) {
        return new TravelContext(first(intent.city(), context.city()), first(context.date(), intent.date()),
                first(context.durationDays(), intent.durationDays()), first(context.budget(), intent.budget()),
                first(context.travelers(), intent.travelers()), merge(context.preferences(), intent.preferences()),
                merge(context.constraints(), intent.constraints()), context.conversationId(),
                context.longitude(), context.latitude());
    }

    private <T> T first(T explicit, T inferred) {
        return explicit != null && (!(explicit instanceof String value) || !value.isBlank()) ? explicit : inferred;
    }

    private List<String> copy(List<String> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    private List<String> merge(List<String> first, List<String> second) {
        return java.util.stream.Stream.concat(first.stream(), second.stream()).distinct().toList();
    }
}
