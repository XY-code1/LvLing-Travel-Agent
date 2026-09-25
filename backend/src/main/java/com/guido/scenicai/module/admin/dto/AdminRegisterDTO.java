package com.guido.scenicai.module.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminRegisterDTO {
    @NotBlank @Size(min = 3, max = 50)
    private String username;
    @NotBlank @Size(min = 6, max = 32)
    private String password;
    private String realName;
    @NotBlank private String captchaId;
    @NotBlank private String captchaCode;
}
