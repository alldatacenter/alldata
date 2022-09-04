package com.alibaba.tesla.appmanager.common;

import com.alibaba.tesla.appmanager.common.constants.DefaultConstant;
import com.alibaba.tesla.appmanager.common.util.CommonUtil;
import com.github.pagehelper.PageHelper;
import lombok.AllArgsConstructor;
import lombok.Builder;
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
public class BaseCondition {
    /**
     * 每页大小
     */
    @Builder.Default
    private Integer pageSize = DefaultConstant.DEFAULT_PAGE_SIZE;

    /**
     * 当前页数
     */
    @Builder.Default
    private Integer page = DefaultConstant.DEFAULT_PAGE_NUMBER;


    /**
     * 需要分页
     */
    @Builder.Default
    private boolean pagination = false;

    /**
     * 需要带大字段
     */
    @Builder.Default
    private boolean withBlobs = false;

    public void doPagination() {
        if (pagination) {
            PageHelper.startPage(CommonUtil.getValue(page, DefaultConstant.DEFAULT_PAGE_NUMBER),
                CommonUtil.getValue(pageSize, DefaultConstant.DEFAULT_PAGE_SIZE));
        }
    }
}
