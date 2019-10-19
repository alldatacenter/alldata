package com.platform.mall.service.admin;

import com.platform.mall.dto.admin.PmsProductCategoryParam;
import com.platform.mall.dto.admin.PmsProductCategoryWithChildrenItem;
import com.platform.mall.entity.admin.PmsProductCategory;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 产品分类Service
 * Created by wulinhao on 2019/9/26.
 */
public interface PmsProductCategoryService {
    @Transactional
    int create(PmsProductCategoryParam pmsProductCategoryParam);

    @Transactional
    int update(Long id, PmsProductCategoryParam pmsProductCategoryParam);

    List<PmsProductCategory> getList(Long parentId, Integer pageSize, Integer pageNum);

    int delete(Long id);

    PmsProductCategory getItem(Long id);

    int updateNavStatus(List<Long> ids, Integer navStatus);

    int updateShowStatus(List<Long> ids, Integer showStatus);

    List<PmsProductCategoryWithChildrenItem> listWithChildren();
}
