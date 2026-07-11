package com.guido.scenicai.module.chat.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SourceVO {

    private String docName;
    private String spotName;
    private String segment;
    private Double score;
}
