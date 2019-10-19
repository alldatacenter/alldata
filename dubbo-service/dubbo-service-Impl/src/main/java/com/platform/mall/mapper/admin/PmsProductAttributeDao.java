package com.platform.mall.mapper.admin;

import com.platform.mall.dto.admin.ProductAttrInfo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 自定义商品属性Dao
 * Created by wulinhao on 2019/9/23.
 */
public interface PmsProductAttributeDao {
    List<ProductAttrInfo> getProductAttrInfo(@Param("id") Long productCategoryId);
}
