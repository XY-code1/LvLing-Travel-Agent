package com.guido.scenicai.module.route.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class RouteUpdateDTO extends RouteSaveDTO {

    @NotNull(message = "路线ID不能为空")
    private Long id;
}
