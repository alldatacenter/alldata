package com.alibaba.tesla.appmanager.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * @author qianmo.zm@alibaba-inc.com
 * @date 2020/11/06.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class BaseRequest {
    /**
     * 每页大小
     */
    private Integer pageSize = 20;

    /**
     * 当前页数
     */
    private Integer page = 1;

    /**
     * 需要分页
     */
    private boolean pagination = true;

    /**
     * 是否查询大对象
     */
    private boolean withBlobs = false;
}
