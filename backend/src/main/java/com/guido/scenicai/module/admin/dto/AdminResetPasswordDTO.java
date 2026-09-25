package com.guido.scenicai.module.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminResetPasswordDTO {
    @NotBlank private String username;
    @NotBlank @Size(min = 6, max = 32) private String newPassword;
    @NotBlank private String captchaId;
    @NotBlank private String captchaCode;
}
