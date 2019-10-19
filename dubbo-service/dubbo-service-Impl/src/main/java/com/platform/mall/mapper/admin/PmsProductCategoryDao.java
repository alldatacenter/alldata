package com.platform.mall.mapper.admin;

import com.platform.mall.dto.admin.PmsProductCategoryWithChildrenItem;

import java.util.List;

/**
 * 商品分类自定义Dao
 * Created by wulinhao on 2019/9/25.
 */
public interface PmsProductCategoryDao {
    List<PmsProductCategoryWithChildrenItem> listWithChildren();
}
