package com.guido.scenicai.integration.amap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guido.scenicai.common.exception.BizException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AmapGeocodingProviderTest {
    private static final String CONVERT = "/v3/assistant/coordinate/convert";
    private static final String REGEO = "/v3/geocode/regeo";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void keepsReverseGeocodingWorkingWhenCoordinateConversionIsUnavailable() throws Exception {
        AmapWebServiceClient client = mock(AmapWebServiceClient.class);
        when(client.get(eq(CONVERT), anyMap()))
                .thenThrow(new BizException(1002, "AMAP_API_ERROR: INVALID_USER_KEY (10001)"));
        when(client.get(eq(REGEO), anyMap())).thenReturn(regeo());

        AmapGeocodingProvider.Result result = new AmapGeocodingProvider(client).reverseGeocode(120.1551, 30.2741, true);

        assertNotNull(result);
        assertEquals("330100", result.adcode());
        assertEquals(120.1551, result.coordinates().longitude(), 1e-9);
        assertEquals(30.2741, result.coordinates().latitude(), 1e-9);
        verify(client).get(eq(REGEO), argThat(parameters -> "120.1551,30.2741".equals(parameters.get("location"))));
    }

    @Test
    void fallsBackWhenConversionReturnsNoLocation() throws Exception {
        AmapWebServiceClient client = mock(AmapWebServiceClient.class);
        when(client.get(eq(CONVERT), anyMap()))
                .thenReturn(objectMapper.readTree("{\"status\":\"1\",\"locations\":\"\"}"));
        when(client.get(eq(REGEO), anyMap())).thenReturn(regeo());

        AmapGeocodingProvider.Result result = new AmapGeocodingProvider(client).reverseGeocode(120.1551, 30.2741, true);

        assertNotNull(result);
        verify(client).get(eq(REGEO), argThat(parameters -> "120.1551,30.2741".equals(parameters.get("location"))));
    }

    @Test
    void usesConvertedCoordinatesWhenConversionSucceeds() throws Exception {
        AmapWebServiceClient client = mock(AmapWebServiceClient.class);
        when(client.get(eq(CONVERT), anyMap()))
                .thenReturn(objectMapper.readTree("{\"status\":\"1\",\"locations\":\"120.1598,30.2775\"}"));
        when(client.get(eq(REGEO), anyMap())).thenReturn(regeo());

        AmapGeocodingProvider.Result result = new AmapGeocodingProvider(client).reverseGeocode(120.1551, 30.2741, true);

        assertNotNull(result);
        assertEquals(120.1598, result.coordinates().longitude(), 1e-9);
        verify(client).get(eq(REGEO), argThat(parameters -> "120.1598,30.2775".equals(parameters.get("location"))));
    }

    @Test
    void skipsConversionForGcj02Input() throws Exception {
        AmapWebServiceClient client = mock(AmapWebServiceClient.class);
        when(client.get(eq(REGEO), anyMap())).thenReturn(regeo());

        AmapGeocodingProvider.Result result = new AmapGeocodingProvider(client).reverseGeocode(120.1551, 30.2741, false);

        assertNotNull(result);
        verify(client, never()).get(eq(CONVERT), anyMap());
        verify(client).get(eq(REGEO), argThat(parameters -> "120.1551,30.2741".equals(parameters.get("location"))));
    }

    private JsonNode regeo() throws Exception {
        return objectMapper.readTree("{\"status\":\"1\",\"regeocode\":{\"formatted_address\":\"Zhejiang Hangzhou Xihu\","
                + "\"addressComponent\":{\"province\":\"Zhejiang\",\"city\":\"Hangzhou\",\"district\":\"Xihu\",\"adcode\":\"330100\"}}}");
    }
}
