package com.guido.scenicai.module.admin.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminLoginVO {

    private String token;
    private AdminInfoVO adminInfo;
}
