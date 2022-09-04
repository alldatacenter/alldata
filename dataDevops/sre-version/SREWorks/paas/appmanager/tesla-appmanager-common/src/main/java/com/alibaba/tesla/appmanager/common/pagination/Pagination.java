package com.alibaba.tesla.appmanager.common.pagination;

import com.github.pagehelper.PageInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 公共分页信息类
 *
 * @author qianmo.am@alibaba-inc.com
 */
@Data
@Builder
@AllArgsConstructor
public class Pagination<T> implements Serializable {

    public Pagination() {
        this.items = new ArrayList<>();
    }

    /**
     * 当前页
     */
    private int page;

    /**
     * 每页大小
     */
    private int pageSize;

    /**
     * 总数量
     */
    private long total;

    /**
     * items
     */
    protected List<T> items;

    /**
     * 返回当前对象是否为空
     *
     * @return true or false
     */
    public boolean isEmpty() {
        return items == null || items.size() == 0;
    }

    /**
     * 将 source 根据 converter 转换函数转换到目标对象的 Pagination 对象
     *
     * @param source    来源
     * @param converter 转换函数
     * @return Pagination 对象
     */
    public static <P, V> Pagination<V> transform(Pagination<P> source, Function<P, V> converter) {
        if (source == null) {
            return null;
        }
        return Pagination.<V>builder()
            .items(source.getItems().stream().map(converter).collect(Collectors.toList()))
            .page(source.getPage())
            .pageSize(source.getPageSize())
            .total(source.getTotal())
            .build();
    }

    /**
     * 将 source 对应的 list 通过 converter 转换函数转换为 Pagination 对象
     *
     * @param source    数据来源
     * @param converter 转换函数
     * @return Pagination 对象
     */
    public static <P, V> Pagination<V> valueOf(List<P> source, Function<P, V> converter) {
        if (source == null) {
            return null;
        }
        PageInfo<P> pageInfo = new PageInfo<>(source);
        // TODO: 这里的 total 在传入纯 list 的情况下需要被设置
        return Pagination.<V>builder()
            .items(source.stream().map(converter).collect(Collectors.toList()))
            .page(pageInfo.getPageNum())
            .pageSize(pageInfo.getPageSize())
            .total(pageInfo.getTotal())
            .build();
    }

    public static Integer parseStart(Integer currentPage, Integer pageSize) {
        return (currentPage - 1) * pageSize;
    }

    public static Integer parseEnd(Integer currentPage, Integer pageSize) {
        return parseStart(currentPage - 1, pageSize) + pageSize - 1;
    }
}
