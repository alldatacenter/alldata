package com.platform.mall.dto.admin;


import java.io.Serializable;

/**
 * 商品分类对应属性信息
 * Created by wulinhao on 2019/9/23.
 */
public class ProductAttrInfo  implements Serializable {
    private Long attributeId;
    private Long attributeCategoryId;

    public Long getAttributeId() {
        return attributeId;
    }

    public void setAttributeId(Long attributeId) {
        this.attributeId = attributeId;
    }

    public Long getAttributeCategoryId() {
        return attributeCategoryId;
    }

    public void setAttributeCategoryId(Long attributeCategoryId) {
        this.attributeCategoryId = attributeCategoryId;
    }
}
