package com.guido.scenicai.module.sos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class SosCreateDTO {
    private Long cityId; private Long scenicId;
    @NotBlank private String helpType;
    @NotBlank private String urgency;
    @NotBlank @Size(max = 255) private String locationText;
    private BigDecimal longitude; private BigDecimal latitude;
    @NotBlank @Pattern(regexp = "^[0-9+\\- ]{6,24}$", message = "联系电话格式不正确") private String phone;
    @NotBlank @Size(max = 2000) private String description;
}
