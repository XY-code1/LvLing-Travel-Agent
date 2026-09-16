package com.guido.scenicai.module.city.provider;

import com.guido.scenicai.module.feature.entity.AdminFeatureItem;

import java.util.List;

public interface ServiceProvider {
    List<AdminFeatureItem> list(Long cityId, String moduleType);
}
