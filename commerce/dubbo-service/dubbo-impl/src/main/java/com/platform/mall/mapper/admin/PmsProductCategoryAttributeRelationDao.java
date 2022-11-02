package com.platform.mall.mapper.admin;

import com.platform.mall.entity.admin.PmsProductCategoryAttributeRelation;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 自定义商品分类和属性关系Dao
 * @author AllDataDC
 */
public interface PmsProductCategoryAttributeRelationDao {
    int insertList(@Param("list") List<PmsProductCategoryAttributeRelation> productCategoryAttributeRelationList);
}
