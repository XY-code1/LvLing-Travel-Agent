package com.guido.scenicai.common.result;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {

    private long total;
    private int pageNum;
    private int pageSize;
    private List<T> list;

    public static <T> PageResult<T> of(long total, int pageNum, int pageSize, List<T> list) {
        return new PageResult<>(total, pageNum, pageSize, list);
    }

    public static <T> PageResult<T> empty(int pageNum, int pageSize) {
        return new PageResult<>(0L, pageNum, pageSize, List.of());
    }
}
