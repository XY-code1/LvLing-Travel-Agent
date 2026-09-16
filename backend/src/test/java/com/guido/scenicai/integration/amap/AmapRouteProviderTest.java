package com.guido.scenicai.integration.amap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guido.scenicai.common.exception.BizException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AmapRouteProviderTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void aggregatesRealPolylineAcrossSegments() throws Exception {
        AmapWebServiceClient client = mock(AmapWebServiceClient.class);
        when(client.get(eq("/v5/direction/walking"), anyMap()))
                .thenReturn(objectMapper.readTree(response("100", "60", "120.1,30.1;120.2,30.2")))
                .thenReturn(objectMapper.readTree(response("200", "120", "120.2,30.2;120.3,30.3")));

        AmapRouteProvider.Route route = new AmapRouteProvider(client).walking(List.of(
                new AmapPoiProvider.Coordinate(120.1, 30.1),
                new AmapPoiProvider.Coordinate(120.2, 30.2),
                new AmapPoiProvider.Coordinate(120.3, 30.3)));

        assertEquals(300, route.distanceMeters());
        assertEquals(180, route.durationSeconds());
        assertEquals(3, route.polyline().size());
    }

    @Test
    void rejectsEmptyAmapRouteInsteadOfReturningFakeSuccess() throws Exception {
        AmapWebServiceClient client = mock(AmapWebServiceClient.class);
        when(client.get(eq("/v5/direction/walking"), anyMap()))
                .thenReturn(objectMapper.readTree("{\"route\":{\"paths\":[]}}"));

        assertThrows(BizException.class, () -> new AmapRouteProvider(client).walking(List.of(
                new AmapPoiProvider.Coordinate(120.1, 30.1),
                new AmapPoiProvider.Coordinate(120.2, 30.2))));
    }

    private String response(String distance, String duration, String polyline) {
        return "{\"route\":{\"paths\":[{\"distance\":\"" + distance
                + "\",\"cost\":{\"duration\":\"" + duration
                + "\"},\"steps\":[{\"polyline\":\"" + polyline + "\"}]}]}}";
    }
}
