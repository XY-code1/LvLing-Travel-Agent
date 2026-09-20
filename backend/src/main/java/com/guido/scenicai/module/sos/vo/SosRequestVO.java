package com.guido.scenicai.module.sos.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SosRequestVO {
    private Long id; private String requestNo; private Long userId; private Long cityId; private Long scenicId;
    private String helpType; private String urgency; private String locationText;
    private BigDecimal longitude; private BigDecimal latitude; private String phone;
    private String description; private String status; private LocalDateTime createdAt; private LocalDateTime updatedAt;
}
