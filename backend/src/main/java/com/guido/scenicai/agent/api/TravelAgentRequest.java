package com.guido.scenicai.agent.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class TravelAgentRequest {
    @NotBlank(message = "旅行需求不能为空")
    @Size(max = 1000, message = "旅行需求不能超过1000字")
    private String message;
    private String city;
    private String date;
    private Integer durationDays;
    private BigDecimal budget;
    private String travelers;
    private List<String> preferences;
    private List<String> constraints;
    private String conversationId;
}
