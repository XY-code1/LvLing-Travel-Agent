package com.guido.scenicai.module.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 登录日志实体（对应 sys_login_log 表）。
 */
@Data
@TableName("sys_login_log")
public class SysLoginLogEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;
    private String ip;
    private String userAgent;
    private Integer status;
    private String msg;
    private LocalDateTime createTime;
}
