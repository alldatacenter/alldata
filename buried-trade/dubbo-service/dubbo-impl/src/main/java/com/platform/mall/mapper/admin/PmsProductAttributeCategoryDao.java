package com.platform.mall.mapper.admin;

import com.platform.mall.dto.admin.PmsProductAttributeCategoryItem;

import java.util.List;

/**
 * 自定义商品属性分类Dao
 * @author AllDataDC
 */
public interface PmsProductAttributeCategoryDao {
    List<PmsProductAttributeCategoryItem> getListWithAttr();
}
