package com.hw.lineage.common.util;

import com.github.pagehelper.PageInfo;
import org.springframework.beans.BeanUtils;

import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @description: PageUtils
 * @author: HamaWhite
 * @version: 1.0.0
 */
public class PageUtils {
    private PageUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static <R, T> PageInfo<R> convertPage(PageInfo<T> inputPage, Function<T, R> mapper) {
        PageInfo<R> resultPage = new PageInfo<>();
        BeanUtils.copyProperties(inputPage, resultPage);
        resultPage.setList(inputPage.getList().stream().map(mapper).collect(Collectors.toList()));
        return resultPage;
    }
}
