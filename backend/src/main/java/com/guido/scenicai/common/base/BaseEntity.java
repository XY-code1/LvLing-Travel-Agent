package com.guido.scenicai.common.base;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 公共字段基类 — 每张业务表必须包含的 6 个公共字段。
 */
@Data
public abstract class BaseEntity {

    /** 主键，AUTO_INCREMENT */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 创建人 admin id（游客侧数据可空） */
    private Long createBy;

    /** 更新人 admin id */
    private Long updateBy;

    /** 逻辑删除：0=未删，1=已删 */
    @TableLogic
    private Integer deleted;
}
