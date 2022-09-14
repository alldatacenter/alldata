package com.alibaba.sreworks.job.utils;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

public class PageUtil {

    public static Pageable pageable(Integer page, Integer pageSize) {
        if (page == null) {
            page = 1;
        }
        if (pageSize == null) {
            pageSize = 10;
        }
        page = page - 1;
        return PageRequest.of(page, pageSize);
    }

}
