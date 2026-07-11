package com.guido.scenicai.module.chat.service.impl;

import java.util.LinkedHashMap;
import java.util.Map;

final class MapData {

    private MapData() {
    }

    static Map<String, Object> of(Object... values) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) {
            map.put(String.valueOf(values[i]), values[i + 1]);
        }
        return map;
    }
}
