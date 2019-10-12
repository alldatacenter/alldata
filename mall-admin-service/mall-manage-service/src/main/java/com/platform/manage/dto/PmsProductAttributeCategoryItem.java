package com.platform.manage.dto;

import com.platform.mbg.model.PmsProductAttribute;
import com.platform.mbg.model.PmsProductAttributeCategory;

import java.util.List;

/**
 * 包含有分类下属性的dto
 * Created by wulinhao on 2019/5/24.
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
