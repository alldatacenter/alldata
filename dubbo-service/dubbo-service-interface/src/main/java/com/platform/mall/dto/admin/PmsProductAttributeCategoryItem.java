package com.platform.mall.dto.admin;

import com.platform.mall.entity.admin.PmsProductAttribute;
import com.platform.mall.entity.admin.PmsProductAttributeCategory;

import java.util.List;

/**
 * 包含有分类下属性的dto
 * Created by wulinhao on 2019/9/24.
 */
public class PmsProductAttributeCategoryItem extends PmsProductAttributeCategory {
    private List<PmsProductAttribute> productAttributeList;

    public List<PmsProductAttribute> getProductAttributeList() {
        return productAttributeList;
    }

    public void setProductAttributeList(List<PmsProductAttribute> productAttributeList) {
        this.productAttributeList = productAttributeList;
    }
}
