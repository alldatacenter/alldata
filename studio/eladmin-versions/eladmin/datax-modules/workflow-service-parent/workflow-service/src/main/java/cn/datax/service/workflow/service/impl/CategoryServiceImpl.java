package cn.datax.service.workflow.service.impl;

import cn.datax.service.workflow.api.entity.CategoryEntity;
import cn.datax.service.workflow.api.dto.CategoryDto;
import cn.datax.service.workflow.service.CategoryService;
import cn.datax.service.workflow.mapstruct.CategoryMapper;
import cn.datax.service.workflow.dao.CategoryDao;
import cn.datax.common.base.BaseServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * <p>
 * 流程分类表 服务实现类
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-10
 */
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class CategoryServiceImpl extends BaseServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {

    @Autowired
    private CategoryDao categoryDao;

    @Autowired
    private CategoryMapper categoryMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CategoryEntity saveCategory(CategoryDto categoryDto) {
        CategoryEntity category = categoryMapper.toEntity(categoryDto);
        categoryDao.insert(category);
        return category;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CategoryEntity updateCategory(CategoryDto categoryDto) {
        CategoryEntity category = categoryMapper.toEntity(categoryDto);
        categoryDao.updateById(category);
        return category;
    }

    @Override
    public CategoryEntity getCategoryById(String id) {
        CategoryEntity categoryEntity = super.getById(id);
        return categoryEntity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCategoryById(String id) {
        categoryDao.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCategoryBatch(List<String> ids) {
        categoryDao.deleteBatchIds(ids);
    }
}
