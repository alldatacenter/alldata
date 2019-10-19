package com.platform.mall.dto.admin;

import com.platform.mall.entity.admin.PmsProductCategory;

import java.util.List;

/**
 * Created by wulinhao on 2019/9/25.
 */
public class PmsProductCategoryWithChildrenItem extends PmsProductCategory {
    private List<PmsProductCategory> children;

    public List<PmsProductCategory> getChildren() {
        return children;
    }

    public void setChildren(List<PmsProductCategory> children) {
        this.children = children;
    }
}
