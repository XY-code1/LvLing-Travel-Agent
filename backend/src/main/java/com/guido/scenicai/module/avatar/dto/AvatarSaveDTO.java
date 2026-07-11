package com.guido.scenicai.module.avatar.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AvatarSaveDTO {

    private Long id;

    @NotBlank(message = "形象名称不能为空")
    private String name;

    private String provider;
    private String instanceId;
    private String avatarImage;
    private String gender;
    private String appearance;
    private String outfit;
    private String outfitImage;
    private String renderConfig;
    private String voice;
    private Integer speechRate;
    private String welcomeText;
    private Integer enabled;
    private String remark;
}
