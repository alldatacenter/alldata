package com.platform.manage.service;

import com.platform.manage.dto.PmsProductAttributeCategoryItem;
import com.platform.mbg.model.PmsProductAttributeCategory;

import java.util.List;

/**
 * 商品属性分类Service
 * Created by wulinhao on 2019/4/26.
 */
public interface PmsProductAttributeCategoryService {
    int create(String name);

    int update(Long id, String name);

    int delete(Long id);

    PmsProductAttributeCategory getItem(Long id);

    List<PmsProductAttributeCategory> getList(Integer pageSize, Integer pageNum);

    List<PmsProductAttributeCategoryItem> getListWithAttr();
}
