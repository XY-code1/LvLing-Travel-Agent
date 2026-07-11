package com.guido.scenicai.module.knowledge.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class KnowledgeDocumentVO {

    private Long id;
    private Long scenicId;
    private String scenicName;
    private String fileName;
    private String fileType;
    private String filePath;
    private Long fileSize;
    private Integer chunkCount;
    private Integer embedStatus;
    private String difyDocumentId;
    private String difyDatasetId;
    private Integer syncStatus;
    private String syncMsg;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
