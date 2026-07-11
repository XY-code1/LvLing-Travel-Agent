package com.guido.scenicai.module.admin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guido.scenicai.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_admin")
public class SysAdmin extends BaseEntity {

    private String username;
    private String password;
    private String realName;
    private String role;
    private Integer status;
    private LocalDateTime lastLoginTime;
}
