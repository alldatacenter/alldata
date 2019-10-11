package com.platform.dto;

import com.platform.model.PmsProductCategory;

import java.util.List;

/**
 * Created by wulinhao on 2019/5/25.
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
