package com.platform.mall.service.admin;

import com.platform.mall.entity.admin.PmsSkuStock;

import java.util.List;

/**
 * sku商品库存管理Service
 * Created by wulinhao on 2019/9/27.
 */
public interface PmsSkuStockService {
    /**
     * 根据产品id和skuCode模糊搜索
     */
    List<PmsSkuStock> getList(Long pid, String keyword);

    /**
     * 批量更新商品库存信息
     */
    int update(Long pid, List<PmsSkuStock> skuStockList);
}
