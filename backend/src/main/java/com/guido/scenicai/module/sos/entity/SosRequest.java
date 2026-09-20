package com.guido.scenicai.module.sos.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guido.scenicai.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;

@Data @EqualsAndHashCode(callSuper = true) @TableName("sos_request")
public class SosRequest extends BaseEntity {
    private String requestNo; private Long userId; private Long cityId; private Long scenicId;
    private String helpType; private String urgency; private String locationText;
    private BigDecimal longitude; private BigDecimal latitude; private String phone;
    private String description; private String status;
}
