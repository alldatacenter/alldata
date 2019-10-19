package com.platform.mall.service.admin;

import com.platform.mall.dto.admin.PmsProductAttributeParam;
import com.platform.mall.dto.admin.ProductAttrInfo;
import com.platform.mall.entity.admin.PmsProductAttribute;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 商品属性Service
 * Created by wulinhao on 2019/9/26.
 */
public interface PmsProductAttributeService {
    /**
     * 根据分类分页获取商品属性
     *
     * @param cid  分类id
     * @param type 0->属性；2->参数
     * @return
     */
    List<PmsProductAttribute> getList(Long cid, Integer type, Integer pageSize, Integer pageNum);

    /**
     * 添加商品属性
     */
    @Transactional
    int create(PmsProductAttributeParam pmsProductAttributeParam);

    /**
     * 修改商品属性
     */
    int update(Long id, PmsProductAttributeParam productAttributeParam);

    /**
     * 获取单个商品属性信息
     */
    PmsProductAttribute getItem(Long id);

    @Transactional
    int delete(List<Long> ids);

    List<ProductAttrInfo> getProductAttrInfo(Long productCategoryId);
}
