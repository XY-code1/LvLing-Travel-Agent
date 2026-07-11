package com.guido.scenicai.common.base;

import lombok.Data;

/**
 * 分页入参基类。
 */
@Data
public class BasePageQuery {

    /** 页码，从 1 开始，默认 1 */
    private int pageNum = 1;

    /** 每页条数，默认 10，最大 100 */
    private int pageSize = 10;

    public int getPageNum() {
        return Math.max(pageNum, 1);
    }

    public int getPageSize() {
        return Math.min(Math.max(pageSize, 1), 100);
    }
}
