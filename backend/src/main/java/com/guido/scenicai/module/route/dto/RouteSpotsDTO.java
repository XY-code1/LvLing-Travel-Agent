package com.guido.scenicai.module.route.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class RouteSpotsDTO {

    @NotNull(message = "路线ID不能为空")
    private Long routeId;

    @NotEmpty(message = "景点列表不能为空")
    private List<Long> spotIds;
}
