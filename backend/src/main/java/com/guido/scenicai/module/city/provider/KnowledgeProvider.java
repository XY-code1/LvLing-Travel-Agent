package com.guido.scenicai.module.city.provider;

import java.util.List;

public interface KnowledgeProvider {
    List<String> list(Long cityId);
}
