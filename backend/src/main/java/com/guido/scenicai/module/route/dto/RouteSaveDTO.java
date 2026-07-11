package com.guido.scenicai.module.route.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RouteSaveDTO {

    @NotNull(message = "所属景区不能为空")
    private Long scenicId;

    @NotBlank(message = "路线名称不能为空")
    @Size(max = 100, message = "路线名称不能超过100个字符")
    private String name;

    @NotNull(message = "路线类型不能为空")
    private Integer type;

    private String intro;
    private Integer estimateMinutes;
    private String suitCrowd;
    private String interestTags;
    private String recommendReason;
    private String notice;
    private Integer status;
}
