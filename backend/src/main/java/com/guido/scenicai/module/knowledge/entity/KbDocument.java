package com.guido.scenicai.module.knowledge.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guido.scenicai.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("kb_document")
public class KbDocument extends BaseEntity {

    private Long scenicId;
    private String fileName;
    private String fileType;
    private String filePath;
    private Long fileSize;
    private Integer charCount;
    /** 已生成分块数 */
    private Integer chunkCount;
    /** 0待解析 1解析中 2成功 3失败 */
    private Integer parseStatus;
    /** 0待向量化 1向量化中 2成功 3失败 */
    private Integer embedStatus;
    private String failMsg;
    private Integer status;
}

