package cn.datax.service.workflow.service;

import cn.datax.service.workflow.api.entity.CategoryEntity;
import cn.datax.service.workflow.api.dto.CategoryDto;
import cn.datax.common.base.BaseService;

import java.util.List;

/**
 * <p>
 * 流程分类表 服务类
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-10
 */
public interface CategoryService extends BaseService<CategoryEntity> {

    CategoryEntity saveCategory(CategoryDto category);

    CategoryEntity updateCategory(CategoryDto category);

    CategoryEntity getCategoryById(String id);

    void deleteCategoryById(String id);

    void deleteCategoryBatch(List<String> ids);
}
