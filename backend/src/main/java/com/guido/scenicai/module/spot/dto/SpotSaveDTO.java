package com.guido.scenicai.module.spot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SpotSaveDTO {

    @NotNull(message = "所属景区不能为空")
    private Long scenicId;

    @NotBlank(message = "景点名称不能为空")
    @Size(max = 100, message = "景点名称不能超过100个字符")
    private String name;

    private String alias;
    private String intro;
    private String historyCulture;
    private String guideText;
    private String tags;
    private String images;
    private Integer stayMinutes;
    private String suitCrowd;
    private Integer isHot;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private Integer status;
}
