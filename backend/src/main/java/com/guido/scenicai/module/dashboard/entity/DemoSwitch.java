package com.guido.scenicai.module.dashboard.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guido.scenicai.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("demo_switch")
public class DemoSwitch extends BaseEntity {

    private String switchKey;
    private Integer enabled;
    private String remark;
}
