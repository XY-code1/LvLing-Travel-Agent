package com.guido.scenicai.module.scenic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ScenicSaveDTO {

    @NotBlank(message = "景区名称不能为空")
    @Size(max = 100, message = "景区名称不能超过100个字符")
    private String name;

    private String intro;
    private String address;
    private String openTime;
    private String ticketInfo;
    private String trafficInfo;
    private String servicePhone;
    private String notice;
    private String coverImage;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private Integer status;
}
