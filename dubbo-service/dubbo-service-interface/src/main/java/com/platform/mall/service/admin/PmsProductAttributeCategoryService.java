package com.platform.mall.service.admin;

import com.platform.mall.dto.admin.PmsProductAttributeCategoryItem;
import com.platform.mall.entity.admin.PmsProductAttributeCategory;

import java.util.List;

/**
 * 商品属性分类Service
 * Created by wulinhao on 2019/9/26.
 */
public interface PmsProductAttributeCategoryService {
    int create(String name);

    int update(Long id, String name);

    int delete(Long id);

    PmsProductAttributeCategory getItem(Long id);

    List<PmsProductAttributeCategory> getList(Integer pageSize, Integer pageNum);

    List<PmsProductAttributeCategoryItem> getListWithAttr();
}
