package com.guido.scenicai.module.knowledge.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guido.scenicai.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("kb_test_record")
public class KbTestRecord extends BaseEntity {

    private String question;
    private String answer;
    private Integer hit;
    private String sources;
    private Integer costMs;
}
