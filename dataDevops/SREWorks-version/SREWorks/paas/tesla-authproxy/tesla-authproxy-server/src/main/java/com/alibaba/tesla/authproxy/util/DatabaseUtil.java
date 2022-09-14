package com.alibaba.tesla.authproxy.util;

import com.alibaba.tesla.authproxy.lib.exceptions.AuthProxyException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.lang.reflect.Field;

/**
 * 数据库工具类
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public class DatabaseUtil {

    /**
     * 根据 condition 中的 sortBy/orderBy/page/pageSize 树形生成对应的 PageRequest 对象
     *
     * @param condition 查询条件
     * @return
     */
    public static PageRequest getPageRequest(Object condition) {
        String sortBy, orderBy;
        Integer page, pageSize;
        try {
            Field sortByField = condition.getClass().getDeclaredField("sortBy");
            sortByField.setAccessible(true);
            sortBy = (String)sortByField.get(condition);

            Field orderByField = condition.getClass().getDeclaredField("orderBy");
            orderByField.setAccessible(true);
            orderBy = (String) orderByField.get(condition);

            Field pageField = condition.getClass().getDeclaredField("page");
            pageField.setAccessible(true);
            page = (Integer)pageField.get(condition);

            Field pageSizeField = condition.getClass().getDeclaredField("pageSize");
            pageSizeField.setAccessible(true);
            pageSize = (Integer)pageSizeField.get(condition);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new AuthProxyException("Get pageRequest failed: " + ExceptionUtils.getStackTrace(e));
        }

        Sort sort = null;
        if (!StringUtils.isEmpty(sortBy) && !StringUtils.isEmpty(orderBy)) {
            sort = new Sort(new Sort.Order(Sort.Direction.fromString(orderBy), sortBy));
        }
        PageRequest pageable;
        if (sort != null) {
            pageable = new PageRequest(page - 1, pageSize, sort);
        } else {
            pageable = new PageRequest(page - 1, pageSize);
        }
        return pageable;
    }
}
